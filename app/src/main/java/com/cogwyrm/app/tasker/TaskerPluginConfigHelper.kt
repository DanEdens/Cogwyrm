package com.cogwyrm.app.tasker

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginRunner
import com.joaomgcd.taskerpluginlibrary.SimpleResult
import android.app.Activity

abstract class TaskerPluginConfigHelper<TInput : Any, TOutput : Any, TRunner : TaskerPluginRunner<TInput, TOutput>>(
    private val config: TaskerPluginConfig<TInput>
) {
    fun isInputValid(): SimpleResult {
        return try {
            val input = config.getInputForTasker()
            if (input.regular.validateInput()) {
                SimpleResult.Success()
            } else {
                SimpleResult.Error("Invalid input")
            }
        } catch (e: Exception) {
            SimpleResult.Error(e.message ?: "Unknown error")
        }
    }

    fun finishForTasker(): SimpleResult {
        val result = isInputValid()
        if (result is SimpleResult.Success) {
            config.setResult(Activity.RESULT_OK, config.getInputForTasker().toIntent())
            config.finish()
        }
        return result
    }

    fun getDefaultInput(): TaskerInput<TInput> {
        return TaskerInput(config.context)
    }
}
