package com.cogwyrm.app.tasker

import android.content.Context
import android.os.Parcelable
import android.util.Log
import com.cogwyrm.app.mqtt.MQTTClient
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginRunnerConditionEvent
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputObject
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultCondition
import kotlinx.coroutines.*
import kotlinx.parcelize.Parcelize

class MQTTEventRunner : TaskerPluginRunnerConditionEvent<MQTTEventInput, MQTTEventOutput, MQTTEventUpdate>() {
    private var client: MQTTClient? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun getSatisfiedCondition(
        context: Context,
        input: TaskerInput<MQTTEventInput>,
        update: MQTTEventUpdate?
    ): TaskerPluginResultCondition<MQTTEventOutput> {
        return if (update != null && isTopicMatch(input.regular.topic, update.topic)) {
            TaskerPluginResultCondition.satisfied(context, MQTTEventOutput(update.topic, update.payload))
        } else {
            TaskerPluginResultCondition.unsatisfied(context)
        }
    }

    override fun requestQuery(context: Context, input: TaskerInput<MQTTEventInput>) {
        scope.launch {
            try {
                client = MQTTClient(
                    context = context,
                    brokerUrl = input.regular.brokerUrl,
                    port = input.regular.port,
                    clientId = input.regular.clientId,
                    useSsl = input.regular.useSSL,
                    username = input.regular.username,
                    password = input.regular.password
                )

                client?.connect()
                client?.subscribe(input.regular.topic, input.regular.qos) { topic, payload ->
                    val update = MQTTEventUpdate(topic, payload)
                    eventReceived(context, update)
                }
            } catch (e: Exception) {
                Log.e("MQTTEventRunner", "Error in MQTT connection", e)
            }
        }
    }

    override fun destroy(context: Context, input: TaskerInput<MQTTEventInput>) {
        scope.launch {
            try {
                client?.unsubscribe(input.regular.topic)
                client?.disconnect()
            } catch (e: Exception) {
                Log.e("MQTTEventRunner", "Error destroying MQTT client", e)
            } finally {
                client = null
                scope.cancel()
            }
        }
    }

    private fun isTopicMatch(subscribedTopic: String, publishedTopic: String): Boolean {
        val subscribedParts = subscribedTopic.split("/")
        val publishedParts = publishedTopic.split("/")

        if (subscribedParts.size != publishedParts.size) return false

        return subscribedParts.zip(publishedParts).all { (sub, pub) ->
            sub == "#" || sub == "+" || sub == pub
        }
    }
}

@TaskerInputObject(key = "mqtt_event_update")
@Parcelize
data class MQTTEventUpdate(
    val topic: String,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable
