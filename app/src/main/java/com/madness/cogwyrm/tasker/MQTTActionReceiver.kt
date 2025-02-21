package com.cogwyrm.app.tasker

import android.content.Context
import android.content.Intent
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSuccess
import com.cogwyrm.app.MQTTService
import com.cogwyrm.app.R

class MQTTActionReceiver : TaskerPluginRunnerAction<MQTTActionInput, Unit>() {
    override val notificationProperties by lazy {
        NotificationProperties(
            iconResId = R.drawable.ic_arrow_upward,
            titleResId = R.string.tasker_notification_title,
            textResId = R.string.tasker_notification_text
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
            TaskerPluginResultSuccess()
        } catch (e: Exception) {
            TaskerPluginResultError(e)
        }
    }
}
