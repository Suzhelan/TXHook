package moe.ore.xposed.ws

import android.util.Log
import bsh.ConsoleInterface
import bsh.Interpreter
import com.google.gson.Gson
import kotlinx.atomicfu.atomic
import moe.ore.txhook.helper.closeQuietly
import moe.ore.xposed.Initiator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.Reader
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class ArkWebSocket(private val url: String) {
    @Suppress("PrivatePropertyName")
    private val GSON = Gson()

    private val isConnected = atomic(false)
    private val client = OkHttpClient()
        .newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private var channel: WebSocket? = null

    fun open(source: Int) {
        if (isConnected.value) return
        val request: Request = Request.Builder()
            .url(url)
            .build()
            .newBuilder()
            .build()
        client.newWebSocket(request, object : WebSocketListener() {
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                this@ArkWebSocket.isConnected.lazySet(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                send(-1, Log.getStackTraceString(t), "Center.error", -100)
                close()
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.string(Charset.defaultCharset()))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val fromService = GSON.fromJson(text, FromService::class.java)
                kotlin.runCatching {
                    when (fromService.cmd) {
                        "hello" -> send(0, "Nice to meet you.", "hello", fromService.seq)
                        "Center.runCode" -> {
                            val code = fromService.params?.get("code")
                            if (code == null) {
                                send(-3, "missing code content", fromService.cmd, fromService.seq)
                                return
                            }
                            val stream = ByteArrayOutputStream()
                            val outStream = PrintStream(stream, true)
                            val bsh = Interpreter()
                            bsh.setClassLoader(Initiator.getHostClassLoader())
                            bsh.setConsole(object : ConsoleInterface {
                                override fun getIn(): Reader {
                                    return System.`in`.reader()
                                }

                                override fun getOut(): PrintStream {
                                    return outStream
                                }

                                override fun getErr(): PrintStream {
                                    return outStream
                                }

                                override fun println(o: Any?) {
                                    outStream.println(o)
                                }

                                override fun print(o: Any?) {
                                    outStream.print(o)
                                }

                                override fun error(o: Any?) {
                                    outStream.println("Error:$o")
                                }
                            })
                            val result = bsh.eval(code) ?: ""
                            val console = stream.toByteArray()
                            outStream.closeQuietly()
                            send(
                                0, result.toString(), fromService.cmd, fromService.seq, hashMapOf(
                                    "console" to console.decodeToString(),
                                )
                            )
                        }
                    }
                }.onFailure {
                    send(-2, Log.getStackTraceString(it), fromService.cmd, fromService.seq)
                }
            }

            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                this@ArkWebSocket.isConnected.lazySet(true)
                this@ArkWebSocket.channel = webSocket
                send(
                    0, "Hello, I am txhook.", "hello", 0, hashMapOf(
                        "source" to source.toString()
                    )
                )
            }
        })
    }

    private fun send(
        result: Int,
        msg: String,
        cmd: String,
        seq: Int,
        data: HashMap<String, String> = hashMapOf()
    ) {
        send(ToService(result, msg, cmd, seq, data))
    }

    fun send(toService: ToService) {
        send(GSON.toJson(toService))
    }

    fun isConnected(): Boolean {
        return isConnected.value
    }

    private fun send(data: String) {
        this.channel?.send(data)
    }

    fun close() {
        this.isConnected.lazySet(false)
        this.channel?.close(0, "active shutdown")
        this.channel = null
    }
}

data class ToService(
    var result: Int,
    var msg: String,
    var cmd: String,
    var seq: Int,
    var data: HashMap<String, String>,
)

data class FromService(
    var cmd: String,
    var seq: Int,
    var params: HashMap<String, String>?,
)