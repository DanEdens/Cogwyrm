package com.cogwyrm.app.tasker

import android.os.Bundle
import android.os.Parcelable
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
@TaskerOutputObject
data class MQTTEventOutput(
    @get:TaskerOutputVariable("message", "Message", "str") var message: String = "",
    @get:TaskerOutputVariable("topic", "Topic", "str") var topic: String = "",
    @get:TaskerOutputVariable("timestamp", "Timestamp (Unix)", "int") var timestamp: Long = System.currentTimeMillis(),
    @get:TaskerOutputVariable("timestamp_formatted", "Timestamp (Formatted)", "str") var timestampFormatted: String = formatTimestamp(System.currentTimeMillis()),
    @get:TaskerOutputVariable("qos", "QoS Level", "int") var qos: Int = 0,
    @get:TaskerOutputVariable("retained", "Is Retained", "bool") var retained: Boolean = false
) : Parcelable {
    fun toBundle(): Bundle {
        return Bundle().apply {
            putString("message", message)
            putString("topic", topic)
            putLong("timestamp", timestamp)
            putString("timestamp_formatted", timestampFormatted)
            putInt("qos", qos)
            putBoolean("retained", retained)
        }
    }

    companion object {
        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
}
