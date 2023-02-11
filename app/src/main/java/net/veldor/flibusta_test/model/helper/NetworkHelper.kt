package net.veldor.flibusta_test.model.helper

import android.util.Log
import net.veldor.flibusta_test.model.exception.IllegalFormatException
import java.net.NetworkInterface
import java.util.*

class NetworkHelper {
    fun isVpnConnected(): Boolean {
        val networkList: MutableList<String> = ArrayList()
        try {
            for (networkInterface in Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (networkInterface.isUp) networkList.add(networkInterface.name)
            }
        } catch (ex: Exception) {
            Log.d("surprise", "NetworkHandler.kt 15: error check vpn")
        }

        return networkList.contains("tun0")
    }

    fun getExtensionFromHeaders(headers: HashMap<String, String>): String {
        // if not- check Content-Disposition
        val contentDisposition = headers["Content-Disposition"]
        if (!contentDisposition.isNullOrEmpty()) {
            Log.d("surprise", "NetworkHelper: 26 $contentDisposition")
            if (contentDisposition.startsWith("attachment")) {
                val content = contentDisposition.removeSuffix("\"")
                return content.substringAfterLast(".")
            }
        }
        Log.d("surprise", "NetworkHelper.kt 38: $headers")
        val contentType = headers["Content-Type"]
        if (!contentType.isNullOrEmpty()) {
            if (contentType != "application/octet-stream") {
                return MimeHelper.getDownloadMime(contentType)
            }
        }
        throw IllegalFormatException()
    }
}