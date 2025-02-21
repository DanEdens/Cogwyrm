package com.cogwyrm.app.tasker

import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable

@TaskerOutputObject
class MQTTEventOutput(
    @get:TaskerOutputVariable("topic", label = "Topic", hint = "The MQTT topic that triggered the event")
    val topic: String,

    @get:TaskerOutputVariable("message", label = "Message", hint = "The message content received")
    val message: String,

    @get:TaskerOutputVariable("qos", label = "QoS", hint = "Quality of Service level")
    val qos: Int,

    @get:TaskerOutputVariable("retained", label = "Retained", hint = "Whether the message was retained")
    val retained: Boolean,

    @get:TaskerOutputVariable("timestamp", label = "Timestamp", hint = "When the message was received")
    val timestamp: Long
)
