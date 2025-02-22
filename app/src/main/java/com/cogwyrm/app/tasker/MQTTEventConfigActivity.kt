package com.cogwyrm.app.tasker

import android.os.Bundle
import android.widget.*
import com.cogwyrm.app.R
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.cogwyrm.app.databinding.ActivityMqttEventConfigBinding

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

        // Load existing input if available
        intent.getBundleExtra(EXTRA_INPUT)?.let { bundle ->
            val input = MQTTEventInput.fromBundle(bundle)
            assignFromInput(input)
        }
    }

    private fun setupViews() {
        brokerUrlLayout = binding.brokerUrlLayout
        portLayout = binding.portLayout
        clientIdLayout = binding.clientIdLayout
        topicLayout = binding.topicLayout
        usernameLayout = binding.usernameLayout
        passwordLayout = binding.passwordLayout

        brokerUrlInput = binding.brokerUrlInput
        portInput = binding.portInput
        clientIdInput = binding.clientIdInput
        topicInput = binding.topicInput
        qosSpinner = binding.qosSpinner
        useSslSwitch = binding.useSslSwitch
        usernameInput = binding.usernameInput
        passwordInput = binding.passwordInput
    }

    override fun assignFromInput(input: TaskerInput<MQTTEventInput>) {
        brokerUrlInput.setText(input.data.brokerUrl)
        portInput.setText(input.data.port.toString())
        clientIdInput.setText(input.data.clientId)
        topicInput.setText(input.data.topic)
        qosSpinner.setSelection(input.data.qos)
        useSslSwitch.isChecked = input.data.useSsl
        usernameInput.setText(input.data.username)
        passwordInput.setText(input.data.password)
    }

    override val inputForTasker: TaskerInput<MQTTEventInput>
        get() = TaskerInput(
            MQTTEventInput(
                brokerUrl = brokerUrlInput.text.toString(),
                port = portInput.text.toString().toIntOrNull() ?: 1883,
                clientId = clientIdInput.text?.toString(),
                topic = topicInput.text.toString(),
                qos = qosSpinner.selectedItemPosition,
                useSsl = useSslSwitch.isChecked,
                username = usernameInput.text?.toString(),
                password = passwordInput.text?.toString()
            )
        )

    override fun getOutput(): MQTTEventOutput {
        return MQTTEventOutput(
            topic = topicInput.text.toString(),
            message = "" // Empty message since this is just configuration
        )
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

    companion object {
        const val EXTRA_INPUT = "com.cogwyrm.app.tasker.EXTRA_INPUT"
    }
}
