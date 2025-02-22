package com.cogwyrm.app.mqtt

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min
import kotlin.math.pow

class MQTTClient(
    private val context: Context,
    private val brokerUrl: String,
    private val port: String,
    private val clientId: String,
    private val useSsl: Boolean,
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
                        onMessageArrived?.invoke(topic, messageStr)
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

    suspend fun connect() = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    keepAliveInterval = 60
                    connectionTimeout = 30
                    isAutomaticReconnect = true
                }

                client.connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d(TAG, "Connection successful")
                        isRetrying = false
                        retryAttempts.set(0)
                        pendingReconnect = false
                        continuation.resume(Unit)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e(TAG, "Connection failed: ${exception?.message}")
                        retryAttempts.incrementAndGet()
                        isRetrying = false
                        val error = exception ?: Exception("Connection failed")
                        continuation.resumeWithException(error)
                        reconnect()
                    }
                })

                continuation.invokeOnCancellation {
                    try {
                        if (client.isConnected) {
                            client.disconnect()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during cancellation", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection error: ${e.message}")
                continuation.resumeWithException(e)
                if (!isRetrying) {
                    reconnect()
                }
            }
        }
    }

    suspend fun publish(
        topic: String,
        message: String,
        qos: Int = 1,
        retained: Boolean = false
    ) = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val mqttMessage = MqttMessage(message.toByteArray()).apply {
                    this.qos = qos
                    this.isRetained = retained
                }
                client.publish(topic, mqttMessage, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d(TAG, "Message published successfully")
                        continuation.resume(Unit)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val error = exception ?: Exception("Failed to publish message")
                        Log.e(TAG, "Publish error: ${error.message}")
                        continuation.resumeWithException(error)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Publish error: ${e.message}")
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun subscribe(
        topic: String,
        qos: Int = 1
    ) = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                client.subscribe(topic, qos, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d(TAG, "Subscribed successfully to: $topic")
                        continuation.resume(Unit)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val error = exception ?: Exception("Failed to subscribe")
                        Log.e(TAG, "Subscribe error: ${error.message}")
                        continuation.resumeWithException(error)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Subscribe error: ${e.message}")
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun unsubscribe(topic: String) = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                client.unsubscribe(topic, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d(TAG, "Unsubscribed successfully from: $topic")
                        continuation.resume(Unit)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val error = exception ?: Exception("Failed to unsubscribe")
                        Log.e(TAG, "Unsubscribe error: ${error.message}")
                        continuation.resumeWithException(error)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Unsubscribe error: ${e.message}")
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            handler.removeCallbacksAndMessages(null)
            if (client.isConnected) {
                suspendCancellableCoroutine { continuation ->
                    client.disconnect(null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d(TAG, "Disconnected successfully")
                            continuation.resume(Unit)
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            val error = exception ?: Exception("Failed to disconnect")
                            Log.e(TAG, "Disconnect error: ${error.message}")
                            continuation.resumeWithException(error)
                        }
                    })
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error: ${e.message}")
            throw e
        }
    }

    fun isConnected(): Boolean = client.isConnected

    companion object {
        private const val TAG = "MQTTClient"
    }
}
