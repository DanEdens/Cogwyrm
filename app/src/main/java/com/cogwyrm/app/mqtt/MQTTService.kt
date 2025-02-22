package com.cogwyrm.app.mqtt

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cogwyrm.app.MainActivity
import com.cogwyrm.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MQTTService(private val context: Context) {
    companion object {
        private const val TAG = "MQTTService"
        private const val NOTIFICATION_CHANNEL_ID = "mqtt_service"
        private const val NOTIFICATION_ID = 1
        private const val MAX_MESSAGE_HISTORY = 100
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mqttClient: MqttAsyncClient? = null
    private val messageHistory = mutableListOf<MQTTMessage>()
    private val activeSubscriptions = ConcurrentHashMap<String, MQTTSubscription>()
    private var notificationManager: NotificationManager? = null
    private var serviceNotification: NotificationCompat.Builder? = null

    data class MQTTMessage(
        val topic: String,
        val payload: String,
        val qos: Int,
        val retained: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class MQTTSubscription(
        val topic: String,
        val qos: Int,
        val callback: (String, String) -> Unit,
        val refCount: AtomicInteger = AtomicInteger(1)
    )

    init {
        setupNotificationChannel()
    }

    private fun setupNotificationChannel() {
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "MQTT Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "MQTT Service notifications"
            setShowBadge(false)
        }

        notificationManager?.createNotificationChannel(channel)

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        serviceNotification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("MQTT Service")
            .setContentText("Service is running")
            .setSmallIcon(R.drawable.ic_mqtt_notification)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
    }

    private fun updateNotification(status: String) {
        serviceNotification?.setContentText(status)
        notificationManager?.notify(NOTIFICATION_ID, serviceNotification?.build())
    }

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
                            updateNotification("Connection lost")
                        }

                        override fun messageArrived(topic: String, message: MqttMessage) {
                            val payload = String(message.payload)
                            Log.d(TAG, "Message received on topic $topic: $payload")

                            synchronized(messageHistory) {
                                messageHistory.add(
                                    MQTTMessage(
                                        topic = topic,
                                        payload = payload,
                                        qos = message.qos,
                                        retained = message.isRetained
                                    )
                                )
                                if (messageHistory.size > MAX_MESSAGE_HISTORY) {
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
                        updateNotification("Connected to $brokerUrl")
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
                        mqttClient = null
                        activeSubscriptions.clear()
                        updateNotification("Disconnected")
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
                            Log.d(TAG, "Published message to topic $topic: $message")
                            continuation.resume(Unit)
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            val error = exception ?: Exception("Failed to publish message")
                            Log.e(TAG, "Failed to publish message", error)
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
        callback: (String, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                activeSubscriptions.compute(topic) { _, existing ->
                    existing?.apply {
                        refCount.incrementAndGet()
                    } ?: MQTTSubscription(topic, qos, callback)
                }

                mqttClient?.subscribe(topic, qos, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d(TAG, "Subscribed to topic: $topic")
                        continuation.resume(Unit)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val error = exception ?: Exception("Failed to subscribe")
                        Log.e(TAG, "Failed to subscribe to topic", error)
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
                activeSubscriptions[topic]?.let { subscription ->
                    if (subscription.refCount.decrementAndGet() <= 0) {
                        activeSubscriptions.remove(topic)
                        mqttClient?.unsubscribe(topic, null, object : IMqttActionListener {
                            override fun onSuccess(asyncActionToken: IMqttToken?) {
                                Log.d(TAG, "Unsubscribed from topic: $topic")
                                continuation.resume(Unit)
                            }

                            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                                val error = exception ?: Exception("Failed to unsubscribe")
                                Log.e(TAG, "Failed to unsubscribe from topic", error)
                                continuation.resumeWithException(error)
                            }
                        })
                    } else {
                        continuation.resume(Unit)
                    }
                } ?: continuation.resume(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error during unsubscribe", e)
                continuation.resumeWithException(e)
            }
        }
    }

    fun getMessageHistory(): List<MQTTMessage> {
        return synchronized(messageHistory) {
            messageHistory.toList()
        }
    }

    fun isConnected(): Boolean {
        return mqttClient?.isConnected ?: false
    }
}
