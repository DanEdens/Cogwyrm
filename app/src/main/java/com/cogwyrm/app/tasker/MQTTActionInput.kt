package com.cogwyrm.app.tasker

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper

@TaskerInputRoot
data class MQTTActionInput(
    @TaskerInputField(key = "brokerUrl", labelResId = 0)
    val brokerUrl: String,

    @TaskerInputField(key = "port", labelResId = 0)
    val port: String,

    @TaskerInputField(key = "clientId", labelResId = 0)
    val clientId: String? = null,

    @TaskerInputField(key = "topic", labelResId = 0)
    val topic: String,

    @TaskerInputField(key = "message", labelResId = 0)
    val message: String,

    @TaskerInputField(key = "qos", labelResId = 0)
    val qos: Int = 1,

    @TaskerInputField(key = "retained", labelResId = 0)
    val retained: Boolean = false,

    @TaskerInputField(key = "useSsl", labelResId = 0)
    val useSsl: Boolean = false,

    @TaskerInputField(key = "username", labelResId = 0)
    val username: String? = null,

    @TaskerInputField(key = "password", labelResId = 0)
    val password: String? = null
) {
    override fun toString(): String = "Send MQTT message to $topic@$brokerUrl"
}
