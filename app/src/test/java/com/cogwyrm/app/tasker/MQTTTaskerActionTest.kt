package com.cogwyrm.app.tasker

import android.content.Context
import com.cogwyrm.app.mqtt.MQTTClient
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSuccess
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MQTTTaskerActionTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var mqttClient: MQTTClient

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var mqttTaskerAction: MQTTTaskerAction

    @Before
    fun setup() {
        mqttTaskerAction = MQTTTaskerAction()

        // Mock MQTTClient creation
        Mockito.mockStatic(MQTTClient::class.java).use { mockedStatic ->
            mockedStatic.`when`<MQTTClient> {
                MQTTClient(
                    context = eq(context),
                    brokerUrl = anyString(),
                    port = anyString(),
                    clientId = anyString(),
                    useSsl = anyBoolean(),
                    username = anyString(),
                    password = anyString()
                )
            }.thenReturn(mqttClient)
        }
    }

    @Test
    fun `test valid input returns success`() = testScope.runTest {
        // Arrange
        val validInput = MQTTEventInput(
            brokerUrl = "test.mosquitto.org",
            port = 1883,
            clientId = "test-client",
            topic = "test/topic",
            payload = "Hello, MQTT!",
            qos = 0,
            useSsl = false
        )

        // Act
        val result = mqttTaskerAction.run(context, TaskerInput(validInput))

        // Assert
        assert(result is TaskerPluginResultSuccess)
        assert((result as TaskerPluginResultSuccess<MQTTEventOutput>).regular.topic == "test/topic")
        assert(result.regular.payload == "Hello, MQTT!")

        // Allow coroutines to complete
        advanceUntilIdle()
    }

    @Test
    fun `test invalid input returns error`() = testScope.runTest {
        // Arrange
        val invalidInput = MQTTEventInput(
            brokerUrl = "",
            port = 1883,
            clientId = "test-client",
            topic = "test/topic",
            payload = "Hello, MQTT!",
            qos = 0,
            useSsl = false
        )

        // Act
        val result = mqttTaskerAction.run(context, TaskerInput(invalidInput))

        // Assert
        assert(result is TaskerPluginResultError)

        // Allow coroutines to complete
        advanceUntilIdle()
    }

    @Test
    fun `test mqtt client is properly called`() = testScope.runTest {
        // Arrange
        val validInput = MQTTEventInput(
            brokerUrl = "test.mosquitto.org",
            port = 1883,
            clientId = "test-client",
            topic = "test/topic",
            payload = "Hello, MQTT!",
            qos = 1,
            useSsl = false
        )

        // Mock client methods
        whenever(mqttClient.connect()).thenReturn(Unit)
        whenever(mqttClient.publish(anyString(), anyString(), anyInt(), anyBoolean())).thenReturn(Unit)
        whenever(mqttClient.disconnect()).thenReturn(Unit)

        // Act
        mqttTaskerAction.run(context, TaskerInput(validInput))

        // Allow coroutines to complete
        advanceUntilIdle()

        // Assert - verify client methods were called with correct parameters
        verify(mqttClient).connect()
        verify(mqttClient).publish(
            eq("test/topic"),
            eq("Hello, MQTT!"),
            eq(1),
            eq(false)
        )
        verify(mqttClient).disconnect()
    }
}
