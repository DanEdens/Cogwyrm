package com.cogwyrm.app.tasker

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
data class MQTTActionInput(
    @field:TaskerInputField("Broker URL") val brokerUrl: String,
    @field:TaskerInputField("Port") val port: String = "1883",
    @field:TaskerInputField("Client ID") val clientId: String? = null,
    @field:TaskerInputField("Topic") val topic: String,
    @field:TaskerInputField("Message") val message: String,
    @field:TaskerInputField("Use SSL") val useSsl: Boolean = false
)
