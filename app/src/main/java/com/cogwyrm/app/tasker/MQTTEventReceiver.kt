package com.cogwyrm.app.tasker

import android.content.Context
import android.content.Intent
import android.util.Log
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSuccess

class MQTTEventReceiver(
    private val context: Context,
    private val config: TaskerPluginConfig<MQTTEventInput>
) : TaskerPluginRunnerAction<MQTTEventInput, MQTTEventOutput>() {

    override fun run(input: TaskerInput<MQTTEventInput>): TaskerPluginResult<MQTTEventOutput> {
        val mqttInput = input.regular
        return try {
            if (!mqttInput.isValid()) {
                return TaskerPluginResultError(IllegalArgumentException("Invalid MQTT configuration"))
            }
            handleOutput(mqttInput)
        } catch (e: Exception) {
            Log.e("MQTTEventReceiver", "Error processing MQTT event", e)
            TaskerPluginResultError(e)
        }
    }

    private fun handleOutput(output: MQTTEventInput): TaskerPluginResult<MQTTEventOutput> {
        val intent = Intent("com.cogwyrm.app.tasker.ACTION_MQTT_MESSAGE").apply {
            putExtra("topic", output.topic)
            putExtra("payload", output.payload)
        }
        context.sendBroadcast(intent)
        return TaskerPluginResultSuccess(MQTTEventOutput(output.topic, output.payload))
    }
}
