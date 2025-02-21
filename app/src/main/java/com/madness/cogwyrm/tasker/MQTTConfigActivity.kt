package com.cogwyrm.app.tasker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.cogwyrm.app.R
import com.cogwyrm.app.databinding.ActivityMqttConfigBinding

class MQTTConfigActivity : AppCompatActivity(), TaskerPluginConfig<MQTTActionInput> {
    private lateinit var binding: ActivityMqttConfigBinding
    private val taskerHelper by lazy { MQTTActionHelper(this) }

    override val context get() = this
    override val inputForTasker: TaskerInput<MQTTActionInput>
        get() = TaskerInput(MQTTActionInput(
            brokerUrl = binding.brokerUrlInput.text.toString(),
            port = binding.portInput.text.toString(),
            clientId = binding.clientIdInput.text.toString(),
            useSsl = binding.sslSwitch.isChecked,
            topic = binding.topicInput.text.toString(),
            message = binding.messageInput.text.toString()
        ))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMqttConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            // Set default port
            portInput.setText("1883")

            // Set up save button
            saveButton.setOnClickListener {
                taskerHelper.finishForTasker()
            }
        }

        taskerHelper.onCreate()
    }

    override fun assignFromInput(input: TaskerInput<MQTTActionInput>) {
        with(binding) {
            input.regular.run {
                brokerUrlInput.setText(brokerUrl)
                portInput.setText(port)
                clientIdInput.setText(clientId)
                sslSwitch.isChecked = useSsl
                topicInput.setText(topic)
                messageInput.setText(message)
            }
        }
    }
}
