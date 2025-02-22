package com.cogwyrm.app.tasker

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginRunner

abstract class TaskerPluginConfigActivity<TInput : Any> : AppCompatActivity(), TaskerPluginConfig<TInput> {

    override val context: Context
        get() = this

    abstract override fun assignFromInput(input: TaskerInput<TInput>)
    abstract override fun getInputForTasker(): TaskerInput<TInput>

    protected fun finishForTasker() {
        val input = getInputForTasker()
        if (input.regular.validateInput()) {
            setResult(Activity.RESULT_OK, input.toIntent())
            finish()
        } else {
            Toast.makeText(this, "Invalid configuration", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        finishForTasker()
    }
}
