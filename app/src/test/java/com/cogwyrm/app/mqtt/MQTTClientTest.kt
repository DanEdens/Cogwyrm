package com.cogwyrm.app.mqtt

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
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
class MQTTClientTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var mqttAndroidClient: MqttAndroidClient

    @Mock
    private lateinit var connectivityManager: ConnectivityManager

    @Mock
    private lateinit var handler: Handler

    @Mock
    private lateinit var onConnectionLost: (Throwable?) -> Unit

    @Mock
    private lateinit var onMessageArrived: (String, String) -> Unit

    @Mock
    private lateinit var onDeliveryComplete: (IMqttDeliveryToken?) -> Unit

    private lateinit var mqttClient: MQTTClient

    @Before
    fun setup() {
        // Mock system services
        whenever(context.getSystemService(Context.CONNECTIVITY_SERVICE))
            .thenReturn(connectivityManager)

        // Create MQTTClient instance with mocked dependencies
        mqttClient = MQTTClient(
            context = context,
            brokerUrl = "test.mosquitto.org",
            port = "1883",
            clientId = "test-client",
            useSsl = false,
            onConnectionLost = onConnectionLost,
            onMessageArrived = onMessageArrived,
            onDeliveryComplete = onDeliveryComplete
        )

        // Inject mocked MqttAndroidClient
        val field = MQTTClient::class.java.getDeclaredField("client")
        field.isAccessible = true
        field.set(mqttClient, mqttAndroidClient)
    }

    @Test
    fun `test successful connection`() {
        // Arrange
        val connectOptionsCaptor = ArgumentCaptor.forClass(MqttConnectOptions::class.java)
        val actionListenerCaptor = ArgumentCaptor.forClass(IMqttActionListener::class.java)

        // Act
        mqttClient.connect()

        // Assert
        verify(mqttAndroidClient).connect(
            capture(connectOptionsCaptor),
            any(),
            capture(actionListenerCaptor)
        )

        // Verify connect options
        val connectOptions = connectOptionsCaptor.value
        assert(connectOptions.isCleanSession)
        assert(connectOptions.keepAliveInterval == 60)
        assert(connectOptions.connectionTimeout == 30)
        assert(connectOptions.isAutomaticReconnect)

        // Simulate successful connection
        actionListenerCaptor.value.onSuccess(mock(IMqttToken::class.java))
        verify(mqttAndroidClient, never()).disconnect()
    }

    @Test
    fun `test connection failure with retry`() {
        // Arrange
        val actionListenerCaptor = ArgumentCaptor.forClass(IMqttActionListener::class.java)
        whenever(mqttAndroidClient.isConnected).thenReturn(false)

        // Act
        mqttClient.connect()

        // Assert
        verify(mqttAndroidClient).connect(
            any(),
            any(),
            capture(actionListenerCaptor)
        )

        // Simulate connection failure
        actionListenerCaptor.value.onFailure(
            mock(IMqttToken::class.java),
            RuntimeException("Connection failed")
        )

        // Verify retry attempt
        verify(handler).postDelayed(any(), eq(1000L))
    }

    @Test
    fun `test publish message`() {
        // Arrange
        val topic = "test/topic"
        val message = "test message"
        val messageCaptor = ArgumentCaptor.forClass(MqttMessage::class.java)

        // Act
        mqttClient.publish(topic, message, qos = 1, retained = true)

        // Assert
        verify(mqttAndroidClient).publish(eq(topic), capture(messageCaptor))

        val capturedMessage = messageCaptor.value
        assert(String(capturedMessage.payload) == message)
        assert(capturedMessage.qos == 1)
        assert(capturedMessage.isRetained)
    }

    @Test
    fun `test subscribe to topic`() {
        // Arrange
        val topic = "test/topic"
        val qos = 1

        // Act
        mqttClient.subscribe(topic, qos)

        // Assert
        verify(mqttAndroidClient).subscribe(eq(topic), eq(qos), any(), any())
    }

    @Test
    fun `test network callback registration`() {
        // Arrange
        val networkRequestCaptor = ArgumentCaptor.forClass(NetworkRequest::class.java)
        val networkCallbackCaptor = ArgumentCaptor.forClass(ConnectivityManager.NetworkCallback::class.java)

        // Assert
        verify(connectivityManager).registerNetworkCallback(
            capture(networkRequestCaptor),
            capture(networkCallbackCaptor)
        )

        // Verify network request
        val networkRequest = networkRequestCaptor.value
        assert(networkRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
    }

    @Test
    fun `test network callback triggers reconnect`() {
        // Arrange
        val networkCallbackCaptor = ArgumentCaptor.forClass(ConnectivityManager.NetworkCallback::class.java)
        verify(connectivityManager).registerNetworkCallback(any(), capture(networkCallbackCaptor))

        // Act - simulate network loss and restoration
        networkCallbackCaptor.value.onLost(mock(Network::class.java))
        networkCallbackCaptor.value.onAvailable(mock(Network::class.java))

        // Assert
        verify(handler).post(any())
    }

    @Test
    fun `test disconnect cleanup`() {
        // Arrange
        whenever(mqttAndroidClient.isConnected).thenReturn(true)

        // Act
        mqttClient.disconnect()

        // Assert
        verify(connectivityManager).unregisterNetworkCallback(any())
        verify(handler).removeCallbacksAndMessages(null)
        verify(mqttAndroidClient).disconnect()
    }
}
