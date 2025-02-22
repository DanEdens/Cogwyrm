package com.cogwyrm.app.tasker

import android.content.Context
import android.util.Log
import com.cogwyrm.app.mqtt.MQTTClient
import com.cogwyrm.app.utils.CogwyrmError
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginRunnerConditionEvent
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultCondition
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionSatisfied
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionUnsatisfied
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class MQTTEventRunner : TaskerPluginRunnerConditionEvent<MQTTEventInput, MQTTEventOutput>() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeClients = ConcurrentHashMap<String, MQTTClient>()
    private val activeSubscriptions = ConcurrentHashMap<String, MutableSet<String>>()

    override fun getSatisfiedCondition(
        context: Context,
        input: MQTTEventInput,
        update: MQTTEventOutput?
    ): TaskerPluginResultCondition<MQTTEventOutput> {
        if (update == null) {
            return TaskerPluginResultConditionUnsatisfied()
        }

        // Check if the update matches our subscription
        if (!isTopicMatch(input.topic, update.topic)) {
            return TaskerPluginResultConditionUnsatisfied()
        }

        return TaskerPluginResultConditionSatisfied(context, update)
    }

    override fun requestQuery(context: Context, input: TaskerInput<MQTTEventInput>) {
        val config = input.regular
        val clientKey = "${config.brokerUrl}:${config.port}"

        scope.launch {
            try {
                // Get or create client
                val client = activeClients.getOrPut(clientKey) {
                    MQTTClient(
                        context = context,
                        brokerUrl = config.brokerUrl,
                        port = config.port.toString(),
                        clientId = config.clientId,
                        useSsl = config.useSSL,
                        onConnectionLost = { cause -> handleConnectionLost(clientKey, cause) },
                        onMessageArrived = { topic, message -> handleMessageArrived(topic, message) }
                    )
                }

                // Connect if not already connected
                if (!client.isConnected()) {
                    withContext(Dispatchers.IO) {
                        client.connect()
                    }
                }

                // Add subscription
                val subscriptions = activeSubscriptions.getOrPut(clientKey) { mutableSetOf() }
                if (subscriptions.add(config.topic)) {
                    withContext(Dispatchers.IO) {
                        client.subscribe(config.topic, config.qos) { topic, message ->
                            handleMessageArrived(topic, message)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in requestQuery", e)
                handleError(context, e)
            }
        }
    }

    override fun destroy(context: Context, input: TaskerInput<MQTTEventInput>) {
        val config = input.regular
        val clientKey = "${config.brokerUrl}:${config.port}"

        scope.launch {
            try {
                // Remove subscription
                activeSubscriptions[clientKey]?.remove(config.topic)

                // If no more subscriptions for this client, disconnect and remove
                if (activeSubscriptions[clientKey]?.isEmpty() == true) {
                    activeClients[clientKey]?.let { client ->
                        withContext(Dispatchers.IO) {
                            client.disconnect()
                        }
                        activeClients.remove(clientKey)
                        activeSubscriptions.remove(clientKey)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in destroy", e)
                handleError(context, e)
            }
        }
    }

    private fun handleConnectionLost(clientKey: String, cause: Throwable?) {
        Log.w(TAG, "Connection lost for $clientKey: ${cause?.message}")
        // The MQTTClient will handle reconnection automatically
    }

    private fun handleMessageArrived(topic: String, message: String) {
        val output = MQTTEventOutput(
            message = message,
            topic = topic,
            timestamp = System.currentTimeMillis()
        )
        // Notify Tasker of the event
        taskerPluginRunnerConditionEvent?.onEventReceived(output)
    }

    private fun handleError(context: Context, error: Throwable) {
        when (error) {
            is CogwyrmError -> Log.e(TAG, "Cogwyrm error: ${error.message}")
            is MqttException -> Log.e(TAG, "MQTT error: ${error.message}")
            else -> Log.e(TAG, "Unknown error: ${error.message}")
        }
    }

    private fun isTopicMatch(subscription: String, topic: String): Boolean {
        val subParts = subscription.split('/')
        val topicParts = topic.split('/')

        if (subParts.size > topicParts.size && subParts.last() != "#") {
            return false
        }

        for (i in subParts.indices) {
            if (i >= topicParts.size) {
                return false
            }
            when (subParts[i]) {
                "#" -> return true
                "+" -> continue
                else -> if (subParts[i] != topicParts[i]) return false
            }
        }

        return subParts.size == topicParts.size || (subParts.last() == "#" && subParts.size <= topicParts.size)
    }

    companion object {
        private const val TAG = "MQTTEventRunner"
    }
}

data class MQTTEventUpdate(
    val topic: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
