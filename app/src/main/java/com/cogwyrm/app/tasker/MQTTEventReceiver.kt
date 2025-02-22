package com.cogwyrm.app.tasker

import android.content.Context
import android.util.Log
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
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class MQTTEventHelper(config: TaskerPluginConfig<MQTTEventInput>) : TaskerPluginConfigHelper<MQTTEventInput, MQTTEventOutput, MQTTEventRunner>(config) {
    override val runnerClass = MQTTEventRunner::class.java
    override val inputClass = MQTTEventInput::class.java
    override val outputClass = MQTTEventOutput::class.java
}

class MQTTEventReceiver : TaskerPluginRunnerConditionEvent<MQTTEventInput, MQTTEventOutput, MQTTEventOutput>() {
    companion object {
        private const val TAG = "MQTTEventReceiver"
        private val activeSubscriptions = ConcurrentHashMap<String, MQTTSubscription>()

        data class MQTTSubscription(
            val client: MQTTClient,
            val pattern: String,
            val refCount: AtomicInteger = AtomicInteger(1),
            val topics: MutableSet<String> = mutableSetOf()
        )

        fun handleMqttMessage(topic: String, message: String, qos: Int, retained: Boolean) {
            Log.d(TAG, "Received MQTT message on topic: $topic")

            // Create update object
            val update = MQTTEventOutput(
                topic = topic,
                message = message,
                qos = qos,
                retained = retained,
                timestamp = System.currentTimeMillis()
            )

            // Notify Tasker about the event
            requestQuery(MQTTEventReceiver::class.java, update)
        }
    }

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

    override fun addCondition(context: Context, input: TaskerInput<MQTTEventInput>) {
        val mqttInput = input.regular

        // Validate topic pattern
        if (!TopicUtils.validateTopic(mqttInput.topic)) {
            Log.e(TAG, "Invalid topic pattern: ${mqttInput.topic}")
            return
        }

        val subscriptionKey = "${mqttInput.brokerUrl}:${mqttInput.port}:${mqttInput.clientId}"

        // Try to reuse existing subscription
        activeSubscriptions[subscriptionKey]?.let { subscription ->
            if (subscription.refCount.incrementAndGet() > 1) {
                // Add new topic to existing subscription if not already subscribed
                if (subscription.topics.add(mqttInput.topic)) {
                    subscription.client.subscribe(mqttInput.topic, mqttInput.qos, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d(TAG, "Successfully added topic ${mqttInput.topic} to existing subscription")
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e(TAG, "Failed to add topic ${mqttInput.topic} to existing subscription", exception)
                            subscription.topics.remove(mqttInput.topic)
                            subscription.refCount.decrementAndGet()
                        }
                    })
                }
                Log.d(TAG, "Reusing existing subscription for $subscriptionKey (ref count: ${subscription.refCount.get()})")
                return
            }
        }

        // Create new subscription
        try {
            val client = MQTTClient(
                context = context,
                brokerUrl = mqttInput.brokerUrl,
                port = mqttInput.port,
                clientId = mqttInput.clientId ?: "cogwyrm_event_${System.currentTimeMillis()}",
                useSsl = mqttInput.useSsl,
                onConnectionLost = { cause ->
                    Log.e(TAG, "MQTT connection lost for event subscription", cause)
                },
                onMessageArrived = { topic, message ->
                    handleMqttMessage(topic, message, mqttInput.qos, false)
                },
                onDeliveryComplete = { }
            )

            val subscription = MQTTSubscription(
                client = client,
                pattern = mqttInput.topic
            ).also { sub ->
                sub.topics.add(mqttInput.topic)
            }

            client.connect(object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    client.subscribe(mqttInput.topic, mqttInput.qos, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d(TAG, "Successfully subscribed to ${mqttInput.topic}")
                            activeSubscriptions[subscriptionKey] = subscription
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e(TAG, "Failed to subscribe to ${mqttInput.topic}", exception)
                            client.disconnect()
                        }
                    })
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Failed to connect for subscription", exception)
                }
            })

            Log.d(TAG, "Created new subscription for: $subscriptionKey")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create MQTT subscription", e)
        }
    }

    override fun removeCondition(context: Context, input: TaskerInput<MQTTEventInput>) {
        val mqttInput = input.regular
        val subscriptionKey = "${mqttInput.brokerUrl}:${mqttInput.port}:${mqttInput.clientId}"

        activeSubscriptions[subscriptionKey]?.let { subscription ->
            if (subscription.refCount.decrementAndGet() <= 0) {
                // Last reference removed, clean up subscription
                try {
                    subscription.client.disconnect()
                    activeSubscriptions.remove(subscriptionKey)
                    Log.d(TAG, "Removed subscription: $subscriptionKey")
                } catch (e: Exception) {
                    Log.e(TAG, "Error cleaning up MQTT subscription", e)
                }
            } else {
                // Remove topic from subscription if no other conditions use it
                subscription.topics.remove(mqttInput.topic)
                Log.d(TAG, "Decremented subscription ref count: ${subscription.refCount.get()}")
            }
        }
    }
}
