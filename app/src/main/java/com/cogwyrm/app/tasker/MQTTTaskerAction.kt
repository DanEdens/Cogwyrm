package com.cogwyrm.app.tasker

import android.content.Context
import android.util.Log
import com.cogwyrm.app.mqtt.MQTTClient
import com.cogwyrm.app.mqtt.MQTTException
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSuccess
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MQTTTaskerAction : TaskerPluginRunnerAction<MQTTEventInput, MQTTEventOutput>() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun run(context: Context, input: TaskerInput<MQTTEventInput>): TaskerPluginResult<MQTTEventOutput> {
        val mqttInput = input.regular

        // Validate the input configuration
        if (!mqttInput.isValid()) {
            return TaskerPluginResultError(IllegalArgumentException("Invalid MQTT configuration"))
        }

        // Publish the MQTT message in a background coroutine
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val client = MQTTClient(
                    context = context,
                    brokerUrl = mqttInput.brokerUrl,
                    port = mqttInput.port.toString(),
                    clientId = mqttInput.clientId,
                    useSsl = mqttInput.useSsl,
                    username = mqttInput.username,
                    password = mqttInput.password
                )

                try {
                    // Connect to the MQTT broker
                    client.connect()

                    // Publish the message
                    client.publish(
                        topic = mqttInput.topic,
                        message = mqttInput.payload,
                        qos = mqttInput.qos,
                        retained = false // Default to non-retained messages
                    )

                    Log.d(TAG, "Successfully published message to ${mqttInput.topic}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error publishing MQTT message", e)
                } finally {
                    // Always disconnect when done
                    try {
                        client.disconnect()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error disconnecting MQTT client", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing MQTT client", e)
            }
        }

        // Return success with the output
        val output = MQTTEventOutput(
            topic = mqttInput.topic,
            payload = mqttInput.payload
        )
        return TaskerPluginResultSuccess(output)
    }

    companion object {
        private const val TAG = "MQTTTaskerAction"
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
}
