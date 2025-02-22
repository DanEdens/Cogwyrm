package com.cogwyrm.app.tasker

import android.os.Bundle
import android.os.Parcelable
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable
import kotlinx.parcelize.Parcelize

@TaskerOutputObject
@Parcelize
data class MQTTEventOutput(
    @get:TaskerOutputVariable("topic")
    val topic: String,

    @get:TaskerOutputVariable("payload")
    val payload: String
) : Parcelable {
    fun toBundle(): Bundle = Bundle().apply {
        putString("topic", topic)
        putString("payload", payload)
    }
}
