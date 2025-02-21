package com.cogwyrm.app.mqtt

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class MQTTClient(
    context: Context,
    private val brokerUrl: String,
    private val port: String,
    private val clientId: String,
    private val useSsl: Boolean,
    private val onConnectionLost: ((Throwable?) -> Unit)? = null,
    private val onMessageArrived: ((String, String) -> Unit)? = null,
    private val onDeliveryComplete: ((IMqttDeliveryToken?) -> Unit)? = null
) {
    private val client: MqttAndroidClient by lazy {
        val serverUri = if (useSsl) {
            "ssl://$brokerUrl:$port"
        } else {
            "tcp://$brokerUrl:$port"
        }
        MqttAndroidClient(context, serverUri, clientId).apply {
            setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.e(TAG, "Connection lost: ${cause?.message}")
                    onConnectionLost?.invoke(cause)
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    if (topic != null && message != null) {
                        val messageStr = String(message.payload)
                        Log.d(TAG, "Message received: $messageStr on topic: $topic")
                        onMessageArrived?.invoke(topic, messageStr)
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d(TAG, "Message delivered")
                    onDeliveryComplete?.invoke(token)
                }
            })
        }
    }

    fun connect(callback: IMqttActionListener? = null) {
        val options = MqttConnectOptions().apply {
            isCleanSession = true
            keepAliveInterval = 60
            connectionTimeout = 30
        }
        client.connect(options, null, callback)
    }

    fun publish(topic: String, message: String) {
        val mqttMessage = MqttMessage(message.toByteArray())
        client.publish(topic, mqttMessage)
    }

    fun subscribe(topic: String, qos: Int = 1) {
        client.subscribe(topic, qos)
    }

    fun disconnect() {
        if (client.isConnected) {
            client.disconnect()
        }
    }

    fun isConnected(): Boolean = client.isConnected

    companion object {
        private const val TAG = "MQTTClient"
    }
}
