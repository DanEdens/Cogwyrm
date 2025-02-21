package com.cogwyrm.app.test

import com.cogwyrm.app.MQTTService
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Mock MQTT service for testing
 */
class MockMQTTService : MQTTService() {
    private val isConnected = AtomicBoolean(false)
    private val shouldFailConnection = AtomicBoolean(false)
    private val shouldFailPublish = AtomicBoolean(false)
    private val connectionDelay = 500L
    private val publishDelay = 200L

    var lastTopic: String? = null
        private set
    var lastMessage: String? = null
        private set
    var lastQos: Int? = null
        private set
    var lastRetained: Boolean? = null
        private set

    fun setConnectionShouldFail(shouldFail: Boolean) {
        shouldFailConnection.set(shouldFail)
    }

    fun setPublishShouldFail(shouldFail: Boolean) {
        shouldFailPublish.set(shouldFail)
    }

    override fun isConnected(): Boolean = isConnected.get()

    override suspend fun connect(
        serverUri: String,
        port: Int,
        clientId: String?,
        useSsl: Boolean,
        username: String?,
        password: String?
    ) {
        delay(connectionDelay)
        if (shouldFailConnection.get()) {
            throw Exception("Mock connection failure")
        }
        isConnected.set(true)
    }

    override suspend fun publish(
        topic: String,
        message: String,
        qos: Int,
        retained: Boolean
    ) {
        if (!isConnected.get()) {
            throw Exception("Not connected")
        }
        if (shouldFailPublish.get()) {
            throw Exception("Mock publish failure")
        }
        delay(publishDelay)
        lastTopic = topic
        lastMessage = message
        lastQos = qos
        lastRetained = retained
    }

    override fun disconnect() {
        isConnected.set(false)
        lastTopic = null
        lastMessage = null
        lastQos = null
        lastRetained = null
    }
}
