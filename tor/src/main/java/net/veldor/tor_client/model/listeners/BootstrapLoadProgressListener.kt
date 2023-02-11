package net.veldor.tor_client.model.listeners

interface BootstrapLoadProgressListener {
    fun tick(totalSeconds: Int, leftSeconds: Int, lastBootstrapLog: String?)
}