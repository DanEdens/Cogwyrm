package com.cogwyrm.app.mqtt

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.util.concurrent.atomic.AtomicInteger
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
            connect(object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Reconnection successful")
                    isRetrying = false
                    retryAttempts.set(0)
                    pendingReconnect = false
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Reconnection failed: ${exception?.message}")
                    retryAttempts.incrementAndGet()
                    isRetrying = false
                    reconnect()
                }
            })
        }, delay)
    }

    fun connect(callback: IMqttActionListener? = null) {
        val options = MqttConnectOptions().apply {
            isCleanSession = true
            keepAliveInterval = 60
            connectionTimeout = 30
            isAutomaticReconnect = true
        }

        try {
            client.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connection successful")
                    retryAttempts.set(0)
                    callback?.onSuccess(asyncActionToken)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Connection failed: ${exception?.message}")
                    callback?.onFailure(asyncActionToken, exception)
                    if (!isRetrying) {
                        reconnect()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Connection error: ${e.message}")
            callback?.onFailure(null, e)
            if (!isRetrying) {
                reconnect()
            }
        }
    }

    fun publish(topic: String, message: String, qos: Int = 1, retained: Boolean = false) {
        try {
            val mqttMessage = MqttMessage(message.toByteArray()).apply {
                this.qos = qos
                this.isRetained = retained
            }
            client.publish(topic, mqttMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Publish error: ${e.message}")
            throw e
        }
    }

    fun subscribe(topic: String, qos: Int = 1, callback: IMqttActionListener? = null) {
        try {
            client.subscribe(topic, qos, null, callback)
        } catch (e: Exception) {
            Log.e(TAG, "Subscribe error: ${e.message}")
            throw e
        }
    }

    fun disconnect() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            handler.removeCallbacksAndMessages(null)
            if (client.isConnected) {
                client.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error: ${e.message}")
        }
    }

    fun isConnected(): Boolean = client.isConnected

    companion object {
        private const val TAG = "MQTTClient"
    }
}
