package com.madness.mqttcommander.tasker

import android.content.Context
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import com.madness.mqttcommander.MQTTService

class MQTTActionReceiver : TaskerPluginRunnerAction<MQTTActionInput, Unit>() {
    override val notificationProperties get() = NotificationProperties()

    override fun run(context: Context, input: TaskerInput<MQTTActionInput>): TaskerPluginResult<Unit> {
        return try {
            val service = MQTTService()
            service.connect(
                input.regular.brokerUrl,
                input.regular.port,
                input.regular.clientId,
                input.regular.useSSL
            )
            service.publish(input.regular.topic, input.regular.message)
            TaskerPluginResultSucess()
        } catch (e: Exception) {
            TaskerPluginResultError(e)
        }
    }
}
