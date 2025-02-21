package com.cogwyrm.app.tasker

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSuccess

class MQTTTaskerAction : TaskerPluginConfigHelper<MQTTActionInput, Unit, MQTTActionReceiver>() {
    override val runnerClass = MQTTActionReceiver::class.java
    override val inputClass = MQTTActionInput::class.java
    override val outputClass = Unit::class.java

    override fun isInputValid(input: TaskerInput<MQTTActionInput>): TaskerPluginResult<Unit> {
        if (input.regular.brokerUrl.isEmpty()) {
            return TaskerPluginResultError(Exception("Broker URL cannot be empty"))
        }
        if (input.regular.topic.isEmpty()) {
            return TaskerPluginResultError(Exception("Topic cannot be empty"))
        }
        return TaskerPluginResultSuccess()
    }
}
