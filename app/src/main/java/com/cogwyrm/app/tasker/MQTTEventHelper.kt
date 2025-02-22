package com.cogwyrm.app.tasker

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

class MQTTEventHelper(config: TaskerPluginConfig<MQTTEventInput>) :
    TaskerPluginConfigHelper<MQTTEventInput, MQTTEventOutput, MQTTEventRunner>(config) {
    override val runnerClass: Class<MQTTEventRunner> = MQTTEventRunner::class.java
    override val inputClass: Class<MQTTEventInput> = MQTTEventInput::class.java
    override val outputClass: Class<MQTTEventOutput> = MQTTEventOutput::class.java

    override fun isInputValid(input: TaskerInput<MQTTEventInput>): TaskerPluginResult<Unit> {
        return if (input.data.brokerUrl.isNotBlank() &&
            input.data.port > 0 &&
            input.data.topic.isNotBlank()
        ) {
            TaskerPluginResultSucess()
        } else {
            TaskerPluginResultError("Broker URL, port, and topic are required")
        }
    }
}
