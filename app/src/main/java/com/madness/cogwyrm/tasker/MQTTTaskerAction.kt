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

// Configuration activity for the RITUAL
class MQTTActionHelper(config: TaskerPluginConfig<MQTTActionInput>) : TaskerPluginConfigHelper<MQTTActionInput, MQTTActionReceiver, Unit>(config) {
    override val runnerClass = MQTTActionReceiver::class.java
    override val inputClass = MQTTActionInput::class.java

    override fun isInputValid(input: TaskerInput<MQTTActionInput>): TaskerPluginResult<Unit> {
        if (input.regular.brokerUrl.isEmpty()) {
            return TaskerPluginResultError("Broker URL cannot be empty")
        }
        if (input.regular.topic.isEmpty()) {
            return TaskerPluginResultError("Topic cannot be empty")
        }
        return TaskerPluginResultSuccess()
    }
}
