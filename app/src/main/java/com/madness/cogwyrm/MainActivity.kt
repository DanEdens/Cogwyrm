package com.cogwyrm.app

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private var mqttService: MQTTService? = null
    private var isBound = false
    private lateinit var messageAdapter: MessageAdapter
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateMessageHistory()
            handler.postDelayed(this, 1000)
        }
    }

    private lateinit var brokerUrlInput: TextInputEditText
    private lateinit var brokerPortInput: TextInputEditText
    private lateinit var clientIdInput: TextInputEditText
    private lateinit var topicInput: TextInputEditText
    private lateinit var messageInput: TextInputEditText
    private lateinit var connectButton: MaterialButton
    private lateinit var publishButton: MaterialButton
    private lateinit var subscribeButton: MaterialButton
    private lateinit var sslSwitch: SwitchMaterial
    private lateinit var statusCard: MaterialCardView
    private lateinit var messagesRecyclerView: RecyclerView

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MQTTService.LocalBinder
            mqttService = binder.getService()
            isBound = true
            updateConnectionStatus()
            handler.post(updateRunnable)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            updateConnectionStatus()
            handler.removeCallbacks(updateRunnable)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind to MQTTService
        Intent(this, MQTTService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        initializeViews()
        setupListeners()
        setupRecyclerView()
    }

    private fun initializeViews() {
        brokerUrlInput = findViewById(R.id.broker_url_input)
        brokerPortInput = findViewById(R.id.broker_port_input)
        clientIdInput = findViewById(R.id.client_id_input)
        topicInput = findViewById(R.id.topic_input)
        messageInput = findViewById(R.id.message_input)
        connectButton = findViewById(R.id.connect_button)
        publishButton = findViewById(R.id.publish_button)
        subscribeButton = findViewById(R.id.subscribe_button)
        sslSwitch = findViewById(R.id.ssl_switch)
        statusCard = findViewById(R.id.status_card)
        messagesRecyclerView = findViewById(R.id.messages_recycler_view)
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter()
        messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = messageAdapter
        }
    }

    private fun setupListeners() {
        connectButton.setOnClickListener {
            if (isBound) {
                if (mqttService?.isConnected() == true) {
                    mqttService?.disconnect()
                } else {
                    val url = brokerUrlInput.text.toString()
                    val port = brokerPortInput.text.toString().toIntOrNull() ?: 1883
                    val clientId = clientIdInput.text.toString()
                    val useSSL = sslSwitch.isChecked

                    mqttService?.connect(url, port, clientId, useSSL)
                }
                updateConnectionStatus()
            }
        }

        publishButton.setOnClickListener {
            if (isBound && mqttService?.isConnected() == true) {
                val topic = topicInput.text.toString()
                val message = messageInput.text.toString()
                mqttService?.publish(topic, message)
            } else {
                Toast.makeText(this, "Not connected to broker", Toast.LENGTH_SHORT).show()
            }
        }

        subscribeButton.setOnClickListener {
            if (isBound && mqttService?.isConnected() == true) {
                val topic = topicInput.text.toString()
                mqttService?.subscribe(topic)
                Toast.makeText(this, "Subscribed to $topic", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Not connected to broker", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateConnectionStatus() {
        val isConnected = mqttService?.isConnected() == true
        statusCard.setCardBackgroundColor(
            if (isConnected) getColor(R.color.status_connected)
            else getColor(R.color.status_disconnected)
        )
        connectButton.text = if (isConnected) "Disconnect" else "Connect"
        publishButton.isEnabled = isConnected
        subscribeButton.isEnabled = isConnected
    }

    private fun updateMessageHistory() {
        mqttService?.getMessageHistory()?.let { messages ->
            messageAdapter.submitList(messages)
            messagesRecyclerView.scrollToPosition(messages.size - 1)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            handler.removeCallbacks(updateRunnable)
            unbindService(connection)
            isBound = false
        }
    }
}

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    private var messages: List<MQTTService.MessageRecord> = emptyList()

    fun submitList(newMessages: List<MQTTService.MessageRecord>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val topicText: TextView = view.findViewById(R.id.message_topic)
        private val messageText: TextView = view.findViewById(R.id.message_content)
        private val timeText: TextView = view.findViewById(R.id.message_time)
        private val directionIcon: ImageView = view.findViewById(R.id.message_direction)

        fun bind(message: MQTTService.MessageRecord) {
            topicText.text = message.topic
            messageText.text = message.message
            timeText.text = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(Date(message.timestamp))
            directionIcon.setImageResource(
                if (message.isIncoming) R.drawable.ic_arrow_downward
                else R.drawable.ic_arrow_upward
            )
        }
    }
}
