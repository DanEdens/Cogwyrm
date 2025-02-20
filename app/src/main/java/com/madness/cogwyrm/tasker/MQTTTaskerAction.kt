package com.madness.cogwyrm.tasker

import android.content.Context
import android.content.Intent
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSuccess
import com.madness.cogwyrm.MQTTService

// Input configuration for our DARK RITUAL
data class MQTTActionInput(
    val brokerUrl: String = "",
    val port: String = "1883",
    val clientId: String = "",
    val useSsl: Boolean = false,
    val topic: String = "",
    val message: String = ""
)

// The SUMMONER of MQTT messages
class MQTTActionRunner {
    fun run(context: Context, input: TaskerInput<MQTTActionInput>): TaskerPluginResult<Unit> {
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

// Configuration activity for the RITUAL
class MQTTActionHelper(config: TaskerPluginConfig<MQTTActionInput>) : TaskerPluginConfigHelper<MQTTActionInput, MQTTActionRunner, Unit>(config) {
    override val runnerClass = MQTTActionRunner::class.java
    override val inputClass = MQTTActionInput::class.java
    override val outputClass = Unit::class.java

    override fun isInputValid(input: TaskerInput<MQTTActionInput>): TaskerPluginResult<Unit> {
        if (input.regular.brokerUrl.isEmpty()) {
            return TaskerPluginResultError(Exception("Broker URL cannot be empty"))
        }
        if (input.regular.topic.isEmpty()) {
            return TaskerPluginResultError(Exception("Topic cannot be empty"))
        }
        return TaskerPluginResultSuccess(Unit)
    }
}
