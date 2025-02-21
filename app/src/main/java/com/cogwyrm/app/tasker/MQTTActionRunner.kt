package com.cogwyrm.app.tasker

import android.content.Context
import com.cogwyrm.app.MQTTService
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import kotlinx.coroutines.*

class MQTTActionRunner : TaskerPluginRunnerAction<MQTTActionInput, Unit>() {
    override fun run(context: Context, input: TaskerInput<MQTTActionInput>): TaskerPluginResult<Unit> {
        return runBlocking {
            try {
                val service = MQTTService()

                // Connect to broker
                service.connect(
                    serverUri = input.regular.brokerUrl,
                    port = input.regular.port.toInt(),
                    clientId = input.regular.clientId,
                    useSsl = input.regular.useSsl,
                    username = input.regular.username,
                    password = input.regular.password
                )

                // Wait for connection
                delay(2000)

                if (!service.isConnected()) {
                    throw Exception("Failed to connect to MQTT broker")
                }

                // Publish message
                service.publish(
                    topic = input.regular.topic,
                    message = input.regular.message,
                    qos = input.regular.qos,
                    retained = input.regular.retained
                )

                // Wait for message to be published
                delay(1000)

                // Cleanup
                service.disconnect()

                TaskerPluginResultSucess(Unit)
            } catch (e: Exception) {
                TaskerPluginResultError(0, "Failed to publish message: ${e.message}")
            }
        }
    }
}
