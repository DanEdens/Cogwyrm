package com.cogwyrm.app.mqtt

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MQTTClient(
    private val brokerUrl: String,
    private val port: String,
    private val clientId: String,
    private val useSsl: Boolean
) {
    private val client: MqttClient by lazy {
        val serverUri = if (useSsl) {
            "ssl://$brokerUrl:$port"
        } else {
            "tcp://$brokerUrl:$port"
        }
        MqttClient(serverUri, clientId, MemoryPersistence())
    }

    fun connect() {
        val options = MqttConnectOptions().apply {
            isCleanSession = true
        }
        client.connect(options)
    }

    fun publish(topic: String, message: String) {
        val mqttMessage = MqttMessage(message.toByteArray())
        client.publish(topic, mqttMessage)
    }

    fun disconnect() {
        if (client.isConnected) {
            client.disconnect()
        }
    }
}
