package com.cogwyrm.app

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
import com.cogwyrm.app.mqtt.MQTTClient
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class MQTTService : Service() {
    private val binder = LocalBinder()
    private var mqttClient: MQTTClient? = null
    private var _isConnected = false
    private val messageHistory = Collections.synchronizedList(mutableListOf<MessageRecord>())
    private val activeSubscriptions = ConcurrentHashMap<String, SubscriptionInfo>()
    private lateinit var prefs: SharedPreferences
    private lateinit var notificationManager: NotificationManager

    private val NOTIFICATION_CHANNEL_ID = "com.cogwyrm.app.mqtt"
    private val FOREGROUND_NOTIFICATION_ID = 1
    private val MESSAGE_NOTIFICATION_ID = 2

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
            // Service channel
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "MQTT Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification for MQTT service status"
            }

            // Message channel
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
            // Add MainActivity to the stack
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

    fun connect(serverUri: String, port: Int, clientId: String? = null, useSsl: Boolean = false,
                username: String? = null, password: String? = null) {
        val generatedClientId = clientId ?: "cogwyrm_${UUID.randomUUID()}"

        disconnect() // Clean up any existing connection

        mqttClient = MQTTClient(
            context = applicationContext,
            brokerUrl = serverUri,
            port = port.toString(),
            clientId = generatedClientId,
            useSsl = useSsl,
            onConnectionLost = { cause ->
                _isConnected = false
                updateForegroundNotification()
                showMessageNotification("Connection Lost", "MQTT connection was lost: ${cause?.message}")
                Log.e(TAG, "Connection lost", cause)
            },
            onMessageArrived = { topic, message ->
                handleIncomingMessage(topic, message)
            }
        ).apply {
            connect(object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connection success")
                    _isConnected = true
                    updateForegroundNotification()
                    saveConnectionState(serverUri, port, generatedClientId, useSsl, username, password)
                    restoreSubscriptions()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Connection failure", exception)
                    _isConnected = false
                    updateForegroundNotification()
                    showMessageNotification("Connection Failed",
                        "Failed to connect to MQTT broker: ${exception?.message}")
                }
            })
        }
    }

    private fun handleIncomingMessage(topic: String, message: String) {
        val record = MessageRecord(topic, message)
        messageHistory.add(record)

        // Trim history if it gets too large
        while (messageHistory.size > MAX_HISTORY_SIZE) {
            messageHistory.removeAt(0)
        }

        showMessageNotification("Message Received", "New message on topic: $topic")
        Log.d(TAG, "Message received on topic: $topic")
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
            _isConnected = false
            updateForegroundNotification()
            clearConnectionState()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
        }
    }

    fun publish(topic: String, message: String, qos: Int = 1, retained: Boolean = false) {
        try {
            mqttClient?.publish(topic, message, qos, retained)
            messageHistory.add(MessageRecord(topic, message, qos, retained, isIncoming = false))
        } catch (e: Exception) {
            Log.e(TAG, "Error publishing message", e)
            showMessageNotification("Publish Failed", "Failed to publish message: ${e.message}")
        }
    }

    fun subscribe(topic: String, qos: Int = 1) {
        try {
            mqttClient?.subscribe(topic, qos, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Successfully subscribed to: $topic")
                    activeSubscriptions[topic] = SubscriptionInfo(topic, qos)
                    saveSubscriptions()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Failed to subscribe to: $topic", exception)
                    showMessageNotification("Subscribe Failed",
                        "Failed to subscribe to topic: ${exception?.message}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing", e)
            showMessageNotification("Subscribe Failed", "Failed to subscribe to topic: ${e.message}")
        }
    }

    fun unsubscribe(topic: String) {
        try {
            mqttClient?.unsubscribe(topic)
            activeSubscriptions.remove(topic)
            saveSubscriptions()
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing", e)
        }
    }

    fun getMessageHistory(): List<MessageRecord> = messageHistory.toList()

    fun getActiveSubscriptions(): Map<String, SubscriptionInfo> = activeSubscriptions.toMap()

    private fun showMessageNotification(title: String, content: String) {
        val notification = NotificationCompat.Builder(this, "${NOTIFICATION_CHANNEL_ID}_messages")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_mqtt_notification)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(MESSAGE_NOTIFICATION_ID, notification)
    }

    private fun saveConnectionState(serverUri: String, port: Int, clientId: String,
                                  useSsl: Boolean, username: String?, password: String?) {
        val state = JSONObject().apply {
            put("serverUri", serverUri)
            put("port", port)
            put("clientId", clientId)
            put("useSsl", useSsl)
            username?.let { put("username", it) }
            password?.let { put("password", it) }
        }
        prefs.edit().putString("connection_state", state.toString()).apply()
    }

    private fun clearConnectionState() {
        prefs.edit().remove("connection_state").apply()
    }

    private fun saveSubscriptions() {
        val subscriptions = JSONObject().apply {
            activeSubscriptions.forEach { (topic, info) ->
                put(topic, JSONObject().apply {
                    put("qos", info.qos)
                    put("timestamp", info.timestamp)
                })
            }
        }
        prefs.edit().putString("subscriptions", subscriptions.toString()).apply()
    }

    private fun restoreState() {
        val connectionState = prefs.getString("connection_state", null)?.let {
            try {
                JSONObject(it)
            } catch (e: Exception) {
                null
            }
        }

        connectionState?.let {
            connect(
                serverUri = it.getString("serverUri"),
                port = it.getInt("port"),
                clientId = it.getString("clientId"),
                useSsl = it.getBoolean("useSsl"),
                username = it.optString("username", null),
                password = it.optString("password", null)
            )
        }
    }

    private fun restoreSubscriptions() {
        if (!_isConnected) return

        val subscriptions = prefs.getString("subscriptions", null)?.let {
            try {
                JSONObject(it)
            } catch (e: Exception) {
                null
            }
        }

        subscriptions?.let {
            it.keys().forEach { topic ->
                val info = it.getJSONObject(topic)
                subscribe(topic, info.getInt("qos"))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }

    companion object {
        private const val TAG = "MQTTService"
        private const val MAX_HISTORY_SIZE = 100
    }
}
