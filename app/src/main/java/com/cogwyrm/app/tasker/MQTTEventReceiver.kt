package com.cogwyrm.app.tasker

import android.content.Context
import com.cogwyrm.app.mqtt.MQTTClient
import com.cogwyrm.app.mqtt.TopicUtils
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginRunnerConditionEvent
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultCondition
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionSatisfied
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionUnsatisfied
import org.eclipse.paho.client.mqttv3.MqttMessage
import timber.log.Timber

class MQTTEventRunner : TaskerPluginRunnerConditionEvent<MQTTEventInput, MQTTEventOutput, MQTTEventUpdate>() {
    override fun getSatisfiedCondition(context: Context, input: TaskerInput<MQTTEventInput>, update: MQTTEventUpdate?): TaskerPluginResultCondition<MQTTEventOutput> {
        // If no update or topic doesn't match pattern, condition not satisfied
        if (update == null || !TopicUtils.topicMatchesPattern(input.regular.topic, update.topic)) {
            return TaskerPluginResultConditionUnsatisfied()
        }

        val eventOutput = MQTTEventOutput(
            topic = update.topic,
            message = update.message,
            qos = update.qos,
            retained = update.retained,
            timestamp = update.timestamp
        )
        return TaskerPluginResultConditionSatisfied(context, eventOutput)
    }
}

class MQTTEventHelper(config: TaskerPluginConfig<MQTTEventInput>) : TaskerPluginConfigHelper<MQTTEventInput, MQTTEventOutput, MQTTEventUpdate>(config) {
    override val runnerClass = MQTTEventRunner::class.java
    override val inputClass = MQTTEventInput::class.java
    override val outputClass = MQTTEventOutput::class.java
}

// This class represents an update from MQTT that will trigger the event
data class MQTTEventUpdate(
    val topic: String,
    val message: String,
    val qos: Int,
    val retained: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class MQTTEventReceiver : MQTTEventHelper.ReceiverCondition() {
    companion object {
        private val activeSubscriptions = mutableMapOf<String, MQTTSubscription>()

        data class MQTTSubscription(
            val client: MQTTClient,
            val pattern: String,
            var activeConditions: Int = 1
        )

        fun handleMqttMessage(topic: String, message: MqttMessage) {
            Timber.d("Received MQTT message on topic: $topic")

            // Create update object
            val update = MQTTEventUpdate(
                topic = topic,
                message = String(message.payload),
                qos = message.qos,
                retained = message.isRetained
            )

            // Notify Tasker about the event
            TaskerPluginRunnerConditionEvent.requestQuery(MQTTEventRunner::class.java, update)
        }
    }

    override fun addCondition(context: Context, input: TaskerInput<MQTTEventInput>, update: MQTTEventUpdate?) {
        val mqttInput = input.regular

        // Validate topic pattern
        if (!TopicUtils.validateTopic(mqttInput.topic)) {
            Timber.e("Invalid topic pattern: ${mqttInput.topic}")
            return
        }

        val subscriptionKey = "${mqttInput.brokerUrl}:${mqttInput.port}:${mqttInput.topic}"

        synchronized(activeSubscriptions) {
            val existingSubscription = activeSubscriptions[subscriptionKey]
            if (existingSubscription != null) {
                // Increment reference count for existing subscription
                existingSubscription.activeConditions++
                Timber.d("Incremented condition count for subscription: $subscriptionKey")
                return
            }

            // Create new subscription
            try {
                val client = MQTTClient(
                    context = context,
                    brokerUrl = mqttInput.brokerUrl,
                    port = mqttInput.port,
                    clientId = mqttInput.clientId ?: "cogwyrm_event_${System.currentTimeMillis()}",
                    useSsl = mqttInput.useSsl,
                    username = mqttInput.username,
                    password = mqttInput.password,
                    onConnectionLost = { cause ->
                        Timber.e(cause, "MQTT connection lost for event subscription")
                        // Implement reconnection logic
                        handleConnectionLost(context, subscriptionKey, mqttInput)
                    },
                    onMessageArrived = { topic, message ->
                        handleMqttMessage(topic, message)
                    },
                    onDeliveryComplete = { }
                )

                client.connect()
                client.subscribe(mqttInput.topic, mqttInput.qos)

                activeSubscriptions[subscriptionKey] = MQTTSubscription(
                    client = client,
                    pattern = mqttInput.topic
                )

                Timber.d("Created new subscription for: $subscriptionKey")
            } catch (e: Exception) {
                Timber.e(e, "Failed to create MQTT subscription")
            }
        }
    }

    override fun removeCondition(context: Context, input: TaskerInput<MQTTEventInput>, update: MQTTEventUpdate?) {
        val mqttInput = input.regular
        val subscriptionKey = "${mqttInput.brokerUrl}:${mqttInput.port}:${mqttInput.topic}"

        synchronized(activeSubscriptions) {
            val subscription = activeSubscriptions[subscriptionKey] ?: return

            subscription.activeConditions--
            if (subscription.activeConditions <= 0) {
                // Last condition removed, clean up subscription
                try {
                    subscription.client.disconnect()
                    activeSubscriptions.remove(subscriptionKey)
                    Timber.d("Removed subscription: $subscriptionKey")
                } catch (e: Exception) {
                    Timber.e(e, "Error cleaning up MQTT subscription")
                }
            }
        }
    }

    private fun handleConnectionLost(context: Context, subscriptionKey: String, input: MQTTEventInput) {
        synchronized(activeSubscriptions) {
            val subscription = activeSubscriptions[subscriptionKey] ?: return

            // Simple exponential backoff retry
            var retryDelay = 1000L
            var retryCount = 0
            val maxRetries = 5

            while (retryCount < maxRetries) {
                try {
                    Thread.sleep(retryDelay)
                    subscription.client.connect()
                    subscription.client.subscribe(input.topic, input.qos)
                    Timber.d("Reconnected subscription: $subscriptionKey")
                    return
                } catch (e: Exception) {
                    Timber.e(e, "Retry $retryCount failed")
                    retryCount++
                    retryDelay *= 2
                }
            }

            // If all retries failed, remove the subscription
            activeSubscriptions.remove(subscriptionKey)
            Timber.e("Failed to reconnect after $maxRetries attempts")
        }
    }
}
