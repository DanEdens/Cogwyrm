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
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

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
    ) {
        scope.launch {
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

                mqttClient?.connect(options)?.waitForCompletion()
                updateNotification("Connected to $brokerUrl")
                Log.d(TAG, "Connected to MQTT broker: $serverUri")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to MQTT broker", e)
                throw e
            }
        }
    }

    suspend fun disconnect() {
        scope.launch {
            try {
                mqttClient?.disconnect()?.waitForCompletion()
                mqttClient = null
                activeSubscriptions.clear()
                updateNotification("Disconnected")
                Log.d(TAG, "Disconnected from MQTT broker")
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting from MQTT broker", e)
                throw e
            }
        }
    }

    suspend fun publish(
        topic: String,
        message: String,
        qos: Int = 1,
        retained: Boolean = false
    ) {
        scope.launch {
            try {
                mqttClient?.publish(
                    topic,
                    MqttMessage(message.toByteArray()).apply {
                        this.qos = qos
                        isRetained = retained
                    }
                )?.waitForCompletion()

                Log.d(TAG, "Published message to topic $topic: $message")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to publish message", e)
                throw e
            }
        }
    }

    suspend fun subscribe(
        topic: String,
        qos: Int = 1,
        callback: (String, String) -> Unit
    ) {
        scope.launch {
            try {
                activeSubscriptions.compute(topic) { _, existing ->
                    existing?.apply {
                        refCount.incrementAndGet()
                    } ?: MQTTSubscription(topic, qos, callback)
                }

                mqttClient?.subscribe(topic, qos)?.waitForCompletion()
                Log.d(TAG, "Subscribed to topic: $topic")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to subscribe to topic", e)
                throw e
            }
        }
    }

    suspend fun unsubscribe(topic: String) {
        scope.launch {
            try {
                activeSubscriptions[topic]?.let { subscription ->
                    if (subscription.refCount.decrementAndGet() <= 0) {
                        activeSubscriptions.remove(topic)
                        mqttClient?.unsubscribe(topic)?.waitForCompletion()
                        Log.d(TAG, "Unsubscribed from topic: $topic")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unsubscribe from topic", e)
                throw e
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
