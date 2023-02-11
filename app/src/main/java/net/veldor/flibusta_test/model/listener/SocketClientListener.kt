package net.veldor.flibusta_test.model.listener

interface SocketClientListener {
    fun connectionEstablished()
    fun connectionClosed()
    fun messageReceived(message: String?)
    fun connectionError(reason: String?)
}