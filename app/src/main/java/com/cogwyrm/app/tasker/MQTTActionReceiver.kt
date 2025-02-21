package com.cogwyrm.app.tasker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cogwyrm.app.mqtt.MQTTClient
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputInfo
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputInfos

class MQTTActionReceiver : BroadcastReceiver() {
    private val taskerPlugin by lazy { MQTTActionRunner() }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.twofortyfouram.locale.intent.action.FIRE_SETTING") {
            val bundle = intent.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE")
            val input = TaskerInput(MQTTActionInput::class.java, bundle)
            taskerPlugin.run(context, input)
        }
    }

    private class MQTTActionRunner : TaskerPluginRunnerAction<MQTTActionInput, Unit>() {
        override fun run(context: Context, input: TaskerInput<MQTTActionInput>): TaskerPluginResult<Unit> {
            val mqttClient = MQTTClient(
                input.regular.brokerUrl,
                input.regular.port,
                input.regular.clientId ?: "",
                input.regular.useSsl
            )

            return try {
                mqttClient.connect()
                mqttClient.publish(input.regular.topic, input.regular.message)
                mqttClient.disconnect()
                TaskerPluginResultSucess(Unit)
            } catch (e: Exception) {
                TaskerPluginResultError(e)
            }
        }
    }
}
