package com.cogwyrm.app.tasker

import android.content.Context
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginRunnerConditionEvent
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultCondition
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionSatisfied
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionUnsatisfied

class MQTTEventRunner : TaskerPluginRunnerConditionEvent<MQTTEventInput, MQTTEventOutput>() {
    override fun getSatisfiedCondition(context: Context, input: MQTTEventInput, update: MQTTEventOutput?): TaskerPluginResultCondition<MQTTEventOutput> {
        return if (update != null) {
            TaskerPluginResultConditionSatisfied(context, update)
        } else {
            TaskerPluginResultConditionUnsatisfied()
        }
    }
}

data class MQTTEventUpdate(
    val topic: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
