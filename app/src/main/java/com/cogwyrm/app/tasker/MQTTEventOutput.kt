package com.cogwyrm.app.tasker

import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable

@TaskerOutputObject
data class MQTTEventOutput(
    @get:TaskerOutputVariable("topic", labelResId = 0, htmlLabelResId = 0)
    val topic: String,

    @get:TaskerOutputVariable("message", labelResId = 0, htmlLabelResId = 0)
    val message: String,

    @get:TaskerOutputVariable("qos", labelResId = 0, htmlLabelResId = 0)
    val qos: Int,

    @get:TaskerOutputVariable("retained", labelResId = 0, htmlLabelResId = 0)
    val retained: Boolean,

    @get:TaskerOutputVariable("timestamp", labelResId = 0, htmlLabelResId = 0)
    val timestamp: Long
) {
    override fun toString(): String {
        return "MQTTEventOutput(topic='$topic', message='$message', qos=$qos, retained=$retained, timestamp=$timestamp)"
    }
}
