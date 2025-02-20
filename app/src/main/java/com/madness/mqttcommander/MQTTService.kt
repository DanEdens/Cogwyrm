package com.madness.mqttcommander.mqtt

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.android.service.MqttAndroidClient

class MQTTService : Service() {
    private lateinit var mqttClient: MqttAndroidClient
    private var isConnected = false

    companion object {
        private var instance: MQTTService? = null

        fun getInstance(context: android.content.Context): MQTTService {
            return instance ?: synchronized(this) {
                instance ?: MQTTService().also { instance = it }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initializeMQTT()
    }

    private fun initializeMQTT() {
        mqttClient = MqttAndroidClient(
            applicationContext,
            "tcp://localhost:1883", // For local testing
            "MadnessMQTTCommander_${System.currentTimeMillis()}"
        )

        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                isConnected = true
            }

            override fun connectionLost(cause: Throwable?) {
                isConnected = false
                // ATTEMPT TO RESURRECT THE CONNECTION
                reconnect()
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                // PROCESS THE INCOMING WISDOM
                message?.let {
                    // Notify any listeners (We'll implement this later)
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                // Message successfully delivered to the VOID
            }
        })
    }

    fun publish(topic: String, message: String, qos: Int = 1, retain: Boolean = false) {
        if (!isConnected) {
            connect()
        }

        try {
            val mqttMessage = MqttMessage(message.toByteArray())
            mqttMessage.qos = qos
            mqttMessage.isRetained = retain
            mqttClient.publish(topic, mqttMessage)
        } catch (e: Exception) {
            // LOG THE FAILURE TO THE VOID
        }
    }

    private fun connect() {
        try {
            mqttClient.connect(MqttConnectOptions().apply {
                isAutomaticReconnect = true
                isCleanSession = true
            })
        } catch (e: Exception) {
            // HANDLE THE COSMIC FAILURE
        }
    }

    private fun reconnect() {
        if (!mqttClient.isConnected) {
            connect()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
