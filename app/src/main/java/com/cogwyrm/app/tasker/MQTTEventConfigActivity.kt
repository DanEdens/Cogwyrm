package com.cogwyrm.app.tasker

import android.os.Bundle
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cogwyrm.app.R
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import java.util.UUID

class MQTTEventConfigActivity : AppCompatActivity(), TaskerPluginConfig<MQTTEventInput> {
    private lateinit var brokerUrlInput: EditText
    private lateinit var portInput: EditText
    private lateinit var useSslSwitch: Switch
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var clientIdInput: EditText
    private lateinit var topicInput: EditText
    private lateinit var qosInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mqtt_event_config)
        initializeViews()

        if (clientIdInput.text.toString().isEmpty()) {
            clientIdInput.setText(UUID.randomUUID().toString())
        }
    }

    private fun initializeViews() {
        brokerUrlInput = findViewById(R.id.broker_url_input)
        portInput = findViewById(R.id.port_input)
        useSslSwitch = findViewById(R.id.use_ssl_switch)
        usernameInput = findViewById(R.id.username_input)
        passwordInput = findViewById(R.id.password_input)
        clientIdInput = findViewById(R.id.client_id_input)
        topicInput = findViewById(R.id.topic_input)
        qosInput = findViewById(R.id.qos_input)
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
        })

    override fun onBackPressed() {
        if (inputForTasker.regular.validateInput()) {
            finish()
        } else {
            Toast.makeText(this, R.string.error_invalid_input, Toast.LENGTH_SHORT).show()
        }
    }
}
