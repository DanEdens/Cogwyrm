package com.cogwyrm.app.tasker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput

abstract class TaskerPluginConfigActivity<TInput : Any> : AppCompatActivity() {
    protected val taskerConfig: TaskerPluginConfig<TInput> by lazy { createTaskerConfig() }

    abstract fun createTaskerConfig(): TaskerPluginConfig<TInput>
    abstract fun createDefaultInput(): TInput

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadInitialInput()
    }

    private fun loadInitialInput() {
        val input = taskerConfig.inputForTasker
        taskerConfig.assignFromInput(input)
    }

    protected fun finishForTasker() {
        val input = taskerConfig.inputForTasker
        if ((input.regular as? MQTTEventInput)?.validateInput() == true) {
            val intent = Intent().apply {
                putExtra("input", (input.regular as MQTTEventInput).toBundle())
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        } else {
            Toast.makeText(this, "Invalid configuration", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        finishForTasker()
    }
}
