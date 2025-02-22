package com.cogwyrm.app.tasker

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cogwyrm.app.R
import com.cogwyrm.app.mqtt.MQTTClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MQTTEventConfigActivity : AppCompatActivity(), TaskerPluginConfig<MQTTEventInput> {
    override val context: Context
        get() = this

    private val taskerHelper by lazy { MQTTEventHelper(this) }

    override val inputForTasker: TaskerInput<MQTTEventInput>
        get() = TaskerInput(getCurrentInput())

    private lateinit var brokerUrlInput: TextInputEditText
    private lateinit var portInput: TextInputEditText
    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var clientIdInput: TextInputEditText
    private lateinit var topicInput: TextInputEditText
    private lateinit var qosInput: TextInputEditText
    private lateinit var cleanSessionSwitch: SwitchMaterial
    private lateinit var keepAliveInput: TextInputEditText
    private lateinit var connectionTimeoutInput: TextInputEditText
    private lateinit var useSslSwitch: SwitchMaterial
    private lateinit var autoReconnectSwitch: SwitchMaterial
    private lateinit var maxReconnectDelayInput: TextInputEditText
    private lateinit var generateClientIdButton: MaterialButton
    private lateinit var testConnectionButton: MaterialButton
    private lateinit var connectionStatusText: TextView

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mqtt_event_config)
        initializeViews()
        setupValidation()
        loadInitialValues()
    }

    private fun initializeViews() {
        brokerUrlInput = findViewById(R.id.brokerUrlInput)
        portInput = findViewById(R.id.portInput)
        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        clientIdInput = findViewById(R.id.clientIdInput)
        topicInput = findViewById(R.id.topicInput)
        qosInput = findViewById(R.id.qosInput)
        cleanSessionSwitch = findViewById(R.id.cleanSessionSwitch)
        keepAliveInput = findViewById(R.id.keepAliveInput)
        connectionTimeoutInput = findViewById(R.id.connectionTimeoutInput)
        useSslSwitch = findViewById(R.id.useSslSwitch)
        autoReconnectSwitch = findViewById(R.id.autoReconnectSwitch)
        maxReconnectDelayInput = findViewById(R.id.maxReconnectDelayInput)
        generateClientIdButton = findViewById(R.id.generateClientIdButton)
        testConnectionButton = findViewById(R.id.testConnectionButton)
        connectionStatusText = findViewById(R.id.connectionStatusText)

        generateClientIdButton.setOnClickListener {
            clientIdInput.setText(MQTTEventInput.generateClientId())
        }

        testConnectionButton.setOnClickListener {
            testConnection()
        }
    }

    private fun setupValidation() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInput()
            }
        }

        brokerUrlInput.addTextChangedListener(textWatcher)
        portInput.addTextChangedListener(textWatcher)
        topicInput.addTextChangedListener(textWatcher)
        qosInput.addTextChangedListener(textWatcher)
    }

    private fun getCurrentInput(): MQTTEventInput {
        return MQTTEventInput(
            brokerUrl = brokerUrlInput.text?.toString() ?: "",
            port = portInput.text?.toString()?.toIntOrNull() ?: 1883,
            clientId = clientIdInput.text?.toString() ?: "",
            topic = topicInput.text?.toString() ?: "",
            qos = qosInput.text?.toString()?.toIntOrNull() ?: 0,
            cleanSession = cleanSessionSwitch.isChecked,
            keepAlive = keepAliveInput.text?.toString()?.toIntOrNull() ?: 60,
            connectionTimeout = connectionTimeoutInput.text?.toString()?.toIntOrNull() ?: 30,
            useSSL = useSslSwitch.isChecked,
            username = usernameInput.text?.toString(),
            password = passwordInput.text?.toString(),
            autoReconnect = autoReconnectSwitch.isChecked,
            maxReconnectDelay = maxReconnectDelayInput.text?.toString()?.toIntOrNull() ?: 128
        )
    }

    private fun validateInput(): Boolean {
        val result = taskerHelper.isInputValid(inputForTasker)
        return result.success
    }

    override fun assignFromInput(input: TaskerInput<MQTTEventInput>) {
        val config = input.regular
        brokerUrlInput.setText(config.brokerUrl)
        portInput.setText(config.port.toString())
        usernameInput.setText(config.username)
        passwordInput.setText(config.password)
        clientIdInput.setText(config.clientId)
        topicInput.setText(config.topic)
        qosInput.setText(config.qos.toString())
        cleanSessionSwitch.isChecked = config.cleanSession
        keepAliveInput.setText(config.keepAlive.toString())
        connectionTimeoutInput.setText(config.connectionTimeout.toString())
        useSslSwitch.isChecked = config.useSSL
        autoReconnectSwitch.isChecked = config.autoReconnect
        maxReconnectDelayInput.setText(config.maxReconnectDelay.toString())
    }

    private fun loadInitialValues() {
        val input = inputForTasker
        assignFromInput(input)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun testConnection() {
        if (!validateInput()) {
            return
        }

        testConnectionButton.isEnabled = false
        connectionStatusText.text = getString(R.string.tasker_notification_text)

        scope.launch {
            try {
                val config = inputForTasker.regular
                val client = MQTTClient(
                    context = this@MQTTEventConfigActivity,
                    brokerUrl = config.brokerUrl,
                    port = config.port,
                    clientId = config.clientId,
                    useSsl = config.useSSL,
                    username = config.username,
                    password = config.password
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

    override fun onBackPressed() {
        if (validateInput()) {
            super.onBackPressed()
        } else {
            showError(getString(R.string.error_invalid_input))
        }
    }
}
