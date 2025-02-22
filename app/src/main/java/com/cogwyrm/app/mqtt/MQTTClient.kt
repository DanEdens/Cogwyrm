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
import kotlinx.coroutines.withContext
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
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
    private var mqttClient: MqttAsyncClient? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var _isConnected = false

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

    suspend fun connect(
        brokerUrl: String,
        port: Int,
        clientId: String? = null,
        useSsl: Boolean = false,
        username: String? = null,
        password: String? = null
    ) = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val serverUri = if (useSsl) {
                    "ssl://$brokerUrl:$port"
                } else {
                    "tcp://$brokerUrl:$port"
                }

                val finalClientId = clientId ?: "cogwyrm_${System.currentTimeMillis()}"

                mqttClient = MqttAsyncClient(
                    serverUri,
                    finalClientId,
                    MemoryPersistence()
                )

                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 30
                    keepAliveInterval = 60
                    if (username != null && password != null) {
                        this.userName = username
                        this.password = password.toCharArray()
                    }
                }

                mqttClient?.connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        _isConnected = true
                        continuation.resume(Unit)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val error = exception ?: Exception("Failed to connect")
                        continuation.resumeWithException(error)
                    }
                })
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                mqttClient?.disconnect(null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        _isConnected = false
                        continuation.resume(Unit)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val error = exception ?: Exception("Failed to disconnect")
                        continuation.resumeWithException(error)
                    }
                })
            } catch (e: Exception) {
                continuation.resumeWithException(e)
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
                mqttClient?.publish(
                    topic,
                    MqttMessage(message.toByteArray()).apply {
                        this.qos = qos
                        isRetained = retained
                    },
                    null,
                    object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            continuation.resume(Unit)
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            val error = exception ?: Exception("Failed to publish message")
                            continuation.resumeWithException(error)
                        }
                    }
                )
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun subscribe(
        topic: String,
        qos: Int = 1,
        callback: (String, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                mqttClient?.subscribe(topic, qos, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        mqttClient?.setCallback(object : MqttCallback {
                            override fun connectionLost(cause: Throwable?) {
                                _isConnected = false
                            }

                            override fun messageArrived(topic: String, message: MqttMessage) {
                                callback(topic, String(message.payload))
                            }

                            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                        })
                        continuation.resume(Unit)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val error = exception ?: Exception("Failed to subscribe")
                        continuation.resumeWithException(error)
                    }
                })
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun unsubscribe(topic: String) = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                mqttClient?.unsubscribe(topic, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        continuation.resume(Unit)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val error = exception ?: Exception("Failed to unsubscribe")
                        continuation.resumeWithException(error)
                    }
                })
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    fun isConnected() = _isConnected

    companion object {
        private const val TAG = "MQTTClient"
    }
}
