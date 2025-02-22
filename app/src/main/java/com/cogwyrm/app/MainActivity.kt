package com.cogwyrm.app

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cogwyrm.app.mqtt.MQTTService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var brokerUrlInput: EditText
    private lateinit var portInput: EditText
    private lateinit var topicInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var connectButton: Button
    private lateinit var publishButton: Button
    private lateinit var subscribeButton: Button
    private lateinit var statusText: TextView
    private lateinit var mqttService: MQTTService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        brokerUrlInput = findViewById(R.id.broker_url_input)
        portInput = findViewById(R.id.port_input)
        topicInput = findViewById(R.id.topic_input)
        messageInput = findViewById(R.id.message_input)
        connectButton = findViewById(R.id.connect_button)
        publishButton = findViewById(R.id.publish_button)
        subscribeButton = findViewById(R.id.subscribe_button)
        statusText = findViewById(R.id.status_text)

        // Set default values
        brokerUrlInput.setText("test.mosquitto.org")
        portInput.setText("1883")
        topicInput.setText("test/topic")
        messageInput.setText("Hello, MQTT!")

        mqttService = MQTTService(this)

        // Update UI based on connection state
        updateConnectionState()
    }

    private fun updateConnectionState() {
        val isConnected = mqttService.isConnected()
        connectButton.text = if (isConnected) "Disconnect" else "Connect"
        publishButton.isEnabled = isConnected
        subscribeButton.isEnabled = isConnected
        statusText.text = if (isConnected) "Connected" else "Disconnected"
    }

    fun onConnectClick(view: View) {
        if (mqttService.isConnected()) {
            mqttService.disconnect()
            updateConnectionState()
        } else {
            lifecycleScope.launch {
                try {
                    mqttService.connect(
                        brokerUrl = "tcp://${brokerUrlInput.text.toString()}",
                        port = portInput.text.toString().toInt(),
                        clientId = "cogwyrm_test_${System.currentTimeMillis()}",
                        useSsl = false
                    )
                    updateConnectionState()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Connection error", e)
                    statusText.text = "Error: ${e.message}"
                }
            }
        }
    }

    fun onPublishClick(view: View) {
        lifecycleScope.launch {
            try {
                mqttService.publish(
                    topic = topicInput.text.toString(),
                    message = messageInput.text.toString()
                )
                statusText.text = "Message published"
            } catch (e: Exception) {
                Log.e("MainActivity", "Publish error", e)
                statusText.text = "Error: ${e.message}"
            }
        }
    }

    fun onSubscribeClick(view: View) {
        lifecycleScope.launch {
            try {
                mqttService.subscribe(
                    topic = topicInput.text.toString(),
                    qos = 1
                ) { topic, message ->
                    Log.d("MainActivity", "Received message on topic $topic: $message")
                    statusText.text = "Last message: $topic - $message"
                }
                statusText.text = "Subscribed to topic"
            } catch (e: Exception) {
                Log.e("MainActivity", "Subscribe error", e)
                statusText.text = "Error: ${e.message}"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            mqttService.disconnect()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
