package net.veldor.tor_client.model.bridges

import android.content.Context
import android.os.Build
import android.text.TextUtils
import net.veldor.tor_client.R
import java.util.regex.Pattern

class SnowflakeConfigurator(val context: Context) {

    private val AMP_CACHE = 1
    private val FASTLY = 2

    fun getConfiguration(): String {
        return "ClientTransportPlugin " + getConfiguration(0, "")
    }

    fun getConfiguration(rendezvous: Int): String {
        return getConfiguration(rendezvous, "")
    }

    fun getConfiguration(stunServers: String): String {
        return getConfiguration(0, stunServers)
    }

    private fun getConfiguration(rendezvousType: Int, servers: String): String {
        val appDataDir: String = context.applicationInfo.dataDir
        val nativeLibPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            context.applicationInfo.nativeLibraryDir
        } else {
            TODO("VERSION.SDK_INT < GINGERBREAD")
        }
        val snowflakePath = "$nativeLibPath/libsnowflake.so"
        val stunServers: String
        if (servers.isEmpty()) {
            stunServers = getStunServers()
        } else {
            stunServers = servers
        }
        val stunServerBuilder = StringBuilder()
        val stunServersArr = stunServers.split(", ?".toRegex()).toTypedArray()
        val pattern = Pattern.compile(".+\\..+:\\d+")
        for (server in stunServersArr) {
            val matcher = pattern.matcher(server)
            if (matcher.matches()) {
                stunServerBuilder.append("stun:").append(server.trim { it <= ' ' }).append(",")
            }
        }
        stunServerBuilder.deleteCharAt(stunServerBuilder.lastIndexOf(","))
        val saveLogsString = ""
        return if (rendezvousType == AMP_CACHE) {
            ("snowflake exec "
                    + snowflakePath + " -url https://snowflake-broker.torproject.net/"
                    + " -ampcache https://cdn.ampproject.org/"
                    + " -front www.google.com -ice "
                    + stunServerBuilder
                    + " -max 1"
                    + saveLogsString)
        } else if (rendezvousType == FASTLY) {
            ("snowflake exec "
                    + snowflakePath + " -url https://snowflake-broker.torproject.net.global.prod.fastly.net/"
                    + " -front cdn.sstatic.net -ice "
                    + stunServerBuilder
                    + " -max 1"
                    + saveLogsString)
        } else {
            ""
        }
    }


    private fun getStunServers(): String {
        return TextUtils.join(
            ",", context.resources.getStringArray(R.array.tor_snowflake_stun_servers)
        )
    }

    fun getUtlsClientID(): String {
        val hellorandomizedalpn = "hellorandomizedalpn"
        val hellorandomizednoalpn = "hellorandomizednoalpn"
        val hellofirefox_auto = "hellofirefox_auto"
        val hellofirefox_55 = "hellofirefox_55"
        val hellofirefox_56 = "hellofirefox_56"
        val hellofirefox_63 = "hellofirefox_63"
        val hellofirefox_65 = "hellofirefox_65"
        val hellochrome_auto = "hellochrome_auto"
        val hellochrome_58 = "hellochrome_58"
        val hellochrome_62 = "hellochrome_62"
        val hellochrome_70 = "hellochrome_70"
        val hellochrome_72 = "hellochrome_72"
        val helloios_auto = "helloios_auto"
        val helloios_11_1 = "helloios_11_1"
        val helloios_12_1 = "helloios_12_1"
        return hellorandomizedalpn
    }

}