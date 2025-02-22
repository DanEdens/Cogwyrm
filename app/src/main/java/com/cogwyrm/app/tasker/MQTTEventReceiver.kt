package com.cogwyrm.app.tasker

import android.content.Context
import android.content.Intent
import android.util.Log
import com.cogwyrm.app.mqtt.MQTTClient
import com.cogwyrm.app.utils.ErrorHandler
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.MqttException
import java.util.concurrent.ConcurrentHashMap

class MQTTEventReceiver : TaskerPluginReceiver<MQTTEventInput, MQTTEventOutput>() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeSubscriptions = ConcurrentHashMap<String, MQTTSubscription>()

    data class MQTTSubscription(
        val topic: String,
        val input: MQTTEventInput,
        val context: Context,
        val client: MQTTClient
    )

    override fun onReceive(context: Context, input: MQTTEventInput): MQTTEventOutput {
        val subscription = activeSubscriptions[input.topic]
        if (subscription == null) {
            // New subscription
            scope.launch {
                try {
                    val client = MQTTClient(
                        context = context,
                        brokerUrl = input.brokerUrl,
                        port = input.port.toString(),
                        clientId = input.clientId ?: "cogwyrm_${System.currentTimeMillis()}",
                        useSsl = input.useSsl,
                        onMessageArrived = { topic, message ->
                            handleMessage(topic, message, context)
                        }
                    )

                    activeSubscriptions[input.topic] = MQTTSubscription(
                        topic = input.topic,
                        input = input,
                        context = context,
                        client = client
                    )

                    try {
                        client.connect(
                            input.brokerUrl,
                            input.port,
                            input.clientId,
                            input.useSsl,
                            input.username,
                            input.password
                        )

                        client.subscribe(input.topic, input.qos) { topic, message ->
                            handleMessage(topic, message, context)
                        }
                    } catch (e: MqttException) {
                        ErrorHandler.handleMqttException(context, e)
                        removeSubscription(input.topic)
                    } catch (e: Exception) {
                        val error = CogwyrmError.ConnectionError(
                            context.getString(R.string.error_connection_failed),
                            e
                        )
                        ErrorHandler.handleError(context, error)
                        removeSubscription(input.topic)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting up MQTT subscription", e)
                    val error = CogwyrmError.ConfigurationError(
                        context.getString(R.string.error_configuration, e.message)
                    )
                    ErrorHandler.handleError(context, error)
                }
            }
        }

        return MQTTEventOutput(
            topic = input.topic,
            message = "" // Empty message since this is just setup
        )
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
