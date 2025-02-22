package com.cogwyrm.app.tasker

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
data class MQTTEventInput(
    @TaskerInputField(key = "brokerUrl", labelResId = 0)
    val brokerUrl: String,

    @TaskerInputField(key = "port", labelResId = 0)
    val port: String,

    @TaskerInputField(key = "clientId", labelResId = 0)
    val clientId: String?,

    @TaskerInputField(key = "topic", labelResId = 0)
    val topic: String,

    @TaskerInputField(key = "qos", labelResId = 0)
    val qos: Int,

    @TaskerInputField(key = "useSsl", labelResId = 0)
    val useSsl: Boolean,

    @TaskerInputField(key = "username", labelResId = 0)
    val username: String?,

    @TaskerInputField(key = "password", labelResId = 0)
    val password: String?
) {
    override fun toString(): String {
        return "MQTTEventInput(brokerUrl='$brokerUrl', port='$port', clientId=$clientId, topic='$topic', qos=$qos, useSsl=$useSsl, username=$username, password=***)"
    }
}
