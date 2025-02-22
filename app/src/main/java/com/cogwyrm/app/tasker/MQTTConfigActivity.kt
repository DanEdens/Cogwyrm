package com.cogwyrm.app.tasker

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.cogwyrm.app.MQTTService
import com.cogwyrm.app.R
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import kotlinx.coroutines.*
import java.util.*

class MQTTConfigActivity : AppCompatActivity(), TaskerPluginConfig<MQTTActionInput> {
    override val context get() = applicationContext
    private val helper by lazy { MQTTActionHelper(this) }

    private lateinit var brokerUrlLayout: TextInputLayout
    private lateinit var brokerUrlInput: TextInputEditText
    private lateinit var portLayout: TextInputLayout
    private lateinit var portInput: TextInputEditText
    private lateinit var clientIdLayout: TextInputLayout
    private lateinit var clientIdInput: TextInputEditText
    private lateinit var topicLayout: TextInputLayout
    private lateinit var topicInput: TextInputEditText
    private lateinit var messageLayout: TextInputLayout
    private lateinit var messageInput: TextInputEditText
    private lateinit var qosSpinner: Spinner
    private lateinit var retainedSwitch: SwitchMaterial
    private lateinit var useSslSwitch: SwitchMaterial
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var passwordInput: TextInputEditText
    private lateinit var testButton: Button
    private lateinit var statusText: TextView

    private var testJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mqtt_config)

        initializeViews()
        setupListeners()
        setupQosSpinner()
        helper.onCreate()
    }

    override fun assignFromInput(input: TaskerInput<MQTTActionInput>) {
        input.regular.run {
            brokerUrlInput.setText(brokerUrl)
            portInput.setText(port)
            clientIdInput.setText(clientId)
            topicInput.setText(topic)
            messageInput.setText(message)
            qosSpinner.setSelection(qos)
            retainedSwitch.isChecked = retained
            useSslSwitch.isChecked = useSsl
            usernameInput.setText(username)
            passwordInput.setText(password)
        }
    }

    private fun initializeViews() {
        brokerUrlLayout = findViewById(R.id.brokerUrlLayout)
        brokerUrlInput = findViewById(R.id.brokerUrlInput)
        portLayout = findViewById(R.id.portLayout)
        portInput = findViewById(R.id.portInput)
        clientIdLayout = findViewById(R.id.clientIdLayout)
        clientIdInput = findViewById(R.id.clientIdInput)
        topicLayout = findViewById(R.id.topicLayout)
        topicInput = findViewById(R.id.topicInput)
        messageLayout = findViewById(R.id.messageLayout)
        messageInput = findViewById(R.id.messageInput)
        qosSpinner = findViewById(R.id.qosSpinner)
        retainedSwitch = findViewById(R.id.retainedSwitch)
        useSslSwitch = findViewById(R.id.useSslSwitch)
        usernameLayout = findViewById(R.id.usernameLayout)
        usernameInput = findViewById(R.id.usernameInput)
        passwordLayout = findViewById(R.id.passwordLayout)
        passwordInput = findViewById(R.id.passwordInput)
        testButton = findViewById(R.id.testButton)
        statusText = findViewById(R.id.statusText)
    }

    private fun setupListeners() {
        testButton.setOnClickListener {
            if (validateInputs()) {
                testConnection()
            }
        }

        useSslSwitch.setOnCheckedChangeListener { _, isChecked ->
            portInput.setText(if (isChecked) "8883" else "1883")
        }

        clientIdInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && clientIdInput.text.isNullOrEmpty()) {
                clientIdInput.setText("cogwyrm_${UUID.randomUUID()}")
            }
        }
    }

    private fun setupQosSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.qos_levels,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            qosSpinner.adapter = adapter
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (brokerUrlInput.text.isNullOrBlank()) {
            brokerUrlLayout.error = "Broker URL is required"
            isValid = false
        } else {
            brokerUrlLayout.error = null
        }

        if (portInput.text.isNullOrBlank()) {
            portLayout.error = "Port is required"
            isValid = false
        } else {
            try {
                val port = portInput.text.toString().toInt()
                if (port !in 1..65535) {
                    portLayout.error = "Port must be between 1 and 65535"
                    isValid = false
                } else {
                    portLayout.error = null
                }
            } catch (e: NumberFormatException) {
                portLayout.error = "Invalid port number"
                isValid = false
            }
        }

        if (topicInput.text.isNullOrBlank()) {
            topicLayout.error = "Topic is required"
            isValid = false
        } else {
            topicLayout.error = null
        }

        if (messageInput.text.isNullOrBlank()) {
            messageLayout.error = "Message is required"
            isValid = false
        } else {
            messageLayout.error = null
        }

        return isValid
    }

    private fun testConnection() {
        testJob?.cancel()
        testButton.isEnabled = false
        statusText.text = "Testing connection..."

        testJob = scope.launch {
            try {
                val service = MQTTService()
                service.connect(
                    serverUri = brokerUrlInput.text.toString(),
                    port = portInput.text.toString().toInt(),
                    clientId = clientIdInput.text?.toString(),
                    useSsl = useSslSwitch.isChecked,
                    username = usernameInput.text?.toString(),
                    password = passwordInput.text?.toString()
                )

                delay(2000) // Wait for connection

                if (service.isConnected()) {
                    service.publish(
                        topic = topicInput.text.toString(),
                        message = messageInput.text.toString(),
                        qos = qosSpinner.selectedItemPosition,
                        retained = retainedSwitch.isChecked
                    )
                    statusText.text = "Test successful! Message published."
                } else {
                    statusText.text = "Connection failed"
                }

                service.disconnect()
            } catch (e: Exception) {
                statusText.text = "Error: ${e.message}"
            } finally {
                testButton.isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        testJob?.cancel()
        scope.cancel()
    }

    override fun onBackPressed() {
        helper.finishForTasker()
    }

    override val inputForTasker: TaskerInput<MQTTActionInput>
        get() = TaskerInput(MQTTActionInput(
            brokerUrl = brokerUrlInput.text.toString(),
            port = portInput.text.toString(),
            clientId = clientIdInput.text?.toString(),
            topic = topicInput.text.toString(),
            message = messageInput.text.toString(),
            qos = qosSpinner.selectedItemPosition,
            retained = retainedSwitch.isChecked,
            useSsl = useSslSwitch.isChecked,
            username = usernameInput.text?.toString(),
            password = passwordInput.text?.toString()
        ))
}
