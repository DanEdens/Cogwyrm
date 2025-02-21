package com.cogwyrm.app.tasker

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.cogwyrm.app.R
import com.cogwyrm.app.mqtt.TopicUtils
import com.google.android.material.textfield.TextInputLayout
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import timber.log.Timber

class MQTTEventConfigActivity : AppCompatActivity(), TaskerPluginConfig<MQTTEventInput> {
    private val helper by lazy { MQTTEventHelper(this) }

    private lateinit var brokerUrlInput: EditText
    private lateinit var portInput: EditText
    private lateinit var clientIdInput: EditText
    private lateinit var topicInput: EditText
    private lateinit var topicInputLayout: TextInputLayout
    private lateinit var useSslSwitch: Switch
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var qosInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mqtt_event_config)

        brokerUrlInput = findViewById(R.id.broker_url_input)
        portInput = findViewById(R.id.port_input)
        clientIdInput = findViewById(R.id.client_id_input)
        topicInput = findViewById(R.id.topic_input)
        topicInputLayout = findViewById(R.id.topic_input_layout)
        useSslSwitch = findViewById(R.id.use_ssl_switch)
        usernameInput = findViewById(R.id.username_input)
        passwordInput = findViewById(R.id.password_input)
        qosInput = findViewById(R.id.qos_input)

        setupTopicValidation()
        helper.onCreate()
    }

    private fun setupTopicValidation() {
        topicInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateTopic()
            }
        })
    }

    private fun validateTopic(): Boolean {
        val topic = topicInput.text.toString()
        return if (topic.isEmpty()) {
            topicInputLayout.error = "Topic cannot be empty"
            false
        } else if (!TopicUtils.validateTopic(topic)) {
            topicInputLayout.error = "Invalid topic format. Check wildcard usage."
            false
        } else {
            topicInputLayout.error = null
            true
        }
    }

    override fun onBackPressed() {
        helper.finishForTasker()
    }

    override fun assignFromInput(input: TaskerInput<MQTTEventInput>) {
        input.regular.run {
            brokerUrlInput.setText(brokerUrl)
            portInput.setText(port)
            clientIdInput.setText(clientId)
            topicInput.setText(topic)
            useSslSwitch.isChecked = useSsl
            usernameInput.setText(username)
            passwordInput.setText(password)
            qosInput.setText(qos.toString())
        }
    }

    override val context get() = applicationContext

    override fun getInput(): TaskerInput<MQTTEventInput> {
        return TaskerInput(MQTTEventInput(
            brokerUrl = brokerUrlInput.text.toString(),
            port = portInput.text.toString(),
            clientId = clientIdInput.text.toString().takeIf { it.isNotEmpty() },
            topic = topicInput.text.toString(),
            useSsl = useSslSwitch.isChecked,
            username = usernameInput.text.toString().takeIf { it.isNotEmpty() },
            password = passwordInput.text.toString().takeIf { it.isNotEmpty() },
            qos = qosInput.text.toString().toIntOrNull() ?: 1
        ))
    }

    fun onSave(@Suppress("UNUSED_PARAMETER") view: View) {
        try {
            if (!validateTopic()) {
                return
            }
            if (brokerUrlInput.text.isEmpty()) {
                brokerUrlInput.error = "Broker URL is required"
                return
            }
            helper.finishForTasker()
        } catch (e: Exception) {
            Timber.e(e, "Error saving MQTT event configuration")
            topicInputLayout.error = "Error: ${e.message}"
        }
    }
}
