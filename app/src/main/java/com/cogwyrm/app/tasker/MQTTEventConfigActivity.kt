package com.cogwyrm.app.tasker

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.cogwyrm.app.R
import com.cogwyrm.app.mqtt.MQTTClient
import com.cogwyrm.app.mqtt.TopicUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken

class MQTTEventConfigActivity : AppCompatActivity(), TaskerPluginConfig<MQTTEventInput> {
    private val helper by lazy { MQTTEventHelper(this) }
    private var testClient: MQTTClient? = null

    private lateinit var brokerUrlInput: EditText
    private lateinit var portInput: EditText
    private lateinit var clientIdInput: EditText
    private lateinit var topicInput: EditText
    private lateinit var topicInputLayout: TextInputLayout
    private lateinit var useSslSwitch: SwitchMaterial
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var qosSpinner: AutoCompleteTextView

    companion object {
        private const val TAG = "MQTTEventConfig"
        private val QOS_LEVELS = arrayOf(
            "QoS 0 (At most once)",
            "QoS 1 (At least once)",
            "QoS 2 (Exactly once)"
        )
        private val QOS_DESCRIPTIONS = arrayOf(
            "Fire and forget - Messages may be lost",
            "Guaranteed delivery - Messages may be duplicated",
            "Exactly once - Highest overhead, guaranteed single delivery"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mqtt_event_config)

        // Initialize views
        brokerUrlInput = findViewById(R.id.broker_url_input)
        portInput = findViewById(R.id.port_input)
        clientIdInput = findViewById(R.id.client_id_input)
        topicInput = findViewById(R.id.topic_input)
        topicInputLayout = findViewById(R.id.topic_input_layout)
        useSslSwitch = findViewById(R.id.use_ssl_switch)
        usernameInput = findViewById(R.id.username_input)
        passwordInput = findViewById(R.id.password_input)
        qosSpinner = findViewById(R.id.qos_spinner)

        setupQosSpinner()
        setupTopicValidation()
        helper.onCreate()
    }

    private fun setupQosSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, QOS_LEVELS)
        qosSpinner.setAdapter(adapter)
        qosSpinner.setText(QOS_LEVELS[1], false) // Default to QoS 1
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

    fun onValidateTopic(@Suppress("UNUSED_PARAMETER") view: View) {
        val topic = topicInput.text.toString()
        if (topic.isEmpty()) {
            showMessage("Please enter a topic pattern")
            return
        }

        val isValid = TopicUtils.validateTopic(topic)
        val message = if (isValid) {
            "Topic pattern is valid!\n\nExample matches:\n" + generateExampleMatches(topic)
        } else {
            "Invalid topic pattern. Please check:\n" +
                "- Single-level wildcard (+) can substitute for one topic level\n" +
                "- Multi-level wildcard (#) must be the last character\n" +
                "- Topic must not be empty\n" +
                "- Topic must not contain invalid characters"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Topic Validation")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun generateExampleMatches(pattern: String): String {
        val examples = StringBuilder()
        val parts = pattern.split("/")

        // Generate a few example matches
        repeat(3) { i ->
            val example = parts.mapIndexed { index, part ->
                when {
                    part == "+" -> "value$i"
                    part == "#" -> "level$i/sublevel$i"
                    else -> part
                }
            }.joinToString("/")
            examples.append("- $example\n")
        }

        return examples.toString()
    }

    fun onQosHelp(@Suppress("UNUSED_PARAMETER") view: View) {
        val message = buildString {
            append("Quality of Service Levels:\n\n")
            QOS_LEVELS.forEachIndexed { index, level ->
                append("$level\n")
                append("${QOS_DESCRIPTIONS[index]}\n\n")
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("QoS Levels")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    fun onTestConnection(@Suppress("UNUSED_PARAMETER") view: View) {
        if (!validateConnectionInputs()) {
            return
        }

        val brokerUrl = brokerUrlInput.text.toString()
        val port = portInput.text.toString()
        val clientId = clientIdInput.text.toString().takeIf { it.isNotEmpty() }
            ?: "cogwyrm_test_${System.currentTimeMillis()}"

        showMessage("Testing connection...")

        testClient?.disconnect()
        testClient = MQTTClient(
            context = this,
            brokerUrl = brokerUrl,
            port = port,
            clientId = clientId,
            useSsl = useSslSwitch.isChecked,
            onConnectionLost = { cause ->
                Log.d(TAG, "Test connection lost: ${cause?.message}")
            },
            onMessageArrived = { _, _ -> },
            onDeliveryComplete = { }
        )

        testClient?.connect(object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                runOnUiThread {
                    showMessage("Connection successful!")
                    testClient?.disconnect()
                    testClient = null
                }
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                runOnUiThread {
                    showMessage("Connection failed: ${exception?.message}")
                    testClient = null
                }
            }
        })
    }

    private fun validateConnectionInputs(): Boolean {
        var isValid = true

        if (brokerUrlInput.text.isEmpty()) {
            brokerUrlInput.error = "Broker URL is required"
            isValid = false
        }

        val port = portInput.text.toString().toIntOrNull()
        if (port == null || port !in 1..65535) {
            portInput.error = "Invalid port number"
            isValid = false
        }

        return isValid
    }

    private fun showMessage(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
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
            qosSpinner.setText(QOS_LEVELS[qos], false)
        }
    }

    override val context get() = applicationContext

    override fun getInput(): TaskerInput<MQTTEventInput> {
        val qosLevel = QOS_LEVELS.indexOf(qosSpinner.text.toString()).takeIf { it != -1 } ?: 1

        return TaskerInput(MQTTEventInput(
            brokerUrl = brokerUrlInput.text.toString(),
            port = portInput.text.toString(),
            clientId = clientIdInput.text.toString().takeIf { it.isNotEmpty() },
            topic = topicInput.text.toString(),
            useSsl = useSslSwitch.isChecked,
            username = usernameInput.text.toString().takeIf { it.isNotEmpty() },
            password = passwordInput.text.toString().takeIf { it.isNotEmpty() },
            qos = qosLevel
        ))
    }

    fun onSave(@Suppress("UNUSED_PARAMETER") view: View) {
        try {
            if (!validateConnectionInputs() || !validateTopic()) {
                return
            }
            helper.finishForTasker()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving MQTT event configuration", e)
            showMessage("Error: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        testClient?.disconnect()
        testClient = null
    }
}
