package com.cogwyrm.app.tasker

import android.os.Bundle
import android.widget.*
import com.cogwyrm.app.R
import com.cogwyrm.app.databinding.ActivityMqttEventConfigBinding
import com.cogwyrm.app.utils.ErrorHandler
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import java.util.*

class MQTTEventConfigActivity : TaskerPluginConfigActivity<MQTTEventInput, MQTTEventOutput, MQTTEventRunner, MQTTEventHelper>() {
    private lateinit var binding: ActivityMqttEventConfigBinding

    private lateinit var brokerUrlLayout: TextInputLayout
    private lateinit var portLayout: TextInputLayout
    private lateinit var clientIdLayout: TextInputLayout
    private lateinit var topicLayout: TextInputLayout
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout

    private lateinit var brokerUrlInput: TextInputEditText
    private lateinit var portInput: TextInputEditText
    private lateinit var clientIdInput: TextInputEditText
    private lateinit var topicInput: TextInputEditText
    private lateinit var qosSpinner: Spinner
    private lateinit var useSslSwitch: SwitchMaterial
    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText

    override fun getNewHelper(config: TaskerPluginConfig<MQTTEventInput>): MQTTEventHelper {
        return MQTTEventHelper(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMqttEventConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViews()
        setupQosSpinner()
        setupListeners()
    }

    private fun setupViews() {
        with(binding) {
            brokerUrlLayout = this.brokerUrlLayout
            portLayout = this.portLayout
            clientIdLayout = this.clientIdLayout
            topicLayout = this.topicLayout
            usernameLayout = this.usernameLayout
            passwordLayout = this.passwordLayout

            brokerUrlInput = this.brokerUrlInput
            portInput = this.portInput
            clientIdInput = this.clientIdInput
            topicInput = this.topicInput
            qosSpinner = this.qosSpinner
            useSslSwitch = this.useSslSwitch
            usernameInput = this.usernameInput
            passwordInput = this.passwordInput
        }
    }

    private fun setupListeners() {
        useSslSwitch.setOnCheckedChangeListener { _, isChecked ->
            try {
                if (isChecked && portInput.text.toString().toIntOrNull() != 8883) {
                    portInput.setText("8883")
                } else if (!isChecked && portInput.text.toString().toIntOrNull() == 8883) {
                    portInput.setText("1883")
                }
            } catch (e: Exception) {
                ErrorHandler.handleError(
                    this,
                    CogwyrmError.ConfigurationError(getString(R.string.error_port_invalid)),
                    showDialog = false
                )
            }
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

    override fun assignFromInput(input: TaskerInput<MQTTEventInput>) {
        try {
            with(input.data) {
                brokerUrlInput.setText(brokerUrl)
                portInput.setText(port.toString())
                clientIdInput.setText(clientId)
                topicInput.setText(topic)
                qosSpinner.setSelection(qos)
                useSslSwitch.isChecked = useSsl
                usernameInput.setText(username)
                passwordInput.setText(password)
            }
        } catch (e: Exception) {
            ErrorHandler.handleError(
                this,
                CogwyrmError.ConfigurationError(getString(R.string.error_configuration, e.message)),
                showDialog = true
            )
        }
    }

    override fun getOutput(): MQTTEventOutput {
        return MQTTEventOutput(
            topic = topicInput.text.toString(),
            message = "" // Empty message since this is just configuration
        )
    }

    override val inputForTasker: TaskerInput<MQTTEventInput>
        get() {
            try {
                // Validate all inputs
                val brokerUrl = ErrorHandler.validateBrokerUrl(this, brokerUrlInput.text.toString())
                val port = ErrorHandler.validatePort(this, portInput.text.toString())
                val topic = ErrorHandler.validateTopic(this, topicInput.text.toString())

                return TaskerInput(
                    MQTTEventInput(
                        brokerUrl = brokerUrl,
                        port = port,
                        clientId = clientIdInput.text?.toString(),
                        topic = topic,
                        qos = qosSpinner.selectedItemPosition,
                        useSsl = useSslSwitch.isChecked,
                        username = usernameInput.text?.toString(),
                        password = passwordInput.text?.toString()
                    )
                )
            } catch (e: CogwyrmError) {
                ErrorHandler.handleError(this, e, showDialog = true)
                throw e
            } catch (e: Exception) {
                val error = CogwyrmError.UnknownError(
                    getString(R.string.error_unknown),
                    e
                )
                ErrorHandler.handleError(this, error, showDialog = true)
                throw error
            }
        }

    companion object {
        const val EXTRA_INPUT = "com.cogwyrm.app.tasker.EXTRA_INPUT"
    }
}
