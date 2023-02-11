package net.veldor.flibusta_test.model.connection

import android.util.Log
import net.veldor.flibusta_test.model.exception.CompatClientSocketClosedException
import net.veldor.flibusta_test.model.handler.GrammarHandler
import net.veldor.flibusta_test.model.selection.SocketMultiFile
import net.veldor.flibusta_test.view.components.ConnectCompanionAppDialog
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI

class CompanionAppSocketClient(uri: URI) : WebSocketClient(uri) {

    private var connectionActive = false

    private val waitForConfirm: HashMap<String, Boolean> = hashMapOf()

    var listener: ConnectCompanionAppDialog? = null

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.d("surprise", "CompanionAppSocketClient: 10 socket opened")
        listener?.connectionEstablished()
        connectionActive = true
    }

    override fun onMessage(message: String?) {
        Log.d("surprise", "CompanionAppSocketClient: 15 message received")
        try {
            if (message != null) {
                val jsonMessage = JSONObject(message)
                if (jsonMessage.has("command") && jsonMessage.getString("command") == "book_part_received") {
                    val transferId = jsonMessage.getString("transferId")
                    val part = jsonMessage.getInt("part")
                    waitForConfirm["${transferId}_$part"] = true
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            Log.d(
                "surprise",
                "CompanionAppSocketClient: 32 have error when parse message: ${t.message}"
            )
        }
        listener?.messageReceived(message)
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Log.d("surprise", "CompanionAppSocketClient: 19 socket closed")
        connectionActive = false
    }

    override fun onError(ex: Exception?) {
        Log.d("surprise", "CompanionAppSocketClient: 23 socket error")
        Log.d("surprise", "CompanionAppSocketClient: 28 ${ex?.message}")
        listener?.connectionError(ex?.message)
    }

    fun establishConnection() {
        Log.d("surprise", "CompanionAppSocketClient: 32 establishing connection")
        connectBlocking()
        Log.d("surprise", "CompanionAppSocketClient: 34 connection try initiated")
    }

    fun establishConnection(callback: () -> Unit) {
        establishConnection()
        callback()
    }

    fun sendFile(fileData: String, fileName: String) {
        val json = JSONObject()
        json.put("command", "get_book")
        json.put("payload", fileName)
        json.put("value", fileData)
        val request = json.toString()
        Log.d(
            "surprise",
            "CompanionAppSocketClient: 74 request size is ${GrammarHandler.getTextSize(request.length.toLong())}"
        )
        Log.d("surprise", "CompanionAppSocketClient: 60 start send websocket request")
        send(request)
        Log.d("surprise", "CompanionAppSocketClient: 62 request sent")
    }

    fun sendMultiFilePart(it: SocketMultiFile): Boolean {
        val json = JSONObject()
        json.put("command", "get_book_multipart")
        json.put("payload", it.payload)
        json.put("value", it.value)
        json.put("transferId", it.transferId)
        json.put("index", it.currentFileIndex)
        json.put("size", it.size)
        val request = json.toString()
        Log.d(
            "surprise",
            "CompanionAppSocketClient: 60 start send websocket request, size is ${
                GrammarHandler.getTextSize(request.length.toLong())
            }"
        )
        send(request)
        Log.d("surprise", "CompanionAppSocketClient: 62 request sent")
        // wait here for submit to receive
        registerForConfirm(it.transferId, it.currentFileIndex)
        while (true) {
            if(!connectionActive){
                throw CompatClientSocketClosedException()
            }
            Thread.sleep(1000)
            if (receiveConfirmed(it.transferId, it.currentFileIndex)) {
                Log.d("surprise", "CompanionAppSocketClient: 154 receive confirmed!")
                break
            }
            Log.d(
                "surprise",
                "CompanionAppSocketClient: 134 wait for confirm send ${it.transferId} ${it.currentFileIndex}"
            )
        }
        return true
    }

    private fun receiveConfirmed(transferId: String, index: Int): Boolean {
        return waitForConfirm["${transferId}_$index"] == true
    }

    private fun registerForConfirm(transferId: String, index: Int) {
        waitForConfirm["${transferId}_$index"] = false
    }

    companion object {
        const val MAX_SEND_VIA_SOCKET_SIZE = 2048000
    }
}