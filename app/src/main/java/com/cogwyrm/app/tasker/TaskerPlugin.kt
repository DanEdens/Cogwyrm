package com.cogwyrm.app.tasker

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

interface TaskerPluginInput {
    fun validate(): Boolean
}

interface TaskerPluginOutput {
    fun toBundle(): Bundle
}

abstract class TaskerPluginConfigActivity<TInput : TaskerPluginInput, TOutput : TaskerPluginOutput, TActionRunner : TaskerPluginRunner<TInput, TOutput>, THelper : TaskerPluginConfigHelper<TInput, TOutput, TActionRunner>> : AppCompatActivity(), TaskerPluginConfig<TInput> {
    abstract fun getNewHelper(config: TaskerPluginConfig<TInput>): THelper

    protected val taskerHelper by lazy { getNewHelper(this) }

    override val context get() = applicationContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskerHelper.onCreate()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0) {
            val result = taskerHelper.onBackPressed()
            if (result is TaskerPluginResultError) {
                Toast.makeText(this, "Settings are not valid:\n\n${result.message}", Toast.LENGTH_LONG).show()
            }
            return result.success
        } else super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
        // Do nothing, override back press handling
    }
}

abstract class TaskerPluginRunner<I : TaskerPluginInput, O : TaskerPluginOutput> {
    abstract fun run(context: Context, input: I): O
}

abstract class TaskerPluginReceiver<I : TaskerPluginInput, O : TaskerPluginOutput> {
    abstract fun onReceive(context: Context, input: I): O
}
