package com.cogwyrm.app.tasker

import android.content.Context
import android.content.Intent
import android.util.Log
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSuccess
import com.cogwyrm.app.utils.CogwyrmError
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig

class MQTTTaskerAction : TaskerPluginRunnerAction<MQTTEventInput, MQTTEventOutput>() {
    override fun run(context: Context, input: TaskerInput<MQTTEventInput>): TaskerPluginResult<MQTTEventOutput> {
        val mqttInput = input.regular
        return try {
            if (!mqttInput.isValid()) {
                return TaskerPluginResultError(IllegalArgumentException("Invalid MQTT configuration"))
            }

            val output = MQTTEventOutput(
                topic = mqttInput.topic,
                payload = mqttInput.payload
            )
            TaskerPluginResultSuccess(output)
        } catch (e: Exception) {
            Log.e("MQTTTaskerAction", "Error processing MQTT action", e)
            TaskerPluginResultError(e)
        }
    }
}

data class MQTTEventInput(
    val brokerUrl: String,
    val port: Int,
    val clientId: String,
    val topic: String,
    val payload: String,
    val qos: Int = 0,
    val useSsl: Boolean = false,
    val username: String? = null,
    val password: String? = null
) {
    fun isValid(): Boolean {
        return brokerUrl.isNotEmpty() &&
                port in 1..65535 &&
                clientId.isNotEmpty() &&
                topic.isNotEmpty() &&
                qos in 0..2
    }
}

data class MQTTEventOutput(
    val topic: String,
    val payload: String
)

class MQTTTaskerActionConfig(context: Context) : TaskerPluginConfig<MQTTEventInput>(context) {
    override val inputClass = MQTTEventInput::class.java
    override val runnerClass = MQTTTaskerAction::class.java

    override fun assignFromInput(input: TaskerInput<MQTTEventInput>) {
        if (!input.regular.isValid()) {
            throw IllegalArgumentException("Invalid input configuration")
        }
    }

    override fun getIntent(): Intent? = null

    override fun finish() {
        // Nothing to clean up
    }

    override fun setResult(resultCode: Int, data: Intent) {
        // Handle result if needed
    }
}
