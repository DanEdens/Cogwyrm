package com.cogwyrm.app.tasker

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class MQTTEventInput @JvmOverloads constructor(
    @field:TaskerInputField("brokerUrl", label = "Broker URL") var brokerUrl: String = "",
    @field:TaskerInputField("port", label = "Port") var port: String = "1883",
    @field:TaskerInputField("clientId", label = "Client ID") var clientId: String? = null,
    @field:TaskerInputField("topic", label = "Topic") var topic: String = "",
    @field:TaskerInputField("useSsl", label = "Use SSL") var useSsl: Boolean = false,
    @field:TaskerInputField("username", label = "Username") var username: String? = null,
    @field:TaskerInputField("password", label = "Password") var password: String? = null,
    @field:TaskerInputField("qos", label = "QoS Level") var qos: Int = 1
)
