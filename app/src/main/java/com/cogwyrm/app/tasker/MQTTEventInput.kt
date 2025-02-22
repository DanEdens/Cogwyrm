package com.cogwyrm.app.tasker

import android.os.Bundle
import android.os.Parcelable
import com.cogwyrm.app.utils.CogwyrmError
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputObject
import kotlinx.parcelize.Parcelize
import java.net.URI
import java.util.UUID

@Parcelize
@TaskerInputRoot
@TaskerInputObject(key = "mqtt_event_input")
data class MQTTEventInput(
    @TaskerInputField("broker_url")
    val brokerUrl: String = "",

    @TaskerInputField("port")
    val port: Int = 1883,

    @TaskerInputField("client_id")
    val clientId: String = generateClientId(),

    @TaskerInputField("topic")
    val topic: String = "",

    @TaskerInputField("qos")
    val qos: Int = 0,

    @TaskerInputField("clean_session")
    val cleanSession: Boolean = true,

    @TaskerInputField("keep_alive")
    val keepAlive: Int = 60,

    @TaskerInputField("connection_timeout")
    val connectionTimeout: Int = 30,

    @TaskerInputField("use_ssl")
    val useSSL: Boolean = false,

    @TaskerInputField("username")
    val username: String? = null,

    @TaskerInputField("password")
    val password: String? = null,

    @TaskerInputField("auto_reconnect")
    val autoReconnect: Boolean = true,

    @TaskerInputField("max_reconnect_delay")
    val maxReconnectDelay: Int = 1000
) : Parcelable {

    fun validateInput(): Boolean {
        return validateBrokerUrl() && validatePort() && validateTopic() && validateQoS() && validateConnectionParams()
    }

    private fun validateBrokerUrl(): Boolean = brokerUrl.isNotBlank()

    private fun validatePort(): Boolean = port in 1..65535

    private fun validateTopic(): Boolean = topic.isNotBlank()

    private fun validateQoS(): Boolean = qos in 0..2

    private fun validateConnectionParams(): Boolean {
        return keepAlive > 0 && connectionTimeout > 0 && maxReconnectDelay > 0
    }

    fun toBundle(): Bundle {
        return Bundle().apply {
            putString("broker_url", brokerUrl)
            putInt("port", port)
            putString("client_id", clientId)
            putString("topic", topic)
            putInt("qos", qos)
            putBoolean("clean_session", cleanSession)
            putInt("keep_alive", keepAlive)
            putInt("connection_timeout", connectionTimeout)
            putBoolean("use_ssl", useSSL)
            putString("username", username)
            putString("password", password)
            putBoolean("auto_reconnect", autoReconnect)
            putInt("max_reconnect_delay", maxReconnectDelay)
        }
    }

    companion object {
        fun generateClientId(): String = "cogwyrm_${System.currentTimeMillis()}"
    }
}
