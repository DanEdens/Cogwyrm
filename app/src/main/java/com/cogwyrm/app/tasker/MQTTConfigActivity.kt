package com.cogwyrm.app.tasker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cogwyrm.app.R
import com.cogwyrm.app.mqtt.MQTTClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoReceiver
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MQTTConfigActivity : AppCompatActivity(), TaskerPluginConfig<MQTTEventInput> {
    override val context get() = this
    override val inputForTasker get() = TaskerInput(getCurrentInput())

    private val helper by lazy { MQTTEventHelper(this) }

    // UI components
    private lateinit var brokerUrlInput: TextInputEditText
    private lateinit var portInput: TextInputEditText
    private lateinit var clientIdInput: TextInputEditText
    private lateinit var topicInput: TextInputEditText
    private lateinit var payloadInput: TextInputEditText
    private lateinit var qosInput: TextInputEditText
    private lateinit var useSslSwitch: SwitchMaterial
    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var generateClientIdButton: MaterialButton
    private lateinit var testConnectionButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var cancelButton: MaterialButton

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mqtt_config)

        initializeViews()
        setupListeners()

        // Initialize with existing data if editing
        if (intent?.action == "com.twofortyfouram.locale.intent.action.EDIT_SETTING") {
            helper.onCreate()
        }
    }

    private fun initializeViews() {
        brokerUrlInput = findViewById(R.id.brokerUrlInput)
        portInput = findViewById(R.id.portInput)
        clientIdInput = findViewById(R.id.clientIdInput)
        topicInput = findViewById(R.id.topicInput)
        payloadInput = findViewById(R.id.messageInput)
        qosInput = findViewById(R.id.qosInput)
        useSslSwitch = findViewById(R.id.useSslSwitch)
        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        generateClientIdButton = findViewById(R.id.generateClientIdButton)
        testConnectionButton = findViewById(R.id.testConnectionButton)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)
    }

    private fun setupListeners() {
        generateClientIdButton.setOnClickListener {
            clientIdInput.setText(generateClientId())
        }

        testConnectionButton.setOnClickListener {
            testConnection()
        }

        saveButton.setOnClickListener {
            if (validateInput()) {
                helper.finishForTasker()
            }
        }

        cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun validateInput(): Boolean {
        val brokerUrl = brokerUrlInput.text?.toString() ?: ""
        val topic = topicInput.text?.toString() ?: ""
        val payload = payloadInput.text?.toString() ?: ""

        if (brokerUrl.isEmpty()) {
            Toast.makeText(this, "Broker URL is required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (topic.isEmpty()) {
            Toast.makeText(this, "Topic is required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (payload.isEmpty()) {
            Toast.makeText(this, "Message payload is required", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun testConnection() {
        val brokerUrl = brokerUrlInput.text?.toString() ?: ""
        val port = portInput.text?.toString()?.toIntOrNull() ?: 1883
        val clientId = clientIdInput.text?.toString() ?: generateClientId()
        val useSsl = useSslSwitch.isChecked
        val username = usernameInput.text?.toString()?.takeIf { it.isNotEmpty() }
        val password = passwordInput.text?.toString()?.takeIf { it.isNotEmpty() }

        if (brokerUrl.isEmpty()) {
            Toast.makeText(this, "Broker URL is required", Toast.LENGTH_SHORT).show()
            return
        }

        coroutineScope.launch {
            testConnectionButton.isEnabled = false
            testConnectionButton.text = "Connecting..."

            try {
                val client = MQTTClient(
                    context = this@MQTTConfigActivity,
                    brokerUrl = brokerUrl,
                    port = port.toString(),
                    clientId = clientId,
                    useSsl = useSsl,
                    username = username,
                    password = password
                )

                withContext(Dispatchers.IO) {
                    client.connect()
                    client.disconnect()
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MQTTConfigActivity,
                        "Connection successful!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection test failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MQTTConfigActivity,
                        "Connection failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    testConnectionButton.isEnabled = true
                    testConnectionButton.text = "Test Connection"
                }
            }
        }
    }

    override fun assignFromInput(input: TaskerInput<MQTTEventInput>) {
        val config = input.regular
        brokerUrlInput.setText(config.brokerUrl)
        portInput.setText(config.port.toString())
        clientIdInput.setText(config.clientId)
        topicInput.setText(config.topic)
        payloadInput.setText(config.payload)
        qosInput.setText(config.qos.toString())
        useSslSwitch.isChecked = config.useSsl
        usernameInput.setText(config.username ?: "")
        passwordInput.setText(config.password ?: "")
    }

    private fun getCurrentInput(): MQTTEventInput {
        return MQTTEventInput(
            brokerUrl = brokerUrlInput.text?.toString() ?: "",
            port = portInput.text?.toString()?.toIntOrNull() ?: 1883,
            clientId = clientIdInput.text?.toString() ?: generateClientId(),
            topic = topicInput.text?.toString() ?: "",
            payload = payloadInput.text?.toString() ?: "",
            qos = qosInput.text?.toString()?.toIntOrNull() ?: 0,
            useSsl = useSslSwitch.isChecked,
            username = usernameInput.text?.toString()?.takeIf { it.isNotEmpty() },
            password = passwordInput.text?.toString()?.takeIf { it.isNotEmpty() }
        )
    }

    override fun onBackPressed() {
        helper.finishForTasker()
    }

    private fun generateClientId(): String {
        return "cogwyrm_${UUID.randomUUID().toString().substring(0, 8)}"
    }

    companion object {
        private const val TAG = "MQTTConfigActivity"
    }
}

// Helper class that manages the Tasker plugin lifecycle
class MQTTEventHelper(config: TaskerPluginConfig<MQTTEventInput>) : TaskerPluginConfigHelperNoReceiver<MQTTEventInput, MQTTTaskerAction>(config) {
    override val runnerClass = MQTTTaskerAction::class.java
    override val inputClass = MQTTEventInput::class.java

    override fun addToStringBlurb(input: TaskerInput<MQTTEventInput>, blurbBuilder: StringBuilder) {
        blurbBuilder.apply {
            append("Broker: ${input.regular.brokerUrl}:${input.regular.port}\n")
            append("Topic: ${input.regular.topic}\n")
            append("Payload: ${input.regular.payload.take(20)}${if (input.regular.payload.length > 20) "..." else ""}")
        }
    }
}
