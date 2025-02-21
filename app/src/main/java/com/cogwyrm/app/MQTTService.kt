package com.cogwyrm.app

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.android.service.MqttAndroidClient
import java.util.*

class MQTTService : Service() {
    private val binder = LocalBinder()
    private var mqttClient: MqttAndroidClient? = null
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

    fun connect(serverUri: String, port: Int, username: String? = null, password: String? = null) {
        val generatedClientId = "CogwyrmMQTT-" + UUID.randomUUID().toString()
        val fullServerUri = "tcp://$serverUri:$port"

        mqttClient = MqttAndroidClient(applicationContext, fullServerUri, generatedClientId).apply {
            setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.e(TAG, "Connection lost: ${cause?.message}")
                    _isConnected = false
                    showNotification("Connection Lost", "MQTT connection was lost")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    message?.let {
                        val messageStr = String(it.payload)
                        Log.d(TAG, "Message received: $messageStr on topic: $topic")
                        messageHistory.add(MessageRecord(topic ?: "", messageStr))
                        showNotification("Message Received", "New message on topic: $topic")
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d(TAG, "Message delivered")
                }
            })
        }

        val options = MqttConnectOptions().apply {
            isCleanSession = true
            if (username != null && password != null) {
                this.userName = username
                this.password = password.toCharArray()
            }
        }

        try {
            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connection success")
                    _isConnected = true
                    showNotification("Connected", "Successfully connected to MQTT broker")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Connection failure: ${exception?.message}")
                    _isConnected = false
                    showNotification("Connection Failed", "Failed to connect to MQTT broker")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
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
            mqttClient?.publish(topic, MqttMessage(message.toByteArray()))
            messageHistory.add(MessageRecord(topic, message, isIncoming = false))
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun subscribe(topic: String) {
        try {
            mqttClient?.subscribe(topic, 1)
        } catch (e: MqttException) {
            e.printStackTrace()
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
