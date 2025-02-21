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
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginRunner

class MQTTActionReceiver : BroadcastReceiver() {
    private val runner = Runner()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.twofortyfouram.locale.intent.action.FIRE_SETTING") {
            val bundle = intent.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE")
            if (bundle != null) {
                val input = TaskerInput(MQTTActionInput(
                    brokerUrl = bundle.getString("brokerUrl") ?: "",
                    port = bundle.getString("port") ?: "1883",
                    clientId = bundle.getString("clientId"),
                    topic = bundle.getString("topic") ?: "",
                    message = bundle.getString("message") ?: "",
                    useSsl = bundle.getBoolean("useSsl", false)
                ))
                runner.run(context, input)
            }
        }
    }
}

class Runner : TaskerPluginRunnerAction<MQTTActionInput, Unit>() {
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
