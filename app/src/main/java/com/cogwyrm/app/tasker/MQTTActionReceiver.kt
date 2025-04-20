package com.cogwyrm.app.tasker

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.extensions.BundleExtensions
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSuccess

class MQTTActionReceiver : com.joaomgcd.taskerpluginlibrary.action.TaskerPluginBroadcastReceiver() {
    override val context: Context
        get() = this.applicationContext

    override fun onFireReceived(bundle: Bundle) {
        Log.d(TAG, "Received fire action for MQTT plugin")

        val input = try {
            BundleExtensions.getBundleOrNull(bundle, "com.twofortyfouram.locale.intent.extra.BUNDLE")?.let { inputBundle ->
                MQTTEventInput(
                    brokerUrl = inputBundle.getString("brokerUrl", ""),
                    port = inputBundle.getInt("port", 1883),
                    clientId = inputBundle.getString("clientId", "cogwyrm_${System.currentTimeMillis()}"),
                    topic = inputBundle.getString("topic", ""),
                    payload = inputBundle.getString("payload", ""),
                    qos = inputBundle.getInt("qos", 0),
                    useSsl = inputBundle.getBoolean("useSsl", false),
                    username = inputBundle.getString("username"),
                    password = inputBundle.getString("password")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing input bundle", e)
            null
        }

        if (input == null) {
            Log.e(TAG, "Null input, cannot continue")
            return
        }

        val taskerInput = TaskerInput(input)
        val runner = MQTTTaskerAction()

        try {
            val result = runner.run(context, taskerInput)
            when (result) {
                is TaskerPluginResultSuccess -> {
                    Log.d(TAG, "Successfully executed MQTT action")
                }
                is TaskerPluginResultError -> {
                    Log.e(TAG, "Error executing MQTT action: ${result.message}")
                }
                else -> {
                    Log.w(TAG, "Unknown result type: $result")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while running action", e)
        }
    }

    companion object {
        private const val TAG = "MQTTActionReceiver"
    }
}
