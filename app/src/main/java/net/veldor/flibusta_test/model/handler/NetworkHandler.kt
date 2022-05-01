package net.veldor.flibusta_test.model.handler

import android.util.Log
import java.net.NetworkInterface
import java.util.*
import kotlin.collections.ArrayList

class NetworkHandler {
    fun isVpnConnected(): Boolean {
        val networkList: MutableList<String> = ArrayList()
        try {
            for (networkInterface in Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (networkInterface.isUp) networkList.add(networkInterface.name)
            }
        } catch (ex: Exception) {
            Log.d("surprise", "NetworkHandler.kt 15: error when check connection")
        }

        return networkList.contains("tun0")
    }
}