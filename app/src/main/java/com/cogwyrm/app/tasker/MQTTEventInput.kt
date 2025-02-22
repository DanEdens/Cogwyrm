package com.cogwyrm.app.tasker

import android.os.Bundle
import android.os.Parcelable
import com.cogwyrm.app.utils.CogwyrmError
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.input.TaskerPluginInput
import kotlinx.parcelize.Parcelize

@Parcelize
@TaskerInputRoot
data class MQTTEventInput(
    @TaskerInputField("brokerUrl", label = "Broker URL") var brokerUrl: String = "",
    @TaskerInputField("port", label = "Port") var port: Int = 1883,
    @TaskerInputField("useSSL", label = "Use SSL/TLS") var useSSL: Boolean = false,
    @TaskerInputField("username", label = "Username (Optional)") var username: String = "",
    @TaskerInputField("password", label = "Password (Optional)") var password: String = "",
    @TaskerInputField("clientId", label = "Client ID") var clientId: String = "",
    @TaskerInputField("topic", label = "Topic") var topic: String = "",
    @TaskerInputField("qos", label = "QoS (0-2)") var qos: Int = 0
) : TaskerPluginInput {

    override fun validateInput(): Boolean {
        try {
            if (brokerUrl.isBlank()) throw CogwyrmError.ValidationError("Broker URL is required")
            if (port !in 1..65535) throw CogwyrmError.ValidationError("Port must be between 1 and 65535")
            if (topic.isBlank()) throw CogwyrmError.ValidationError("Topic is required")
            if (qos !in 0..2) throw CogwyrmError.ValidationError("QoS must be 0, 1, or 2")
            return true
        } catch (e: CogwyrmError) {
            return false
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
    }
}
