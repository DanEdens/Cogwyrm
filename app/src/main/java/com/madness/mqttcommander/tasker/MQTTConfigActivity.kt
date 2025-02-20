package com.madness.mqttcommander.tasker

import android.os.Bundle
import android.widget.ArrayAdapter
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.madness.mqttcommander.R
import com.madness.mqttcommander.databinding.ActivityMqttConfigBinding

class MQTTConfigActivity : TaskerPluginConfigNoOutput<MQTTActionInput>() {
    private lateinit var binding: ActivityMqttConfigBinding

    override val context get() = this
    override val inputClass = MQTTActionInput::class.java
    override val runnerClass = MQTTActionRunner::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMqttConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            // Set default port
            portInput.setText("1883")

            // Set up save button
            saveButton.setOnClickListener {
                finish()
            }
        }
    }

    override fun assignFromInput(input: TaskerInput<MQTTActionInput>) {
        input.regular.run {
            binding.apply {
                brokerUrlInput.setText(brokerUrl)
                portInput.setText(port.toString())
                clientIdInput.setText(clientId)
                sslSwitch.isChecked = useSSL
                topicInput.setText(topic)
                messageInput.setText(message)
            }
        }
    }

    override val inputForTasker: TaskerInput<MQTTActionInput>
        get() = TaskerInput(
            MQTTActionInput(
                brokerUrl = binding.brokerUrlInput.text.toString(),
                port = binding.portInput.text.toString().toIntOrNull() ?: 1883,
                clientId = binding.clientIdInput.text.toString(),
                useSSL = binding.sslSwitch.isChecked,
                topic = binding.topicInput.text.toString(),
                message = binding.messageInput.text.toString()
            )
        )
}
