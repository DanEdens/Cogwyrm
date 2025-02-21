package com.cogwyrm.app.tasker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.cogwyrm.app.mqtt.MQTTClient
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginRunner
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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
            context = context,
            brokerUrl = input.regular.brokerUrl,
            port = input.regular.port,
            clientId = input.regular.clientId ?: "",
            useSsl = input.regular.useSsl
        )

        return try {
            var connected = false
            var error: Exception? = null

            mqttClient.connect(object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    connected = true
                    try {
                        mqttClient.publish(input.regular.topic, input.regular.message)
                    } finally {
                        mqttClient.disconnect()
                    }
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    error = exception as? Exception ?: Exception("Failed to connect")
                }
            })

            // Wait briefly for connection
            Thread.sleep(2000)

            if (error != null) {
                throw error as Exception
            }

            if (!connected) {
                throw Exception("Connection timeout")
            }

            TaskerPluginResultSucess(Unit)
        } catch (e: Exception) {
            Log.e("MQTTActionReceiver", "Error executing MQTT action", e)
            TaskerPluginResultError(e)
        }
    }
}
