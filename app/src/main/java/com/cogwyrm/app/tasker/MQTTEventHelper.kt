package com.cogwyrm.app.tasker

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

class MQTTEventHelper(config: TaskerPluginConfig<MQTTEventInput>) : TaskerPluginConfigHelper<MQTTEventInput, MQTTEventOutput, MQTTEventRunner>(config) {
    override val runnerClass = MQTTEventRunner::class.java
    override val inputClass = MQTTEventInput::class.java
    override val outputClass = MQTTEventOutput::class.java
}
