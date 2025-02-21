package com.cogwyrm.app.tasker

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.SimpleResult
import com.joaomgcd.taskerpluginlibrary.SimpleResultError
import com.joaomgcd.taskerpluginlibrary.SimpleResultSuccess

class MQTTTaskerAction(config: TaskerPluginConfig<MQTTActionInput>) : TaskerPluginConfigHelper<MQTTActionInput, Unit, MQTTActionReceiver>(config) {
    override val runnerClass = MQTTActionReceiver::class.java
    override val inputClass = MQTTActionInput::class.java
    override val outputClass = Unit::class.java

    override fun isInputValid(input: TaskerInput<MQTTActionInput>): SimpleResult {
        if (input.regular.brokerUrl.isEmpty()) {
            return SimpleResultError(IllegalArgumentException("Broker URL cannot be empty"))
        }
        if (input.regular.topic.isEmpty()) {
            return SimpleResultError(IllegalArgumentException("Topic cannot be empty"))
        }
        return SimpleResultSuccess()
    }
}
