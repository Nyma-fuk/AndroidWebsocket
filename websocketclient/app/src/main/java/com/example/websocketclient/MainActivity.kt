package com.example.websocketclient
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.google.android.flexbox.FlexboxLayout
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI
import java.net.URISyntaxException

class MainActivity : AppCompatActivity() {
    private var webSocketClient: WebSocketClient? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isConnected = false
    private var messageRunnable: Runnable? = null // メッセージ送信タスクの参照
    private lateinit var flexboxLayout: FlexboxLayout
    private lateinit var sendButton: Button
    private val numberOfProperties = 24 // プロパティの数

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val connectButton = findViewById<Button>(R.id.connectButton)

        connectButton.setOnClickListener {
            if (!isConnected) {
                val uri = "ws://10.0.2.2:8080"
                connectToServer(uri)
            }
        }
        sendButton = findViewById(R.id.sendButton)
        flexboxLayout = findViewById<FlexboxLayout>(R.id.flex_container)
        setupProperties()
        setupSendButton()
    }

    private fun connectToServer(uri: String) {
        try {
            webSocketClient = object : WebSocketClient(URI(uri)) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    Log.d("WebSocket", "Connection opened")
                    isConnected = true
                    startSendingMessages()
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    Log.d("WebSocket", "Connection closed")
                    isConnected = false
                    stopSendingMessages() // 送信タスクを停止
                    webSocketClient?.reconnect()
                }

                override fun onMessage(message: String?) {
                    Log.d("WebSocket", "Received message: $message")
                    message?.let {
                        try {
                            val jsonObject = JSONObject(it)
                            // UIスレッドでEditTextの値を更新する
                            handler.post {
                                updateNumberPickers(jsonObject)
                            }
                        } catch (e: Exception) {
                            Log.e("WebSocket", "Error parsing JSON: ${e.message}")
                        }
                    }
                }

                override fun onError(ex: Exception?) {
                    Log.e("WebSocket", "Error: ${ex?.message}")
                    isConnected = false
                }
            }
            webSocketClient?.connect()
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    private fun startSendingMessages() {
        messageRunnable = Runnable {
            sendData()
            handler.postDelayed(messageRunnable!!, 500)
        }.also { handler.postDelayed(it, 500) }
    }

    private fun stopSendingMessages() {
        messageRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun reconnect() {
        handler.postDelayed({
            if (!isConnected) {
                stopSendingMessages() // 再接続前に送信タスクを確実に停止
                webSocketClient?.reconnect()
            }
        }, 3000)
    }
    private fun setupProperties() {
        val flexboxLayout = findViewById<FlexboxLayout>(R.id.flex_container)

        for (i in 1..numberOfProperties) {
            // 一つのペアを格納するためのLinearLayout
            val pairContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = FlexboxLayout.LayoutParams(
                    0,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    flexBasisPercent = 1f / 8f // 画面幅の1/8を基準にする
                }
            }

            val textView = TextView(this).apply {
                text = "PropID $i"
            }

            val numberPicker = NumberPicker(this).apply {
                minValue = 0
                maxValue = 999
            }

            // LinearLayoutにTextViewとNumberPickerを追加
            pairContainer.addView(textView)
            pairContainer.addView(numberPicker)

            // FlexboxLayoutにペアのコンテナを追加
            flexboxLayout.addView(pairContainer)
        }
    }

    // ユーティリティ関数: DPをピクセル単位に変換（既存のメソッドを再利用）
    private fun Int.dpToPixelsInt(context: Context): Int {
        val metrics = context.resources.displayMetrics
        return (this * metrics.density).toInt()
    }

    private fun setupSendButton() {
        sendButton.setOnClickListener {
            val jsonObject = JSONObject()
            var index = 1
            flexboxLayout.children.forEach { view ->
                if (view is NumberPicker) {
                    jsonObject.put(index.toString(), view.value)
                    index++
                }
            }
            // JSONデータをWebSocketサーバーに送信
            sendJsonToServer(jsonObject)
        }
    }

    private fun sendData() {
        val jsonObject = JSONObject()
        var index = 1 // キーが1から始まる
        flexboxLayout.children.forEach { container ->
            if (container is LinearLayout) {
                // LinearLayoutの中のNumberPickerを探す
                val numberPicker = container.children.find { it is NumberPicker } as NumberPicker?
                numberPicker?.let {
                    jsonObject.put("$index", it.value)
                    index++
                }
            }
        }
        // JSONデータをWebSocketサーバーに送信
        sendJsonToServer(jsonObject)
    }

    private fun updateNumberPickers(jsonObject: JSONObject) {
        var index = 1 // キーが1から始まる
        flexboxLayout.children.forEach { container ->
            if (container is LinearLayout) {
                // LinearLayoutの中のNumberPickerを探す
                val numberPicker = container.children.find { it is NumberPicker } as NumberPicker?
                numberPicker?.let {
                    jsonObject.optString(index.toString()).toIntOrNull()?.let { newValue ->
                        it.value = newValue
                    }
                    index++
                }
            }
        }
    }

    private fun sendJsonToServer(jsonObject: JSONObject) {
        if (isConnected && webSocketClient != null) {
            // JSONオブジェクトを文字列に変換して送信
            webSocketClient?.send(jsonObject.toString())
            Log.d("WebSocket", "JSON sent to server: ${jsonObject.toString()}")
        } else {
            Log.e("WebSocket", "WebSocket is not connected. Cannot send JSON.")
            // ここで接続がない場合の処理を追加することもできます（例: 再接続の試みなど）
        }
    }
}