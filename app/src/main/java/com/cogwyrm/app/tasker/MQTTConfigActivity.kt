package com.cogwyrm.app.tasker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.cogwyrm.app.databinding.ActivityMqttConfigBinding

class MQTTConfigActivity : AppCompatActivity(), TaskerPluginConfig<MQTTActionInput> {
    private lateinit var binding: ActivityMqttConfigBinding
    private val taskerHelper by lazy { MQTTTaskerAction(this) }

    override val context get() = applicationContext
    override val inputForTasker: TaskerInput<MQTTActionInput>
        get() = TaskerInput(MQTTActionInput(
            brokerUrl = binding.brokerUrlInput.text.toString(),
            port = binding.portInput.text.toString(),
            clientId = binding.clientIdInput.text.toString().takeIf { it.isNotEmpty() },
            topic = binding.topicInput.text.toString(),
            message = binding.messageInput.text.toString(),
            useSsl = binding.useSslCheckbox.isChecked
        ))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMqttConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            // Set default port
            portInput.setText("1883")

            // Handle save button click
            saveButton.setOnClickListener {
                taskerHelper.finishForTasker()
            }
        }
    }

    override fun assignFromInput(input: TaskerInput<MQTTActionInput>) {
        with(binding) {
            input.regular.run {
                brokerUrlInput.setText(brokerUrl)
                portInput.setText(port)
                clientIdInput.setText(clientId)
                topicInput.setText(topic)
                messageInput.setText(message)
                useSslCheckbox.isChecked = useSsl
            }
        }
    }
}
