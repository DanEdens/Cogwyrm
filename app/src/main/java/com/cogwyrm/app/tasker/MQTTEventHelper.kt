package com.cogwyrm.app.tasker

import android.content.Context
import android.util.Log
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.SimpleResult
import com.joaomgcd.taskerpluginlibrary.SimpleResultError
import com.joaomgcd.taskerpluginlibrary.SimpleResultSuccess

class MQTTEventHelper(context: Context) : TaskerPluginConfigHelper<MQTTEventInput, MQTTEventOutput, MQTTEventRunner>(
    config = TaskerPluginConfig(
        context = context,
        inputClass = MQTTEventInput::class.java,
        outputClass = MQTTEventOutput::class.java,
        runnerClass = MQTTEventRunner::class.java
    )
) {
    override fun isInputValid(input: TaskerInput<MQTTEventInput>): SimpleResult {
        return try {
            validateConfiguration(input.regular)
            SimpleResultSuccess
        } catch (e: Exception) {
            SimpleResultError(e.message ?: "Invalid configuration")
        }
    }

    private fun validateConfiguration(input: MQTTEventInput) {
        validateConnectionParams(input)
        validateSecurityParams(input)
        validateTopicParams(input)
    }

    private fun validateConnectionParams(input: MQTTEventInput) {
        if (input.brokerUrl.isBlank()) {
            throw IllegalArgumentException("Broker URL cannot be empty")
        }

        if (input.port !in 1..65535) {
            throw IllegalArgumentException("Port must be between 1 and 65535")
        }

        if (input.useSSL && input.port == 1883) {
            throw IllegalArgumentException("Port 1883 is typically used for non-SSL connections. Consider using 8883 for SSL/TLS")
        }

        if (input.keepAlive < 0) {
            throw IllegalArgumentException("Keep alive interval must be non-negative")
        }

        if (input.connectionTimeout < 0) {
            throw IllegalArgumentException("Connection timeout must be non-negative")
        }
    }

    private fun validateSecurityParams(input: MQTTEventInput) {
        if (input.useSSL) {
            if (!input.username.isNullOrBlank() && input.password.isNullOrBlank()) {
                throw IllegalArgumentException("Password is required when username is provided")
            }
            if (input.username.isNullOrBlank() && !input.password.isNullOrBlank()) {
                throw IllegalArgumentException("Username is required when password is provided")
            }
        }
    }

    private fun validateTopicParams(input: MQTTEventInput) {
        if (input.topic.isBlank()) {
            throw IllegalArgumentException("Topic cannot be empty")
        }

        if (!isValidWildcardTopic(input.topic)) {
            throw IllegalArgumentException("Invalid topic format")
        }

        if (input.qos !in 0..2) {
            throw IllegalArgumentException("QoS must be between 0 and 2")
        }

        // Additional validation for wildcard topics
        if (input.topic.contains('#') || input.topic.contains('+')) {
            if (input.qos > 1) {
                throw IllegalArgumentException("QoS 2 is not recommended for wildcard topics")
            }
        }
    }

    private fun isValidWildcardTopic(topic: String): Boolean {
        // Basic MQTT topic validation
        if (topic.contains('#') && !topic.endsWith('#')) {
            return false
        }

        val segments = topic.split('/')
        for (segment in segments) {
            if (segment.contains('#') && segment != "#") {
                return false
            }
            if (segment.contains('+') && segment != "+") {
                return false
            }
        }

        return true
    }

    override fun addDefaultValues(input: TaskerInput<MQTTEventInput>) {
        super.addDefaultValues(input)
        // Add any default values if needed
    }

    companion object {
        private const val TAG = "MQTTEventHelper"
    }
}
