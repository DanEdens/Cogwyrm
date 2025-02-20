package com.madness.mqttcommander.tasker

import android.content.Context
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import com.madness.mqttcommander.mqtt.MQTTService

// Input configuration for our DARK RITUAL
data class MQTTActionInput(
    val brokerUrl: String,
    val port: Int,
    val clientId: String,
    val useSSL: Boolean,
    val topic: String,
    val message: String
)

// The SUMMONER of MQTT messages
class MQTTActionRunner(private val context: Context) {
    fun run(input: TaskerInput<MQTTActionInput>): TaskerPluginResult<Unit> {
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

// Configuration activity for the RITUAL
class MQTTActionHelper(config: TaskerPluginConfig<MQTTActionInput>) : TaskerPluginConfigHelper<MQTTActionInput, MQTTActionRunner, Unit>(config) {
    override val inputClass = MQTTActionInput::class.java
    override val runnerClass = MQTTActionRunner::class.java
    override fun isInputValid(input: TaskerInput<MQTTActionInput>) = input.regular.brokerUrl.isNotEmpty() && input.regular.topic.isNotEmpty()
}
