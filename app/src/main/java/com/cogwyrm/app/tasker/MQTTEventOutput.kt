package com.cogwyrm.app.tasker

import android.os.Bundle
import android.os.Parcelable
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable
import kotlinx.parcelize.Parcelize

@TaskerOutputObject
class MQTTEventOutput(
    @get:TaskerOutputVariable("message", "Message", "str") var message: String = "",
    @get:TaskerOutputVariable("topic", "Topic", "str") var topic: String = "",
    @get:TaskerOutputVariable("timestamp", "Timestamp", "int") var timestamp: Long = 0
) : Parcelable {
    fun toBundle(): Bundle {
        return Bundle().apply {
            putString("message", message)
            putString("topic", topic)
            putLong("timestamp", timestamp)
        }
    }
}
