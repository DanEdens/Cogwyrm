package com.cogwyrm.app.tasker

import android.content.Context
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSuccess

class MQTTEventHelper(private val context: Context) : TaskerPluginConfigHelper<MQTTEventInput, MQTTEventOutput, MQTTEventRunner>(context) {
    override val runnerClass = MQTTEventRunner::class.java
    override val inputClass = MQTTEventInput::class.java
    override val outputClass = MQTTEventOutput::class.java

    override fun isInputValid(input: TaskerInput<MQTTEventInput>): TaskerPluginResult<MQTTEventOutput> {
        val config = input.regular
        return if (config.validateInput()) {
            TaskerPluginResultSuccess()
        } else {
            TaskerPluginResultError(IllegalArgumentException("Invalid configuration"))
        }
    }

    override fun getDefaultInput(): TaskerInput<MQTTEventInput> = TaskerInput(MQTTEventInput())
}
