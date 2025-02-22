package com.cogwyrm.app.tasker

import android.content.Context
import android.util.Log
import com.cogwyrm.app.utils.CogwyrmError
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
        return try {
            val config = input.regular
            validateConfiguration(config)
            TaskerPluginResultSuccess()
        } catch (e: CogwyrmError.ValidationError) {
            Log.e(TAG, "Validation error: ${e.message}")
            TaskerPluginResultError(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during validation", e)
            TaskerPluginResultError(CogwyrmError.ValidationError("Unexpected error: ${e.message}"))
        }
    }

    private fun validateConfiguration(config: MQTTEventInput) {
        // Basic validation
        if (!config.validateInput()) {
            throw CogwyrmError.ValidationError("Invalid configuration")
        }

        // Additional validation
        validateConnectionParams(config)
        validateSecurityParams(config)
        validateTopicParams(config)
    }

    private fun validateConnectionParams(config: MQTTEventInput) {
        if (config.port == 1883 && config.useSSL) {
            throw CogwyrmError.ValidationError("SSL/TLS typically uses port 8883")
        }
        if (config.port == 8883 && !config.useSSL) {
            throw CogwyrmError.ValidationError("Port 8883 is typically used with SSL/TLS")
        }
        if (config.keepAlive < 10) {
            throw CogwyrmError.ValidationError("Keep alive should be at least 10 seconds")
        }
        if (config.connectionTimeout < 10) {
            throw CogwyrmError.ValidationError("Connection timeout should be at least 10 seconds")
        }
    }

    private fun validateSecurityParams(config: MQTTEventInput) {
        if (config.useSSL) {
            if (config.username.isBlank() && config.password.isNotBlank()) {
                throw CogwyrmError.ValidationError("Username is required when password is provided")
            }
            if (config.username.isNotBlank() && config.password.isBlank()) {
                throw CogwyrmError.ValidationError("Password is required when username is provided")
            }
        }
    }

    private fun validateTopicParams(config: MQTTEventInput) {
        // Check for invalid topic characters
        val invalidChars = setOf('#', '+', '/')
        if (config.topic.any { it in invalidChars } && !isValidWildcardTopic(config.topic)) {
            throw CogwyrmError.ValidationError("Invalid topic format")
        }

        // Check QoS level for wildcards
        if (config.topic.contains('#') && config.qos > 1) {
            throw CogwyrmError.ValidationError("Multi-level wildcards (#) should use QoS 0 or 1")
        }
    }

    private fun isValidWildcardTopic(topic: String): Boolean {
        val parts = topic.split('/')

        // Check multi-level wildcard (#) usage
        val hashIndex = parts.indexOf("#")
        if (hashIndex != -1 && hashIndex != parts.lastIndex) {
            return false
        }

        // Check single-level wildcard (+) usage
        parts.forEachIndexed { index, part ->
            if (part == "+" || part == "#") {
                return@forEachIndexed
            }
            if (part.contains('+') || part.contains('#')) {
                return false
            }
        }

        return true
    }

    override fun getDefaultInput(): TaskerInput<MQTTEventInput> = TaskerInput(
        MQTTEventInput(
            clientId = MQTTEventInput.generateClientId(),
            cleanSession = true,
            qos = 1
        )
    )

    companion object {
        private const val TAG = "MQTTEventHelper"
    }
}
