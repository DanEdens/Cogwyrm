package com.cogwyrm.app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cogwyrm.app.mqtt.MQTTService
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var brokerUrlInput: TextInputEditText
    private lateinit var portInput: TextInputEditText
    private lateinit var topicInput: TextInputEditText
    private lateinit var messageInput: TextInputEditText
    private lateinit var connectButton: MaterialButton
    private lateinit var publishButton: MaterialButton
    private lateinit var subscribeButton: MaterialButton
    private lateinit var connectionStatusText: TextInputEditText
    private lateinit var mqttService: MQTTService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mqtt_event_config)

        // Initialize views
        brokerUrlInput = findViewById(R.id.brokerUrlInput)
        portInput = findViewById(R.id.portInput)
        topicInput = findViewById(R.id.topicInput)
        messageInput = findViewById(R.id.messageInput)
        connectButton = findViewById(R.id.testConnectionButton)
        publishButton = findViewById(R.id.publishButton)
        subscribeButton = findViewById(R.id.subscribeButton)
        connectionStatusText = findViewById(R.id.connectionStatusText)

        // Set default values
        brokerUrlInput.setText("test.mosquitto.org")
        portInput.setText("1883")
        topicInput.setText("test/topic")
        messageInput.setText("Hello, MQTT!")

        mqttService = MQTTService(this)

        // Set click listeners
        connectButton.setOnClickListener { onConnectClick() }
        publishButton.setOnClickListener { onPublishClick() }
        subscribeButton.setOnClickListener { onSubscribeClick() }

        // Update UI based on connection state
        updateConnectionState()
    }

    private fun updateConnectionState() {
        val isConnected = mqttService.isConnected()
        connectButton.text = if (isConnected) "Disconnect" else "Connect"
        publishButton.isEnabled = isConnected
        subscribeButton.isEnabled = isConnected
        connectionStatusText.setText(if (isConnected) "Connected" else "Disconnected")
    }

    private fun onConnectClick() {
        if (mqttService.isConnected()) {
            lifecycleScope.launch {
                try {
                    mqttService.disconnect()
                    updateConnectionState()
                } catch (e: Exception) {
                    Log.e(TAG, "Disconnect error", e)
                    showError("Error: ${e.message}")
                }
            }
        } else {
            lifecycleScope.launch {
                try {
                    mqttService.connect(
                        brokerUrl = brokerUrlInput.text.toString(),
                        port = portInput.text.toString().toInt(),
                        clientId = "cogwyrm_test_${System.currentTimeMillis()}",
                        useSsl = false
                    )
                    updateConnectionState()
                } catch (e: Exception) {
                    Log.e(TAG, "Connection error", e)
                    showError("Error: ${e.message}")
                }
            }
        }
    }

    private fun onPublishClick() {
        lifecycleScope.launch {
            try {
                mqttService.publish(
                    topic = topicInput.text.toString(),
                    message = messageInput.text.toString()
                )
                connectionStatusText.setText("Message published")
            } catch (e: Exception) {
                Log.e(TAG, "Publish error", e)
                showError("Error: ${e.message}")
            }
        }
    }

    private fun onSubscribeClick() {
        lifecycleScope.launch {
            try {
                mqttService.subscribe(
                    topic = topicInput.text.toString(),
                    qos = 1
                ) { topic, message ->
                    Log.d(TAG, "Received message on topic $topic: $message")
                    runOnUiThread {
                        connectionStatusText.setText("Last message: $topic - $message")
                    }
                }
                connectionStatusText.setText("Subscribed to topic")
            } catch (e: Exception) {
                Log.e(TAG, "Subscribe error", e)
                showError("Error: ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            try {
                mqttService.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Disconnect error", e)
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
