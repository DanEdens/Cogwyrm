package com.cogwyrm.app.tasker

import android.content.Context
import com.cogwyrm.app.mqtt.MQTTClient
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class MQTTEventReceiverTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var mqttClient: MQTTClient

    @Mock
    private lateinit var taskerInput: TaskerInput<MQTTEventInput>

    private lateinit var eventReceiver: MQTTEventReceiver

    @Before
    fun setup() {
        eventReceiver = MQTTEventReceiver()

        // Mock Tasker input
        val mqttEventInput = MQTTEventInput(
            brokerUrl = "test.mosquitto.org",
            port = "1883",
            clientId = "test-client",
            topic = "test/topic",
            useSsl = false
        )
        whenever(taskerInput.regular).thenReturn(mqttEventInput)

        // Mock MQTTClient creation
        val clientConstructor = MQTTClient::class.java.getDeclaredConstructor(
            Context::class.java,
            String::class.java,
            String::class.java,
            String::class.java,
            Boolean::class.java,
            Function1::class.java,
            Function2::class.java,
            Function1::class.java
        )
        clientConstructor.isAccessible = true
    }

    @Test
    fun `test add condition creates new subscription`() {
        // Act
        eventReceiver.addCondition(context, taskerInput, null)

        // Assert - verify client connection attempt
        verify(mqttClient).connect(any())
    }

    @Test
    fun `test add condition reuses existing subscription`() {
        // Arrange - add first condition
        eventReceiver.addCondition(context, taskerInput, null)

        // Act - add second condition with same broker details
        eventReceiver.addCondition(context, taskerInput, null)

        // Assert - verify client was created only once
        verify(mqttClient, times(1)).connect(any())
    }

    @Test
    fun `test remove condition decrements reference count`() {
        // Arrange - add condition
        eventReceiver.addCondition(context, taskerInput, null)

        // Act - remove condition
        eventReceiver.removeCondition(context, taskerInput, null)

        // Assert - verify client disconnect not called (ref count > 0)
        verify(mqttClient, never()).disconnect()
    }

    @Test
    fun `test remove last condition cleans up subscription`() {
        // Arrange - add and remove same condition
        eventReceiver.addCondition(context, taskerInput, null)
        eventReceiver.removeCondition(context, taskerInput, null)
        eventReceiver.removeCondition(context, taskerInput, null) // Second removal should trigger cleanup

        // Assert - verify client disconnect called
        verify(mqttClient).disconnect()
    }

    @Test
    fun `test event runner matches topic pattern`() {
        // Arrange
        val runner = MQTTEventRunner()
        val update = MQTTEventUpdate(
            topic = "test/topic",
            message = "test message",
            qos = 1,
            retained = false
        )

        // Act
        val result = runner.getSatisfiedCondition(context, taskerInput, update)

        // Assert
        assert(result is TaskerPluginResultConditionSatisfied)
    }

    @Test
    fun `test event runner handles wildcard pattern`() {
        // Arrange
        val runner = MQTTEventRunner()
        whenever(taskerInput.regular.topic).thenReturn("test/+/temp")
        val update = MQTTEventUpdate(
            topic = "test/room1/temp",
            message = "25.5",
            qos = 1,
            retained = false
        )

        // Act
        val result = runner.getSatisfiedCondition(context, taskerInput, update)

        // Assert
        assert(result is TaskerPluginResultConditionSatisfied)
    }

    @Test
    fun `test subscription handles connection failure`() {
        // Arrange
        val connectListener = argumentCaptor<IMqttActionListener>()

        // Act
        eventReceiver.addCondition(context, taskerInput, null)
        verify(mqttClient).connect(connectListener.capture())

        // Simulate connection failure
        connectListener.firstValue.onFailure(mock(IMqttToken::class.java), RuntimeException("Connection failed"))

        // Assert - verify cleanup
        verify(mqttClient).disconnect()
    }

    @Test
    fun `test subscription handles multiple topics`() {
        // Arrange
        val input1 = MQTTEventInput(
            brokerUrl = "test.mosquitto.org",
            port = "1883",
            clientId = "test-client",
            topic = "test/topic1"
        )
        val input2 = MQTTEventInput(
            brokerUrl = "test.mosquitto.org",
            port = "1883",
            clientId = "test-client",
            topic = "test/topic2"
        )
        whenever(taskerInput.regular).thenReturn(input1, input2)

        // Act - add two conditions with different topics
        eventReceiver.addCondition(context, taskerInput, null)
        eventReceiver.addCondition(context, taskerInput, null)

        // Assert - verify subscribe called for both topics
        verify(mqttClient).subscribe(eq("test/topic1"), any(), any(), any())
        verify(mqttClient).subscribe(eq("test/topic2"), any(), any(), any())
    }
}
