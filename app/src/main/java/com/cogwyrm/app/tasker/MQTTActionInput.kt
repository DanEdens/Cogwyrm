package com.cogwyrm.app.tasker

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
data class MQTTActionInput(
    @field:TaskerInputField("brokerUrl", "Broker URL") val brokerUrl: String,
    @field:TaskerInputField("port", "Port") val port: String = "1883",
    @field:TaskerInputField("clientId", "Client ID") val clientId: String? = null,
    @field:TaskerInputField("topic", "Topic") val topic: String,
    @field:TaskerInputField("message", "Message") val message: String,
    @field:TaskerInputField("useSsl", "Use SSL") val useSsl: Boolean = false
)
