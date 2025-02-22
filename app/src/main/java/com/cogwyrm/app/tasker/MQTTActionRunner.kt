package com.cogwyrm.app.tasker

import android.content.Context
import com.cogwyrm.app.mqtt.MQTTService
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import kotlinx.coroutines.runBlocking

class MQTTActionRunner : TaskerPluginRunnerAction<MQTTActionInput, Unit>() {
    override fun run(context: Context, input: TaskerInput<MQTTActionInput>): TaskerPluginResult<Unit> {
        return try {
            runBlocking {
                val service = MQTTService(context)
                service.connect(
                    brokerUrl = input.regular.brokerUrl,
                    port = input.regular.port.toInt(),
                    clientId = input.regular.clientId,
                    useSsl = input.regular.useSsl,
                    username = input.regular.username,
                    password = input.regular.password
                )

                if (!service.isConnected()) {
                    throw Exception("Failed to connect to MQTT broker")
                }

                service.publish(
                    topic = input.regular.topic,
                    message = input.regular.message,
                    qos = input.regular.qos,
                    retained = input.regular.retained
                )

                service.disconnect()
                TaskerPluginResultSucess()
            }
        } catch (e: Exception) {
            TaskerPluginResultError(0, e.message ?: "Unknown error")
        }
    }
}
