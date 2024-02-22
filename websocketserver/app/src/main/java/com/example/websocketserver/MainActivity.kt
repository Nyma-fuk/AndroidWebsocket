package com.example.websocketserver
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.java_websocket.server.WebSocketServer
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.json.JSONObject
import java.net.InetSocketAddress

class MainActivity : AppCompatActivity() {
    private lateinit var server: MyWebSocketServer
    private var requestCount = 0 // リクエストのカウント
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val port = 8080
        server = MyWebSocketServer(port)
        server.start()
        Log.d("WebSocketServer", "WebSocket server running on port $port")
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
        Log.d("WebSocketServer", "WebSocket server stopped")
    }

    inner class MyWebSocketServer(port: Int) : WebSocketServer(InetSocketAddress(port)) {

        override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
            Log.d("WebSocketServer", "New connection from ${conn?.remoteSocketAddress}")
        }

        override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
            Log.d("WebSocketServer", "Closed connection to ${conn?.remoteSocketAddress}")
        }

        override fun onMessage(conn: WebSocket?, message: String?) {
            println("Message received: $message")
            requestCount++ // リクエストカウントをインクリメント
            try {
                val jsonObject = JSONObject(message)
                // 受信したJSONデータの各キーの値を更新する
                val response = JSONObject()
                jsonObject.keys().forEach { key ->
                    // 10回ごとに値を特別に変更する
                    val originalValue = jsonObject.getInt(key)
                    val newValue = if (requestCount % 10 == 0) {
                        // ここでバリューを変更するロジックを実装（例: 値を2倍にする）
                        originalValue + 1
                    } else {
                        originalValue
                    }
                    response.put(key, newValue)
                }
                conn?.send(response.toString())
                println("Sent response: ${response.toString()}")
            } catch (e: Exception) {
                sendErrorResponse(conn, "Invalid JSON format")
                println("Error parsing JSON: ${e.message}")
            }
        }

        override fun onError(conn: WebSocket?, ex: Exception?) {
            Log.e("WebSocketServer", "Error occurred on connection ${conn?.remoteSocketAddress}: ${ex?.message}")
        }

        override fun onStart() {
            Log.d("WebSocketServer", "WebSocket server started")
        }

        private fun processReceivedData(conn: WebSocket?, jsonObject: JSONObject) {
            // 受信したデータを処理し、レスポンスを生成して送信する
            val response = JSONObject().apply {
                put("response", "Data processed successfully")
            }
            conn?.send(response.toString())
        }
        private fun sendErrorResponse(conn: WebSocket?, errorMessage: String) {
            val errorResponse = JSONObject().apply {
                put("error", errorMessage)
            }
            conn?.send(errorResponse.toString())
        }
    }
}

//
//import android.os.Bundle
//import android.util.Log
//import androidx.appcompat.app.AppCompatActivity
//import org.java_websocket.WebSocket
//import org.java_websocket.handshake.ClientHandshake
//import org.java_websocket.server.WebSocketServer
//import org.json.JSONObject
//import java.net.InetSocketAddress
//import java.util.*
//
//class MainActivity : AppCompatActivity() {
//    private var server: WebSocketServerExample? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//        val emulatorPort = getEmulatorPort()
//        Log.d("EmulatorPort", "Emulator Port: $emulatorPort")
//        startServer()
//    }
//    private fun getEmulatorPort(): Int {
//        val fingerprint = android.os.Build.FINGERPRINT
//        val emulatorId = fingerprint.contains("vbox") || fingerprint.contains("generic")
//
//        return if (emulatorId) {
//            // エミュレータの場合、ポート番号を計算
//            val portOffset = android.os.Process.myPid() % 5554
//            5555 + portOffset
//        } else {
//            // 実機の場合、デフォルトのポート番号を返す（仮に8080とします）
//            8080
//        }
//    }
//    private fun startServer() {
//        server = WebSocketServerExample(InetSocketAddress("0.0.0.0", 8080)) // ポート番号は適宜変更
//        server?.start()
//        Log.d("WebSocketServer", "Server started")
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        server?.stop()
//        Log.d("WebSocketServer", "Server stopped")
//    }
//}
//
//class WebSocketServerExample(address: InetSocketAddress) : WebSocketServer(address) {
//
//    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
//        Log.d("WebSocketServer", "New connection from ${conn?.remoteSocketAddress}")
//    }
//
//    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
//        Log.d("WebSocketServer", "Closed connection to ${conn?.remoteSocketAddress}")
//    }
//
//    override fun onMessage(conn: WebSocket?, message: String?) {
//        Log.d("WebSocketServer", "Message received from ${conn?.remoteSocketAddress}: $message")
//        message?.let {
//            parseJsonData(message)
//        }
//    }
//
//    override fun onError(conn: WebSocket?, ex: Exception?) {
//        Log.e("WebSocketServer", "Error occurred on connection ${conn?.remoteSocketAddress}: ${ex?.message}")
//    }
//
//    override fun onStart() {
//        Log.d("WebSocketServer", "WebSocket server started")
//        val sendDataTimer = Timer()
//        sendDataTimer.scheduleAtFixedRate(object : TimerTask() {
//            override fun run() {
//                broadcastData()
//            }
//        }, 0, 500)
//    }
//
//    private fun broadcastData() {
//        val jsonData = JSONObject()
//        jsonData.put("key", "value") // 送信するJSONデータ
//        broadcast(jsonData.toString())
//    }
//
//    private fun parseJsonData(jsonString: String) {
//        val jsonObject = JSONObject(jsonString)
//        val value = jsonObject.getString("key") // 受信したJSONデータを解析
//        Log.d("WebSocketServer", "Parsed JSON data: $value")
//    }
//}
