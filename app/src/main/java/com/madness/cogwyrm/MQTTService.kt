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
    private var isConnected = false
    private val messageHistory = mutableListOf<MessageRecord>()
    private var notificationManager: NotificationManager? = null
    private val NOTIFICATION_CHANNEL_ID = "com.madness.cogwyrm.mqtt"
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
                enableLights(true)
                enableVibration(true)
            }

            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .build()

        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    fun connect(brokerUrl: String, port: Int, clientId: String, useSSL: Boolean) {
        val serverUri = if (useSSL) "ssl://$brokerUrl:$port" else "tcp://$brokerUrl:$port"
        val generatedClientId = if (clientId.isEmpty()) "Cogwyrm_${UUID.randomUUID()}" else clientId

        mqttClient = MqttAndroidClient(applicationContext, serverUri, generatedClientId).apply {
            setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.e(TAG, "Connection lost: ${cause?.message}")
                    isConnected = false
                    showNotification("Connection Lost", "MQTT connection was lost")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    message?.let {
                        val messageText = String(it.payload)
                        Log.d(TAG, "Message received on $topic: $messageText")
                        messageHistory.add(MessageRecord(topic ?: "", messageText))
                        showNotification("New Message", "Received on topic: $topic")
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d(TAG, "Message delivered")
                }
            })
        }

        try {
            val token = mqttClient?.connect()
            token?.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connection success")
                    isConnected = true
                    showNotification("Connected", "Successfully connected to MQTT broker")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Connection failure: ${exception?.message}")
                    isConnected = false
                    showNotification("Connection Failed", "Could not connect to MQTT broker")
                }
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Error connecting to broker: ${e.message}")
            isConnected = false
            showNotification("Connection Error", "Failed to connect: ${e.message}")
        }
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
            isConnected = false
            showNotification("Disconnected", "Disconnected from MQTT broker")
        } catch (e: MqttException) {
            Log.e(TAG, "Error disconnecting: ${e.message}")
        }
    }

    fun publish(topic: String, message: String, qos: Int = 1) {
        if (!isConnected) {
            Log.e(TAG, "Cannot publish - not connected to broker")
            showNotification("Publish Failed", "Not connected to broker")
            return
        }

        try {
            val msg = MqttMessage(message.toByteArray())
            msg.qos = qos
            mqttClient?.publish(topic, msg)
            messageHistory.add(MessageRecord(topic, message, isIncoming = false))
            showNotification("Message Sent", "Published to topic: $topic")
        } catch (e: MqttException) {
            Log.e(TAG, "Error publishing message: ${e.message}")
            showNotification("Publish Failed", "Error: ${e.message}")
        }
    }

    fun subscribe(topic: String, qos: Int = 1) {
        if (!isConnected) {
            Log.e(TAG, "Cannot subscribe - not connected to broker")
            showNotification("Subscribe Failed", "Not connected to broker")
            return
        }

        try {
            mqttClient?.subscribe(topic, qos)
            showNotification("Subscribed", "Subscribed to topic: $topic")
        } catch (e: MqttException) {
            Log.e(TAG, "Error subscribing to topic: ${e.message}")
            showNotification("Subscribe Failed", "Error: ${e.message}")
        }
    }

    fun isConnected(): Boolean = isConnected

    fun getMessageHistory(): List<MessageRecord> = messageHistory.toList()

    companion object {
        private const val TAG = "MQTTService"
    }
}
