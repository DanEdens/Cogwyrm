package com.cogwyrm.app

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cogwyrm.app.mqtt.MQTTClient
import org.eclipse.paho.client.mqttv3.*
import java.util.*

class MQTTService : Service() {
    private val binder = LocalBinder()
    private var mqttClient: MQTTClient? = null
    private var _isConnected = false
    private val messageHistory = mutableListOf<MessageRecord>()
    private var notificationManager: NotificationManager? = null
    private val NOTIFICATION_CHANNEL_ID = "com.cogwyrm.app.mqtt"
    private val NOTIFICATION_ID = 1

    data class MessageRecord(
        val topic: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
        val isIncoming: Boolean = true
    )

    inner class LocalBinder : Binder() {
        fun getService(): MQTTService = this@MQTTService
    }

    override fun onCreate() {
        super.onCreate()
        setupNotificationChannel()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "MQTT Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for MQTT messages"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun isConnected(): Boolean = _isConnected

    fun connect(serverUri: String, port: Int, clientId: String?, password: String? = null) {
        val generatedClientId = clientId ?: "CogwyrmMQTT-${UUID.randomUUID()}"

        mqttClient = MQTTClient(
            context = applicationContext,
            brokerUrl = serverUri,
            port = port.toString(),
            clientId = generatedClientId,
            useSsl = false,
            onConnectionLost = { cause ->
                _isConnected = false
                showNotification("Connection Lost", "MQTT connection was lost: ${cause?.message}")
            },
            onMessageArrived = { topic, message ->
                messageHistory.add(MessageRecord(topic, message))
                showNotification("Message Received", "New message on topic: $topic")
            }
        ).apply {
            connect(object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connection success")
                    _isConnected = true
                    showNotification("Connected", "Successfully connected to MQTT broker")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Connection failure: ${exception?.message}")
                    _isConnected = false
                    showNotification("Connection Failed", "Failed to connect to MQTT broker: ${exception?.message}")
                }
            })
        }
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
            _isConnected = false
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun publish(topic: String, message: String) {
        try {
            mqttClient?.publish(topic, message)
            messageHistory.add(MessageRecord(topic, message, isIncoming = false))
        } catch (e: MqttException) {
            e.printStackTrace()
            showNotification("Publish Failed", "Failed to publish message: ${e.message}")
        }
    }

    fun subscribe(topic: String) {
        try {
            mqttClient?.subscribe(topic)
            showNotification("Subscribed", "Successfully subscribed to topic: $topic")
        } catch (e: MqttException) {
            e.printStackTrace()
            showNotification("Subscribe Failed", "Failed to subscribe to topic: ${e.message}")
        }
    }

    fun getMessageHistory(): List<MessageRecord> = messageHistory.toList()

    private fun showNotification(title: String, content: String) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "MQTTService"
    }
}
