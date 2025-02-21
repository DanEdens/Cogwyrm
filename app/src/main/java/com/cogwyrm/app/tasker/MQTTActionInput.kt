package com.cogwyrm.app.tasker

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper

@TaskerInputRoot
data class MQTTActionInput(
    @field:TaskerInputField("brokerUrl", "Broker URL", "The MQTT broker URL (e.g., mqtt.example.com)")
    val brokerUrl: String,

    @field:TaskerInputField("port", "Port", "The MQTT broker port (default: 1883)")
    val port: String,

    @field:TaskerInputField("clientId", "Client ID", "Optional unique identifier for this client")
    val clientId: String? = null,

    @field:TaskerInputField("topic", "Topic", "The MQTT topic to publish to")
    val topic: String,

    @field:TaskerInputField("message", "Message", "The message to publish")
    val message: String,

    @field:TaskerInputField("qos", "QoS Level", "Quality of Service level (0-2)")
    val qos: Int = 1,

    @field:TaskerInputField("retained", "Retained", "Whether the message should be retained by the broker")
    val retained: Boolean = false,

    @field:TaskerInputField("useSsl", "Use SSL/TLS", "Whether to use SSL/TLS encryption")
    val useSsl: Boolean = false,

    @field:TaskerInputField("username", "Username", "Optional authentication username")
    val username: String? = null,

    @field:TaskerInputField("password", "Password", "Optional authentication password")
    val password: String? = null
) {
    override fun toString(): String = "Send MQTT message to $topic@$brokerUrl"
}
