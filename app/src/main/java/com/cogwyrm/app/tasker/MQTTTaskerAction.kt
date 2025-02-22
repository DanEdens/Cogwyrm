package com.cogwyrm.app.tasker

import android.content.Context
import android.os.Bundle
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.cogwyrm.app.utils.CogwyrmError
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

class MQTTActionRunner : TaskerPluginRunnerAction<MQTTEventInput, MQTTEventOutput>() {
    override fun run(context: Context, input: TaskerInput<MQTTEventInput>): TaskerPluginResult<MQTTEventOutput> {
        return try {
            // TODO: Implement actual MQTT action
            TaskerPluginResult.Success(MQTTEventOutput("", "", System.currentTimeMillis()))
        } catch (e: CogwyrmError) {
            TaskerPluginResult.Error(e.message ?: "Unknown error")
        }
    }
}

@Parcelize
data class MQTTEventInput(
    val brokerUrl: String,
    val port: Int,
    val clientId: String,
    val topic: String,
    val qos: Int,
    val useSsl: Boolean,
    val username: String?,
    val password: String?
) : TaskerPluginInput, Parcelable {
    override fun validate(): Boolean {
        return brokerUrl.isNotEmpty() && port > 0 && topic.isNotEmpty()
    }

    override fun toBundle(): Bundle {
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
