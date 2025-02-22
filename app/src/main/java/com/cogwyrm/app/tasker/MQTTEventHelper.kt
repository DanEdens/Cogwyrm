package com.cogwyrm.app.tasker

import com.cogwyrm.app.utils.ErrorHandler
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
        return try {
            with(input.data) {
                // Use ErrorHandler validation methods
                ErrorHandler.validateBrokerUrl(context, brokerUrl)
                ErrorHandler.validatePort(context, port.toString())
                ErrorHandler.validateTopic(context, topic)

                // Additional MQTT-specific validation
                if (useSsl && port != 8883) {
                    throw CogwyrmError.ValidationError(
                        context.getString(R.string.error_configuration, "SSL requires port 8883")
                    )
                }
            }
            TaskerPluginResultSucess()
        } catch (e: CogwyrmError) {
            ErrorHandler.createTaskerError(e)
        } catch (e: Exception) {
            val error = CogwyrmError.UnknownError(
                context.getString(R.string.error_unknown),
                e
            )
            ErrorHandler.createTaskerError(error)
        }
    }

    override fun getDefaultInput(): TaskerInput<MQTTEventInput>? {
        return TaskerInput(
            MQTTEventInput(
                brokerUrl = "",
                port = 1883,
                clientId = null,
                topic = "",
                qos = 0,
                useSsl = false,
                username = null,
                password = null
            )
        )
    }
}
