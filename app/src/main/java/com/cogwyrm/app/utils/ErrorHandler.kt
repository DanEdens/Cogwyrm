package com.cogwyrm.app.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.cogwyrm.app.R
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import org.eclipse.paho.client.mqttv3.MqttException

sealed class CogwyrmError(
    val message: String,
    val cause: Throwable? = null,
    val errorCode: Int = 0
) {
    class ValidationError(message: String) : CogwyrmError(message)
    class ConnectionError(message: String, cause: Throwable? = null) : CogwyrmError(message, cause)
    class ConfigurationError(message: String) : CogwyrmError(message)
    class NetworkError(message: String, cause: Throwable? = null) : CogwyrmError(message, cause)
    class UnknownError(message: String, cause: Throwable? = null) : CogwyrmError(message, cause)
}

object ErrorHandler {
    private const val TAG = "ErrorHandler"

    fun handleError(context: Context, error: CogwyrmError, showDialog: Boolean = true) {
        // Log the error
        when (error) {
            is CogwyrmError.ValidationError -> Log.w(TAG, "Validation error: ${error.message}")
            is CogwyrmError.ConnectionError -> Log.e(TAG, "Connection error: ${error.message}", error.cause)
            is CogwyrmError.ConfigurationError -> Log.e(TAG, "Configuration error: ${error.message}")
            is CogwyrmError.NetworkError -> Log.e(TAG, "Network error: ${error.message}", error.cause)
            is CogwyrmError.UnknownError -> Log.e(TAG, "Unknown error: ${error.message}", error.cause)
        }

        // Show error UI if requested
        if (showDialog) {
            showErrorDialog(context, error)
        } else {
            showErrorToast(context, error)
        }
    }

    fun handleMqttException(context: Context, exception: MqttException, showDialog: Boolean = true): CogwyrmError {
        val error = when (exception.reasonCode) {
            MqttException.REASON_CODE_CLIENT_EXCEPTION ->
                CogwyrmError.ConnectionError(context.getString(R.string.error_connection_failed), exception)
            MqttException.REASON_CODE_CONNECT_IN_PROGRESS ->
                CogwyrmError.ConnectionError(context.getString(R.string.error_connection_failed), exception)
            MqttException.REASON_CODE_CONNECTION_LOST ->
                CogwyrmError.ConnectionError(context.getString(R.string.error_connection_lost), exception)
            MqttException.REASON_CODE_SERVER_CONNECT_ERROR ->
                CogwyrmError.ConnectionError(context.getString(R.string.error_connection_failed), exception)
            MqttException.REASON_CODE_FAILED_AUTHENTICATION ->
                CogwyrmError.ConnectionError(context.getString(R.string.error_auth_failed), exception)
            MqttException.REASON_CODE_SSL_CONFIG_ERROR ->
                CogwyrmError.ConnectionError(context.getString(R.string.error_ssl_failed), exception)
            else -> CogwyrmError.UnknownError(context.getString(R.string.error_unknown), exception)
        }

        handleError(context, error, showDialog)
        return error
    }

    fun createTaskerError(error: CogwyrmError): TaskerPluginResultError {
        return TaskerPluginResultError(error.message)
    }

    private fun showErrorDialog(context: Context, error: CogwyrmError) {
        AlertDialog.Builder(context)
            .setTitle(R.string.dialog_error_title)
            .setMessage(error.message)
            .setPositiveButton(R.string.dialog_ok, null)
            .setNegativeButton(R.string.dialog_retry) { _, _ ->
                // Implement retry logic in the calling component
            }
            .show()
    }

    private fun showErrorToast(context: Context, error: CogwyrmError) {
        Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
    }

    // Validation helper methods
    fun validateBrokerUrl(context: Context, url: String?): String {
        if (url.isNullOrBlank()) {
            throw CogwyrmError.ValidationError(context.getString(R.string.error_broker_url_required))
        }
        // Add URL format validation if needed
        return url
    }

    fun validatePort(context: Context, port: String?): Int {
        if (port.isNullOrBlank()) {
            throw CogwyrmError.ValidationError(context.getString(R.string.error_port_required))
        }
        return try {
            val portNum = port.toInt()
            if (portNum !in 1..65535) {
                throw CogwyrmError.ValidationError(context.getString(R.string.error_port_invalid))
            }
            portNum
        } catch (e: NumberFormatException) {
            throw CogwyrmError.ValidationError(context.getString(R.string.error_port_invalid))
        }
    }

    fun validateTopic(context: Context, topic: String?): String {
        if (topic.isNullOrBlank()) {
            throw CogwyrmError.ValidationError(context.getString(R.string.error_topic_required))
        }
        // Add MQTT topic format validation if needed
        return topic
    }

    fun validateMessage(context: Context, message: String?): String {
        if (message.isNullOrBlank()) {
            throw CogwyrmError.ValidationError(context.getString(R.string.error_message_required))
        }
        return message
    }
}
