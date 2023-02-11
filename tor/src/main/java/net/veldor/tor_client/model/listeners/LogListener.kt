package net.veldor.tor_client.model.listeners

interface LogListener {
    fun newLogEventReceived(event: String)
}