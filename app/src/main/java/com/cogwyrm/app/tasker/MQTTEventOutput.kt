package com.cogwyrm.app.tasker

import android.os.Bundle

data class MQTTEventOutput(
    val topic: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) : TaskerPluginOutput {
    override fun toBundle(): Bundle {
        return Bundle().apply {
            putString("topic", topic)
            putString("message", message)
            putLong("timestamp", timestamp)
        }
    }

    companion object {
        fun fromBundle(bundle: Bundle): MQTTEventOutput {
            return MQTTEventOutput(
                topic = bundle.getString("topic", ""),
                message = bundle.getString("message", ""),
                timestamp = bundle.getLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}
