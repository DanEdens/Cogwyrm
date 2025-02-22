package com.cogwyrm.app.mqtt

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min
import kotlin.math.pow
import java.net.URI
import javax.net.ssl.SSLSocketFactory

class MQTTClient(
    private val context: Context,
    private val brokerUrl: String,
    private val port: Int,
    private val clientId: String,
    private val useSsl: Boolean,
    private val username: String? = null,
    private val password: String? = null,
    private val onConnectionLost: ((Throwable?) -> Unit)? = null,
    private val onMessageArrived: ((String, String) -> Unit)? = null,
    private val onDeliveryComplete: ((IMqttDeliveryToken?) -> Unit)? = null
) {
    private val retryAttempts = AtomicInteger(0)
    private val maxRetryDelay = 30_000L // 30 seconds
    private val baseRetryDelay = 1_000L // 1 second
    private val handler = Handler(Looper.getMainLooper())
    private var isRetrying = false
    private var pendingReconnect = false
    private var mqttClient: MqttAsyncClient? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var _isConnected = false
    private val callbacks = mutableMapOf<String, (String, String) -> Unit>()

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available")
            if (pendingReconnect) {
                handler.post { reconnect() }
            }
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "Network lost")
            pendingReconnect = true
        }
    }

    private val client: MqttAndroidClient by lazy {
        val serverUri = if (useSsl) {
            "ssl://$brokerUrl:$port"
        } else {
            "tcp://$brokerUrl:$port"
        }
        MqttAndroidClient(context, serverUri, clientId).apply {
            setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.e(TAG, "Connection lost: ${cause?.message}")
                    onConnectionLost?.invoke(cause)
                    if (!isRetrying) {
                        reconnect()
                    }
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    if (topic != null && message != null) {
                        val messageStr = String(message.payload)
                        Log.d(TAG, "Message received: $messageStr on topic: $topic")
                        callbacks[topic]?.invoke(topic, messageStr)
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d(TAG, "Message delivered")
                    onDeliveryComplete?.invoke(token)
                }
            })
        }
    }

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun calculateRetryDelay(): Long {
        val attempt = retryAttempts.get()
        return min(baseRetryDelay * 2.0.pow(attempt.toDouble()).toLong(), maxRetryDelay)
    }

    private fun reconnect() {
        if (isRetrying || client.isConnected) return

        isRetrying = true
        val delay = calculateRetryDelay()
        Log.d(TAG, "Attempting reconnection after $delay ms (attempt: ${retryAttempts.get() + 1})")

        handler.postDelayed({
            connect()
        }, delay)
    }

    suspend fun connect(
        username: String? = null,
        password: String? = null,
        useSsl: Boolean = false,
        maxReconnectDelay: Long = 30000
    ) = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val options = MqttConnectOptions().apply {
                    isAutomaticReconnect = true
                    maxReconnectDelay = maxReconnectDelay
                    if (username?.isNotEmpty() == true && password?.isNotEmpty() == true) {
                        this.userName = username
                        this.password = password.toCharArray()
                    }
                    isCleanSession = true
                    if (useSsl) {
                        socketFactory = SSLSocketFactory.getDefault()
                    }
                }
                val serverURI = buildServerURI()
                mqttClient = MqttAsyncClient(serverURI, clientId, MemoryPersistence())

                mqttClient?.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        onConnectionLost?.invoke(cause)
                        _isConnected = false
                        if (!isRetrying) {
                            reconnect()
                        }
                    }

                    override fun messageArrived(topic: String, message: MqttMessage) {
                        val payload = String(message.payload)
                        callbacks[topic]?.invoke(topic, payload)
                        onMessageArrived?.invoke(topic, payload)
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        onDeliveryComplete?.invoke(token)
                    }
                })

                mqttClient?.connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        _isConnected = true
                        continuation.resume(Unit)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        continuation.resumeWithException(
                            MQTTException("Failed to connect to broker", exception)
                        )
                    }
                })
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            mqttClient?.disconnect()?.waitForCompletion()
            mqttClient?.close()
            mqttClient = null
            callbacks.clear()
        } catch (e: Exception) {
            throw MQTTException("Failed to disconnect from broker", e)
        }
    }

    suspend fun subscribe(topic: String, qos: Int = 0, callback: (String, String) -> Unit) = withContext(Dispatchers.IO) {
        try {
            callbacks[topic] = callback
            mqttClient?.subscribe(topic, qos)?.waitForCompletion()
        } catch (e: Exception) {
            throw MQTTException("Failed to subscribe to topic: $topic", e)
        }
    }

    suspend fun unsubscribe(topic: String) = withContext(Dispatchers.IO) {
        try {
            mqttClient?.unsubscribe(topic)?.waitForCompletion()
            callbacks.remove(topic)
        } catch (e: Exception) {
            throw MQTTException("Failed to unsubscribe from topic: $topic", e)
        }
    }

    suspend fun publish(topic: String, message: String, qos: Int = 0, retained: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            val mqttMessage = MqttMessage(message.toByteArray()).apply {
                this.qos = qos
                this.isRetained = retained
            }
            mqttClient?.publish(topic, mqttMessage)?.waitForCompletion()
        } catch (e: Exception) {
            throw MQTTException("Failed to publish message to topic: $topic", e)
        }
    }

    private fun buildServerURI(): String {
        val protocol = if (useSsl) "ssl" else "tcp"
        val uri = URI(if (brokerUrl.startsWith("tcp://") || brokerUrl.startsWith("ssl://")) {
            brokerUrl
        } else {
            "$protocol://$brokerUrl"
        })
        return "${uri.scheme}://${uri.host}:$port"
    }

    fun isConnected() = _isConnected

    companion object {
        private const val TAG = "MQTTClient"
    }
}

class MQTTException(message: String, cause: Throwable? = null) : Exception(message, cause)
