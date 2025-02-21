package com.cogwyrm.app.tasker

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.SimpleResult
import com.joaomgcd.taskerpluginlibrary.SimpleResultError
import com.joaomgcd.taskerpluginlibrary.SimpleResultSuccess

class MQTTTaskerAction(config: TaskerPluginConfig<MQTTActionInput>) : TaskerPluginConfigHelper<MQTTActionInput, Unit, Runner>(config) {
    override val inputClass = MQTTActionInput::class.java
    override val outputClass = Unit::class.java
    override val runnerClass = Runner::class.java

    override fun addToStringBlurb(input: TaskerInput<MQTTActionInput>, blurbBuilder: StringBuilder) {
        blurbBuilder.append("Send MQTT message to ${input.regular.topic}@${input.regular.brokerUrl}")
    }

    override fun isInputValid(input: TaskerInput<MQTTActionInput>): SimpleResult {
        val errors = mutableListOf<String>()

        with(input.regular) {
            if (brokerUrl.isBlank() || !brokerUrl.matches(Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"))) {
                errors.add("Invalid broker URL")
            }

            if (port.isBlank() || port.toIntOrNull() !in 1..65535) {
                errors.add("Port must be between 1 and 65535")
            }

            if (topic.isBlank() || !topic.matches(Regex("^[a-zA-Z0-9/#+]+$"))) {
                errors.add("Invalid topic format")
            }

            if (message.isBlank()) {
                errors.add("Message cannot be empty")
            }
        }

        return if (errors.isEmpty()) {
            SimpleResultSuccess()
        } else {
            SimpleResultError(IllegalArgumentException(errors.joinToString("\n")))
        }
    }
}
