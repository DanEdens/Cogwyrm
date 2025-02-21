package com.cogwyrm.app.tasker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.cogwyrm.app.databinding.ActivityMqttConfigBinding

class MQTTConfigActivity : AppCompatActivity(), TaskerPluginConfig<MQTTActionInput> {
    private lateinit var binding: ActivityMqttConfigBinding
    private val taskerHelper by lazy { MQTTTaskerAction() }

    override val context get() = applicationContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMqttConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            // Set default port
            portInput.setText("1883")

            // Handle save button click
            saveButton.setOnClickListener {
                val input = MQTTActionInput(
                    brokerUrl = brokerUrlInput.text.toString(),
                    port = portInput.text.toString(),
                    clientId = clientIdInput.text.toString().takeIf { it.isNotEmpty() },
                    topic = topicInput.text.toString(),
                    message = messageInput.text.toString(),
                    useSsl = useSslCheckbox.isChecked
                )
                taskerHelper.finishForTasker(input)
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
