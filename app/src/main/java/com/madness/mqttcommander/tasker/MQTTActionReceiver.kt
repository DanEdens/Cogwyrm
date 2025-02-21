package com.madness.mqttcommander.tasker

import android.content.Context
import android.content.Intent
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSuccess
import com.madness.mqttcommander.MQTTService
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery
import com.madness.mqttcommander.R

class MQTTActionReceiver : TaskerPluginRunnerAction<MQTTActionInput, Unit>() {
    override val notificationProperties by lazy {
        NotificationProperties(
            iconResId = R.mipmap.ic_launcher,
            titleResId = R.string.tasker_notification_title,
            textResId = R.string.tasker_notification_text,
            iconUsesActionColor = true
        )
    }

    override fun run(context: Context, input: TaskerInput<MQTTActionInput>): TaskerPluginResult<Unit> {
        return try {
            val intent = Intent(context, MQTTService::class.java)
            context.startService(intent)

            val service = MQTTService()
            service.connect(
                input.regular.brokerUrl,
                input.regular.port.toInt(),
                input.regular.clientId,
                input.regular.useSsl
            )
            service.publish(input.regular.topic, input.regular.message)
            TaskerPluginResultSuccess(Unit)
        } catch (e: Exception) {
            TaskerPluginResultError(e)
        }
    }
}
