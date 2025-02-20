package com.madness.mqttcommander

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout

class MainActivity : AppCompatActivity() {
    private var mqttService: MQTTService? = null
    private var isBound = false

    private lateinit var brokerUrlInput: TextInputEditText
    private lateinit var brokerPortInput: TextInputEditText
    private lateinit var clientIdInput: TextInputEditText
    private lateinit var topicInput: TextInputEditText
    private lateinit var messageInput: TextInputEditText
    private lateinit var connectButton: MaterialButton
    private lateinit var publishButton: MaterialButton
    private lateinit var sslSwitch: SwitchMaterial
    private lateinit var statusCard: MaterialCardView

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MQTTService.LocalBinder
            mqttService = binder.getService()
            isBound = true
            updateConnectionStatus()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            updateConnectionStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind to MQTTService
        Intent(this, MQTTService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        brokerUrlInput = findViewById(R.id.broker_url_input)
        brokerPortInput = findViewById(R.id.broker_port_input)
        clientIdInput = findViewById(R.id.client_id_input)
        topicInput = findViewById(R.id.topic_input)
        messageInput = findViewById(R.id.message_input)
        connectButton = findViewById(R.id.connect_button)
        publishButton = findViewById(R.id.publish_button)
        sslSwitch = findViewById(R.id.ssl_switch)
        statusCard = findViewById(R.id.status_card)
    }

    private fun setupListeners() {
        connectButton.setOnClickListener {
            if (isBound) {
                val url = brokerUrlInput.text.toString()
                val port = brokerPortInput.text.toString().toIntOrNull() ?: 1883
                val clientId = clientIdInput.text.toString()
                val useSSL = sslSwitch.isChecked

                mqttService?.connect(url, port, clientId, useSSL)
                updateConnectionStatus()
            }
        }

        publishButton.setOnClickListener {
            if (isBound && mqttService?.isConnected() == true) {
                val topic = topicInput.text.toString()
                val message = messageInput.text.toString()
                mqttService?.publish(topic, message)
                Toast.makeText(this, "Message published!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Not connected to broker", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateConnectionStatus() {
        val isConnected = mqttService?.isConnected() == true
        statusCard.setCardBackgroundColor(
            if (isConnected) getColor(R.color.status_connected)
            else getColor(R.color.status_disconnected)
        )
        connectButton.text = if (isConnected) "Disconnect" else "Connect"
        publishButton.isEnabled = isConnected
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}
