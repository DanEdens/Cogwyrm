package com.madness.mqttcommander

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.android.service.MqttAndroidClient
import java.util.*

class MQTTService : Service() {
    private val binder = LocalBinder()
    private var mqttClient: MqttAndroidClient? = null
    private var isConnected = false

    inner class LocalBinder : Binder() {
        fun getService(): MQTTService = this@MQTTService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun connect(brokerUrl: String, port: Int, clientId: String, useSSL: Boolean) {
        val serverUri = if (useSSL) "ssl://$brokerUrl:$port" else "tcp://$brokerUrl:$port"
        val generatedClientId = if (clientId.isEmpty()) "MQTTCommander_${UUID.randomUUID()}" else clientId

        mqttClient = MqttAndroidClient(applicationContext, serverUri, generatedClientId)

        try {
            val token = mqttClient?.connect()
            token?.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connection success")
                    isConnected = true
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Connection failure: ${exception?.message}")
                    isConnected = false
                }
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Error connecting to broker: ${e.message}")
            isConnected = false
        }
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
            isConnected = false
        } catch (e: MqttException) {
            Log.e(TAG, "Error disconnecting: ${e.message}")
        }
    }

    fun publish(topic: String, message: String, qos: Int = 1) {
        if (!isConnected) {
            Log.e(TAG, "Cannot publish - not connected to broker")
            return
        }

        try {
            val msg = MqttMessage(message.toByteArray())
            msg.qos = qos
            mqttClient?.publish(topic, msg)
        } catch (e: MqttException) {
            Log.e(TAG, "Error publishing message: ${e.message}")
        }
    }

    fun subscribe(topic: String, qos: Int = 1) {
        if (!isConnected) {
            Log.e(TAG, "Cannot subscribe - not connected to broker")
            return
        }

        try {
            mqttClient?.subscribe(topic, qos)
        } catch (e: MqttException) {
            Log.e(TAG, "Error subscribing to topic: ${e.message}")
        }
    }

    fun isConnected(): Boolean = isConnected

    companion object {
        private const val TAG = "MQTTService"
    }
}
