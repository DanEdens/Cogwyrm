package com.madness.mqttcommander.tasker

import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.madness.mqttcommander.mqtt.MQTTService

// Input configuration for our DARK RITUAL
class MQTTActionInput(
    val topic: String,
    val message: String,
    val retain: Boolean = false,
    val qos: Int = 1
)

// The SUMMONER of MQTT messages
class MQTTActionRunner : TaskerPluginRunnerAction<MQTTActionInput, Unit>() {
    override fun run(context: android.content.Context, input: TaskerInput<MQTTActionInput>): TaskerPluginResult<Unit> {
        // Get our service connection
        val mqttService = MQTTService.getInstance(context)

        // UNLEASH THE MESSAGE
        mqttService.publish(
            input.regular.topic,
            input.regular.message,
            input.regular.qos,
            input.regular.retain
        )

        return TaskerPluginResult(Unit)
    }
}

// Configuration activity for the RITUAL
class MQTTActionConfig : TaskerPluginConfig<MQTTActionInput>() {
    override val runnerClass = MQTTActionRunner::class.java
    override val configHelper = TaskerPluginConfigHelperNoOutput(this)
}
