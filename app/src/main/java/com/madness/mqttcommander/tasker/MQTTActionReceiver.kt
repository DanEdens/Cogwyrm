package com.madness.mqttcommander.tasker

import android.content.Context
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigNoOutput
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

class MQTTActionReceiver : TaskerPluginConfigNoOutput<MQTTActionInput, MQTTActionRunner>() {
    override val context: Context
        get() = applicationContext

    override fun run(input: TaskerInput<MQTTActionInput>): TaskerPluginResult<Unit> {
        return try {
            // Attempt to summon the message into the void
            input.regular.let { config ->
                MQTTService.getInstance(context).publish(
                    config.topic,
                    config.message,
                    config.qos,
                    config.retain
                )
            }
            TaskerPluginResultSucess()
        } catch (e: Exception) {
            TaskerPluginResultError(e.message ?: "THE VOID REJECTED OUR OFFERING")
        }
    }
}
