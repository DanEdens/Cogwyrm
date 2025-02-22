package com.cogwyrm.app.tasker

import android.os.Bundle

data class MQTTEventInput(
    val brokerUrl: String,
    val port: Int,
    val clientId: String?,
    val topic: String,
    val qos: Int,
    val useSsl: Boolean,
    val username: String?,
    val password: String?
) : TaskerPluginInput {
    override fun validate(): Boolean {
        return brokerUrl.isNotBlank() && port > 0 && topic.isNotBlank()
    }

    companion object {
        fun fromBundle(bundle: Bundle): MQTTEventInput {
            return MQTTEventInput(
                brokerUrl = bundle.getString("brokerUrl", ""),
                port = bundle.getInt("port", 1883),
                clientId = bundle.getString("clientId"),
                topic = bundle.getString("topic", ""),
                qos = bundle.getInt("qos", 0),
                useSsl = bundle.getBoolean("useSsl", false),
                username = bundle.getString("username"),
                password = bundle.getString("password")
            )
        }
    }

    fun toBundle(): Bundle {
        return Bundle().apply {
            putString("brokerUrl", brokerUrl)
            putInt("port", port)
            putString("clientId", clientId)
            putString("topic", topic)
            putInt("qos", qos)
            putBoolean("useSsl", useSsl)
            putString("username", username)
            putString("password", password)
        }
    }
}
