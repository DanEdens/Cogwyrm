package com.cogwyrm.app.tasker

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cogwyrm.app.R
import com.cogwyrm.app.mqtt.MQTTClient
import com.cogwyrm.app.utils.CogwyrmError
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MQTTEventConfigActivity : AppCompatActivity(), TaskerPluginConfig<MQTTEventInput> {
    private lateinit var brokerUrlInput: TextInputEditText
    private lateinit var brokerUrlLayout: TextInputLayout
    private lateinit var portInput: TextInputEditText
    private lateinit var portLayout: TextInputLayout
    private lateinit var useSslSwitch: SwitchMaterial
    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var clientIdInput: TextInputEditText
    private lateinit var generateClientIdButton: MaterialButton
    private lateinit var topicInput: TextInputEditText
    private lateinit var topicLayout: TextInputLayout
    private lateinit var qosInput: TextInputEditText
    private lateinit var qosLayout: TextInputLayout
    private lateinit var cleanSessionSwitch: SwitchMaterial
    private lateinit var keepAliveInput: TextInputEditText
    private lateinit var keepAliveLayout: TextInputLayout
    private lateinit var connectionTimeoutInput: TextInputEditText
    private lateinit var connectionTimeoutLayout: TextInputLayout
    private lateinit var autoReconnectSwitch: SwitchMaterial
    private lateinit var maxReconnectDelayInput: TextInputEditText
    private lateinit var maxReconnectDelayLayout: TextInputLayout
    private lateinit var testConnectionButton: MaterialButton
    private lateinit var connectionStatusText: TextInputEditText

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mqtt_event_config)
        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        brokerUrlInput = findViewById(R.id.brokerUrlInput)
        brokerUrlLayout = findViewById(R.id.brokerUrlLayout)
        portInput = findViewById(R.id.portInput)
        portLayout = findViewById(R.id.portLayout)
        useSslSwitch = findViewById(R.id.useSslSwitch)
        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        clientIdInput = findViewById(R.id.clientIdInput)
        generateClientIdButton = findViewById(R.id.generateClientIdButton)
        topicInput = findViewById(R.id.topicInput)
        topicLayout = findViewById(R.id.topicLayout)
        qosInput = findViewById(R.id.qosInput)
        qosLayout = findViewById(R.id.qosLayout)
        cleanSessionSwitch = findViewById(R.id.cleanSessionSwitch)
        keepAliveInput = findViewById(R.id.keepAliveInput)
        keepAliveLayout = findViewById(R.id.keepAliveLayout)
        connectionTimeoutInput = findViewById(R.id.connectionTimeoutInput)
        connectionTimeoutLayout = findViewById(R.id.connectionTimeoutLayout)
        autoReconnectSwitch = findViewById(R.id.autoReconnectSwitch)
        maxReconnectDelayInput = findViewById(R.id.maxReconnectDelayInput)
        maxReconnectDelayLayout = findViewById(R.id.maxReconnectDelayLayout)
        testConnectionButton = findViewById(R.id.testConnectionButton)
        connectionStatusText = findViewById(R.id.connectionStatusText)
    }

    private fun setupListeners() {
        generateClientIdButton.setOnClickListener {
            clientIdInput.setText(MQTTEventInput.generateClientId())
        }

        testConnectionButton.setOnClickListener {
            testConnection()
        }

        // Add real-time validation listeners
        brokerUrlInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateBrokerUrl()
        }

        portInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validatePort()
        }

        topicInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateTopic()
        }

        qosInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateQoS()
        }

        keepAliveInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateKeepAlive()
        }

        connectionTimeoutInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateConnectionTimeout()
        }

        maxReconnectDelayInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateMaxReconnectDelay()
        }
    }

    private fun validateBrokerUrl(): Boolean {
        return try {
            inputForTasker.regular.validateBrokerUrl()
            brokerUrlLayout.error = null
            true
        } catch (e: CogwyrmError.ValidationError) {
            brokerUrlLayout.error = e.message
            false
        }
    }

    private fun validatePort(): Boolean {
        return try {
            inputForTasker.regular.validatePort()
            portLayout.error = null
            true
        } catch (e: CogwyrmError.ValidationError) {
            portLayout.error = e.message
            false
        }
    }

    private fun validateTopic(): Boolean {
        return try {
            inputForTasker.regular.validateTopic()
            topicLayout.error = null
            true
        } catch (e: CogwyrmError.ValidationError) {
            topicLayout.error = e.message
            false
        }
    }

    private fun validateQoS(): Boolean {
        return try {
            inputForTasker.regular.validateQoS()
            qosLayout.error = null
            true
        } catch (e: CogwyrmError.ValidationError) {
            qosLayout.error = e.message
            false
        }
    }

    private fun validateKeepAlive(): Boolean {
        val value = keepAliveInput.text.toString().toIntOrNull() ?: -1
        return if (value >= 0) {
            keepAliveLayout.error = null
            true
        } else {
            keepAliveLayout.error = "Keep alive must be non-negative"
            false
        }
    }

    private fun validateConnectionTimeout(): Boolean {
        val value = connectionTimeoutInput.text.toString().toIntOrNull() ?: -1
        return if (value >= 0) {
            connectionTimeoutLayout.error = null
            true
        } else {
            connectionTimeoutLayout.error = "Connection timeout must be non-negative"
            false
        }
    }

    private fun validateMaxReconnectDelay(): Boolean {
        val value = maxReconnectDelayInput.text.toString().toIntOrNull() ?: -1
        return if (value >= 0) {
            maxReconnectDelayLayout.error = null
            true
        } else {
            maxReconnectDelayLayout.error = "Max reconnect delay must be non-negative"
            false
        }
    }

    private fun validateAllFields(): Boolean {
        var isValid = true
        isValid = validateBrokerUrl() && isValid
        isValid = validatePort() && isValid
        isValid = validateTopic() && isValid
        isValid = validateQoS() && isValid
        isValid = validateKeepAlive() && isValid
        isValid = validateConnectionTimeout() && isValid
        isValid = validateMaxReconnectDelay() && isValid
        return isValid
    }

    private fun testConnection() {
        if (!validateAllFields()) {
            return
        }

        testConnectionButton.isEnabled = false
        connectionStatusText.text = "Testing connection..."

        scope.launch {
            try {
                val config = inputForTasker.regular
                val client = MQTTClient(
                    context = this@MQTTEventConfigActivity,
                    brokerUrl = config.brokerUrl,
                    port = config.port.toString(),
                    clientId = config.clientId,
                    useSsl = config.useSSL
                )

                withContext(Dispatchers.IO) {
                    client.connect()
                    client.subscribe(config.topic, config.qos) { _, _ -> }
                    client.disconnect()
                }

                connectionStatusText.text = "Connection test successful!"
                connectionStatusText.setTextColor(getColor(R.color.success_green))
            } catch (e: Exception) {
                connectionStatusText.text = "Connection failed: ${e.message}"
                connectionStatusText.setTextColor(getColor(R.color.error_red))
            } finally {
                testConnectionButton.isEnabled = true
            }
        }
    }

    override fun assignFromInput(input: TaskerInput<MQTTEventInput>) {
        input.regular.let { config ->
            brokerUrlInput.setText(config.brokerUrl)
            portInput.setText(config.port.toString())
            useSslSwitch.isChecked = config.useSSL
            usernameInput.setText(config.username)
            passwordInput.setText(config.password)
            clientIdInput.setText(config.clientId)
            topicInput.setText(config.topic)
            qosInput.setText(config.qos.toString())
            cleanSessionSwitch.isChecked = config.cleanSession
            keepAliveInput.setText(config.keepAlive.toString())
            connectionTimeoutInput.setText(config.connectionTimeout.toString())
            autoReconnectSwitch.isChecked = config.autoReconnect
            maxReconnectDelayInput.setText(config.maxReconnectDelay.toString())
        }
    }

    override val inputForTasker: TaskerInput<MQTTEventInput>
        get() = TaskerInput(MQTTEventInput().apply {
            brokerUrl = brokerUrlInput.text.toString()
            port = portInput.text.toString().toIntOrNull() ?: 1883
            useSSL = useSslSwitch.isChecked
            username = usernameInput.text.toString()
            password = passwordInput.text.toString()
            clientId = clientIdInput.text.toString()
            topic = topicInput.text.toString()
            qos = qosInput.text.toString().toIntOrNull() ?: 0
            cleanSession = cleanSessionSwitch.isChecked
            keepAlive = keepAliveInput.text.toString().toIntOrNull() ?: 60
            connectionTimeout = connectionTimeoutInput.text.toString().toIntOrNull() ?: 30
            autoReconnect = autoReconnectSwitch.isChecked
            maxReconnectDelay = maxReconnectDelayInput.text.toString().toIntOrNull() ?: 128
        })

    override fun onBackPressed() {
        if (validateAllFields()) {
            super.onBackPressed()
        } else {
            Toast.makeText(this, R.string.error_invalid_input, Toast.LENGTH_SHORT).show()
        }
    }
}
