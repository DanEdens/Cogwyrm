package com.cogwyrm.app.tasker

import android.os.Bundle
import android.os.Parcelable
import com.cogwyrm.app.utils.CogwyrmError
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.input.TaskerPluginInput
import kotlinx.parcelize.Parcelize
import java.net.URI
import java.util.UUID

@Parcelize
@TaskerInputRoot
data class MQTTEventInput(
    @TaskerInputField("brokerUrl", label = "Broker URL") var brokerUrl: String = "",
    @TaskerInputField("port", label = "Port") var port: Int = 1883,
    @TaskerInputField("useSSL", label = "Use SSL/TLS") var useSSL: Boolean = false,
    @TaskerInputField("username", label = "Username (Optional)") var username: String = "",
    @TaskerInputField("password", label = "Password (Optional)") var password: String = "",
    @TaskerInputField("clientId", label = "Client ID") var clientId: String = UUID.randomUUID().toString(),
    @TaskerInputField("topic", label = "Topic") var topic: String = "",
    @TaskerInputField("qos", label = "QoS (0-2)") var qos: Int = 0,
    @TaskerInputField("cleanSession", label = "Clean Session") var cleanSession: Boolean = true,
    @TaskerInputField("keepAlive", label = "Keep Alive (seconds)") var keepAlive: Int = 60,
    @TaskerInputField("connectionTimeout", label = "Connection Timeout (seconds)") var connectionTimeout: Int = 30,
    @TaskerInputField("autoReconnect", label = "Auto Reconnect") var autoReconnect: Boolean = true,
    @TaskerInputField("maxReconnectDelay", label = "Max Reconnect Delay (seconds)") var maxReconnectDelay: Int = 128
) : TaskerPluginInput {

    override fun validateInput(): Boolean {
        try {
            validateBrokerUrl()
            validatePort()
            validateTopic()
            validateQoS()
            validateConnectionParams()
            return true
        } catch (e: CogwyrmError) {
            return false
        }
    }

    private fun validateBrokerUrl() {
        if (brokerUrl.isBlank()) {
            throw CogwyrmError.ValidationError("Broker URL is required")
        }
        try {
            val uri = URI(if (brokerUrl.startsWith("tcp://") || brokerUrl.startsWith("ssl://")) brokerUrl else "tcp://$brokerUrl")
            if (uri.host.isNullOrBlank()) {
                throw CogwyrmError.ValidationError("Invalid broker URL format")
            }
        } catch (e: Exception) {
            throw CogwyrmError.ValidationError("Invalid broker URL: ${e.message}")
        }
    }

    private fun validatePort() {
        if (port !in 1..65535) {
            throw CogwyrmError.ValidationError("Port must be between 1 and 65535")
        }
    }

    private fun validateTopic() {
        if (topic.isBlank()) {
            throw CogwyrmError.ValidationError("Topic is required")
        }
        if (topic.contains("+") || topic.contains("#")) {
            // Allow wildcards but validate their usage
            if (topic.contains("++") || topic.contains("##")) {
                throw CogwyrmError.ValidationError("Invalid wildcard usage in topic")
            }
            if (topic.contains("#") && !topic.endsWith("#")) {
                throw CogwyrmError.ValidationError("# wildcard must be at the end of the topic")
            }
        }
    }

    private fun validateQoS() {
        if (qos !in 0..2) {
            throw CogwyrmError.ValidationError("QoS must be 0, 1, or 2")
        }
    }

    private fun validateConnectionParams() {
        if (keepAlive < 0) {
            throw CogwyrmError.ValidationError("Keep alive must be non-negative")
        }
        if (connectionTimeout < 0) {
            throw CogwyrmError.ValidationError("Connection timeout must be non-negative")
        }
        if (maxReconnectDelay < 0) {
            throw CogwyrmError.ValidationError("Max reconnect delay must be non-negative")
        }
    }

    override fun toBundle(): Bundle = Bundle().apply {
        putString("brokerUrl", brokerUrl)
        putInt("port", port)
        putBoolean("useSSL", useSSL)
        putString("username", username)
        putString("password", password)
        putString("clientId", clientId)
        putString("topic", topic)
        putInt("qos", qos)
        putBoolean("cleanSession", cleanSession)
        putInt("keepAlive", keepAlive)
        putInt("connectionTimeout", connectionTimeout)
        putBoolean("autoReconnect", autoReconnect)
        putInt("maxReconnectDelay", maxReconnectDelay)
    }

    companion object {
        fun generateClientId(): String = "cogwyrm-${UUID.randomUUID()}"
    }
}
