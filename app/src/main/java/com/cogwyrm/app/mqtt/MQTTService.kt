package com.cogwyrm.app.mqtt

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.cogwyrm.app.MainActivity
import com.cogwyrm.app.R
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MQTTService(private val context: Context? = null) : Service() {
    private val binder = LocalBinder()
    private var mqttClient: MqttAsyncClient? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val messageHistory = Collections.synchronizedList(mutableListOf<MessageRecord>())
    private val activeSubscriptions = ConcurrentHashMap<String, SubscriptionInfo>()
    private lateinit var prefs: SharedPreferences
    private lateinit var notificationManager: NotificationManager
    private var _isConnected = false

    companion object {
        private const val TAG = "MQTTService"
        private const val NOTIFICATION_CHANNEL_ID = "mqtt_service"
        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val MESSAGE_NOTIFICATION_ID = 2
        private const val MAX_HISTORY_SIZE = 100
    }

    data class MessageRecord(
        val topic: String,
        val message: String,
        val qos: Int = 1,
        val retained: Boolean = false,
        val timestamp: Long = System.currentTimeMillis(),
        val isIncoming: Boolean = true
    )

    data class SubscriptionInfo(
        val topic: String,
        val qos: Int,
        val callback: (String, String) -> Unit,
        val timestamp: Long = System.currentTimeMillis()
    )

    inner class LocalBinder : Binder() {
        fun getService(): MQTTService = this@MQTTService
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("mqtt_service", Context.MODE_PRIVATE)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        setupNotificationChannel()
        restoreState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "MQTT Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "MQTT Service notifications"
                setShowBadge(false)
            }

            val messageChannel = NotificationChannel(
                "${NOTIFICATION_CHANNEL_ID}_messages",
                "MQTT Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for MQTT messages"
            }

            notificationManager.createNotificationChannels(listOf(serviceChannel, messageChannel))
        }
    }

    private fun createForegroundNotification(): Notification {
        val pendingIntent = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(Intent(this@MQTTService, MainActivity::class.java))
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("MQTT Service")
            .setContentText(if (_isConnected) "Connected" else "Disconnected")
            .setSmallIcon(R.drawable.ic_mqtt_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateForegroundNotification() {
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, createForegroundNotification())
    }

    fun isConnected(): Boolean = _isConnected

    suspend fun connect(
        brokerUrl: String,
        port: Int,
        clientId: String? = null,
        useSsl: Boolean = false,
        username: String? = null,
        password: String? = null
    ) = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val serverUri = if (useSsl) {
                    "ssl://$brokerUrl:$port"
                } else {
                    "tcp://$brokerUrl:$port"
                }

                val finalClientId = clientId ?: "cogwyrm_${System.currentTimeMillis()}"

                mqttClient = MqttAsyncClient(
                    serverUri,
                    finalClientId,
                    MemoryPersistence()
                ).apply {
                    setCallback(object : MqttCallback {
                        override fun connectionLost(cause: Throwable?) {
                            Log.e(TAG, "Connection lost", cause)
                            _isConnected = false
                            updateForegroundNotification()
                            showMessageNotification(
                                "Connection Lost",
                                "MQTT connection was lost: ${cause?.message}"
                            )
                            scope.launch {
                                // Attempt to reconnect
                                try {
                                    connect(brokerUrl, port, finalClientId, useSsl, username, password)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Reconnection failed", e)
                                }
                            }
                        }

                        override fun messageArrived(topic: String, message: MqttMessage) {
                            val payload = String(message.payload)
                            Log.d(TAG, "Message received on topic $topic: $payload")

                            synchronized(messageHistory) {
                                messageHistory.add(
                                    MessageRecord(
                                        topic = topic,
                                        message = payload,
                                        qos = message.qos,
                                        retained = message.isRetained
                                    )
                                )
                                if (messageHistory.size > MAX_HISTORY_SIZE) {
                                    messageHistory.removeAt(0)
                                }
                            }

                            activeSubscriptions[topic]?.callback?.invoke(topic, payload)
                        }

                        override fun deliveryComplete(token: IMqttDeliveryToken?) {
                            Log.d(TAG, "Message delivered")
                        }
                    })
                }

                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 30
                    keepAliveInterval = 60
                    if (username != null && password != null) {
                        this.userName = username
                        this.password = password.toCharArray()
                    }
                }

                mqttClient?.connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        _isConnected = true
                        updateForegroundNotification()
                        Log.d(TAG, "Connected to MQTT broker: $serverUri")
                        continuation.resume(Unit)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val error = exception ?: Exception("Failed to connect")
                        Log.e(TAG, "Failed to connect to MQTT broker", error)
                        continuation.resumeWithException(error)
                    }
                })

                continuation.invokeOnCancellation {
                    try {
                        mqttClient?.disconnect()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error disconnecting during cancellation", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up MQTT client", e)
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                mqttClient?.disconnect(null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        _isConnected = false
                        mqttClient = null
                        activeSubscriptions.clear()
                        updateForegroundNotification()
                        Log.d(TAG, "Disconnected from MQTT broker")
                        continuation.resume(Unit)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val error = exception ?: Exception("Failed to disconnect")
                        Log.e(TAG, "Error disconnecting from MQTT broker", error)
                        continuation.resumeWithException(error)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error during disconnect", e)
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun publish(
        topic: String,
        message: String,
        qos: Int = 1,
        retained: Boolean = false
    ) = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                mqttClient?.publish(
                    topic,
                    MqttMessage(message.toByteArray()).apply {
                        this.qos = qos
                        isRetained = retained
                    },
                    null,
                    object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            messageHistory.add(
                                MessageRecord(
                                    topic = topic,
                                    message = message,
                                    qos = qos,
                                    retained = retained,
                                    isIncoming = false
                                )
                            )
                            Log.d(TAG, "Published message to topic $topic: $message")
                            continuation.resume(Unit)
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            val error = exception ?: Exception("Failed to publish message")
                            Log.e(TAG, "Failed to publish message", error)
                            showMessageNotification(
                                "Publish Failed",
                                "Failed to publish message: ${error.message}"
                            )
                            continuation.resumeWithException(error)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error during publish", e)
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun subscribe(
        topic: String,
        qos: Int = 1,
        callback: (String, String) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                activeSubscriptions.compute(topic) { _, existing ->
                    existing?.apply {
                        // Update callback if different
                        if (this.callback != callback) {
                            SubscriptionInfo(topic, qos, callback)
                        } else {
                            this
                        }
                    } ?: SubscriptionInfo(topic, qos, callback)
                }

                mqttClient?.subscribe(topic, qos, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d(TAG, "Subscribed to topic: $topic")
                        saveSubscriptions()
                        continuation.resume(Unit)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val error = exception ?: Exception("Failed to subscribe")
                        Log.e(TAG, "Failed to subscribe to topic", error)
                        showMessageNotification(
                            "Subscribe Failed",
                            "Failed to subscribe to topic: ${error.message}"
                        )
                        continuation.resumeWithException(error)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error during subscribe", e)
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun unsubscribe(topic: String) = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                activeSubscriptions[topic]?.let {
                    activeSubscriptions.remove(topic)
                    mqttClient?.unsubscribe(topic, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d(TAG, "Unsubscribed from topic: $topic")
                            saveSubscriptions()
                            continuation.resume(Unit)
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            val error = exception ?: Exception("Failed to unsubscribe")
                            Log.e(TAG, "Failed to unsubscribe from topic", error)
                            continuation.resumeWithException(error)
                        }
                    })
                } ?: continuation.resume(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error during unsubscribe", e)
                continuation.resumeWithException(e)
            }
        }
    }

    private fun showMessageNotification(title: String, content: String) {
        val notification = NotificationCompat.Builder(this, "${NOTIFICATION_CHANNEL_ID}_messages")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_mqtt_notification)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(MESSAGE_NOTIFICATION_ID, notification)
    }

    fun getMessageHistory(): List<MessageRecord> = messageHistory.toList()

    fun getActiveSubscriptions(): Map<String, SubscriptionInfo> = activeSubscriptions.toMap()

    private fun saveSubscriptions() {
        val subscriptionsJson = JSONObject().apply {
            activeSubscriptions.forEach { (topic, info) ->
                put(topic, JSONObject().apply {
                    put("qos", info.qos)
                    put("timestamp", info.timestamp)
                })
            }
        }
        prefs.edit().putString("subscriptions", subscriptionsJson.toString()).apply()
    }

    private fun restoreState() {
        val subscriptions = prefs.getString("subscriptions", null)?.let {
            try {
                JSONObject(it)
            } catch (e: Exception) {
                null
            }
        }

        subscriptions?.keys()?.forEach { topic ->
            val info = subscriptions.getJSONObject(topic.toString())
            scope.launch {
                try {
                    subscribe(
                        topic = topic.toString(),
                        qos = info.getInt("qos")
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore subscription for topic: $topic", e)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.launch {
            try {
                disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error during service destruction", e)
            }
        }
        scope.cancel()
    }
}
