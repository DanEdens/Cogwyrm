package com.cogwyrm.app.tasker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.cogwyrm.app.mqtt.MQTTClient
import com.cogwyrm.app.utils.ErrorHandler
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.MqttException
import java.util.concurrent.ConcurrentHashMap
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import com.cogwyrm.app.mqtt.MQTTService
import com.cogwyrm.app.R

class MQTTEventReceiver : TaskerPluginRunnerAction<MQTTEventInput, MQTTEventOutput>() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeSubscriptions = ConcurrentHashMap<String, MQTTSubscription>()

    data class MQTTSubscription(
        val topic: String,
        val input: MQTTEventInput,
        val context: Context,
        val client: MQTTClient
    )

    override fun run(context: Context, input: TaskerInput<MQTTEventInput>): TaskerPluginResult<MQTTEventOutput> {
        return try {
            val config = input.regular
            if (!config.validateInput()) {
                return TaskerPluginResultError(context.getString(R.string.error_invalid_input))
            }

            val client = MQTTClient(
                brokerUrl = config.brokerUrl,
                port = config.port,
                useSSL = config.useSSL,
                username = config.username,
                password = config.password,
                clientId = config.clientId
            )

            val intent = Intent(context, MQTTService::class.java).apply {
                putExtra("brokerUrl", config.brokerUrl)
                putExtra("port", config.port)
                putExtra("useSSL", config.useSSL)
                putExtra("username", config.username)
                putExtra("password", config.password)
                putExtra("clientId", config.clientId)
                putExtra("topic", config.topic)
                putExtra("qos", config.qos)
            }

            context.startService(intent)
            TaskerPluginResultSucess()
        } catch (e: Exception) {
            TaskerPluginResultError(e.message ?: context.getString(R.string.error_unknown))
        }
    }

    private fun handleMessage(topic: String, message: String, context: Context) {
        val subscription = activeSubscriptions[topic] ?: return
        val output = MQTTEventOutput(topic = topic, message = message)

        // Broadcast the output
        context.sendBroadcast(Intent().apply {
            action = "com.cogwyrm.app.tasker.ACTION_MQTT_MESSAGE"
            putExtra("output", output.toBundle())
        })
    }

    fun removeSubscription(topic: String) {
        val subscription = activeSubscriptions.remove(topic)
        if (subscription != null) {
            scope.launch {
                try {
                    subscription.client.unsubscribe(topic)
                    subscription.client.disconnect()
                } catch (e: MqttException) {
                    ErrorHandler.handleMqttException(
                        subscription.context,
                        e,
                        showDialog = false
                    )
                } catch (e: Exception) {
                    val error = CogwyrmError.ConnectionError(
                        subscription.context.getString(R.string.error_connection_failed),
                        e
                    )
                    ErrorHandler.handleError(subscription.context, error, showDialog = false)
                }
            }
        }
    }

    companion object {
        private const val TAG = "MQTTEventReceiver"
    }
}
