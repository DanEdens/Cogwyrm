package com.cogwyrm.app.tasker

import android.content.Context
import com.cogwyrm.app.mqtt.TopicUtils
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginRunnerConditionEvent
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultCondition
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionSatisfied
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionUnsatisfied

class MQTTEventRunner : TaskerPluginRunnerConditionEvent<MQTTEventInput, MQTTEventOutput, MQTTEventOutput>() {
    override fun getSatisfiedCondition(
        context: Context,
        input: TaskerInput<MQTTEventInput>,
        update: MQTTEventOutput?
    ): TaskerPluginResultCondition<MQTTEventOutput> {
        // If no update or topic doesn't match pattern, condition not satisfied
        if (update == null || !TopicUtils.topicMatchesPattern(input.regular.topic, update.topic)) {
            return TaskerPluginResultConditionUnsatisfied()
        }

        return TaskerPluginResultConditionSatisfied(context, update)
    }
}
