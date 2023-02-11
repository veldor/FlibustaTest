package net.veldor.tor_client.model.managers

import net.veldor.tor_client.model.listeners.LogListener
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

object LaunchLogManager {

    private val listeners: ArrayList<LogListener> = arrayListOf()

    public fun addListener(listener: LogListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    public fun removeListener(listener: LogListener) {
        listeners.remove(listener)
    }

    private val log = ArrayList<String>()

    fun addToLog(message: String) {
        listeners.forEach {
            it.newLogEventReceived(message)
        }
        val preparedMessage = String.format(
            Locale.ENGLISH,
            "%s: %s",
            SimpleDateFormat("hh:mm:ss", Locale("Ru-ru")).format(Date()),
            message
        )
        log.add(preparedMessage)
    }

    fun getFullLog(): ArrayList<String> {
        return log
    }
}