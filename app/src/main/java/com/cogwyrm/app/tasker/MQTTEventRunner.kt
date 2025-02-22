package com.cogwyrm.app.tasker

import android.content.Context
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginRunnerConditionEvent
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginResultCondition
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginResultConditionSatisfied
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginResultConditionUnsatisfied
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput

class MQTTEventRunner : TaskerPluginRunnerConditionEvent<MQTTEventInput, MQTTEventOutput, MQTTEventUpdate>() {
    override fun getSatisfiedCondition(
        context: Context,
        input: TaskerInput<MQTTEventInput>,
        update: MQTTEventUpdate?
    ): TaskerPluginResultCondition<MQTTEventOutput> {
        if (update == null) {
            return TaskerPluginResultConditionUnsatisfied()
        }

        return TaskerPluginResultConditionSatisfied(
            MQTTEventOutput(
                topic = update.topic,
                message = update.message,
                timestamp = update.timestamp
            )
        )
    }
}

data class MQTTEventUpdate(
    val topic: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
