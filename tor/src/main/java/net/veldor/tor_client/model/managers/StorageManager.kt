package net.veldor.tor_client.model.managers

import android.annotation.SuppressLint
import android.content.Context
import net.veldor.tor_client.model.utils.ChmodCommand
import java.io.*

class StorageManager {
    fun createLogsDir(appDataDir: String) {
        val logDir = File("$appDataDir/logs")
        if (!logDir.isDirectory) {
            if (logDir.mkdir()) {
                ChmodCommand.dirChmod(logDir.absolutePath, false)
            } else {
                throw IllegalStateException("Installer Create log dir failed")
            }
        }
    }

    @SuppressLint("SetWorldReadable")
    fun readTextFile(filePath: String): List<String> {
        val lines: MutableList<String> = ArrayList()
        val f = File(filePath)
        if (f.isFile) {
            FileInputStream(filePath).use { fStream ->
                BufferedReader(InputStreamReader(fStream)).use { br ->
                    var tmp: String?
                    while (br.readLine().also { tmp = it } != null) {
                        lines.add(tmp!!.trim { it <= ' ' })
                    }
                }
            }
        }
        return lines
    }

    fun editConfigurationFile(context: Context) {
        val appDataDir: String = context.applicationInfo.dataDir
        val currentConfiguration =
            readTextFile("${context.applicationInfo.dataDir}/app_data/tor/tor.conf")
        val clearText = ArrayList<String>(arrayListOf())
        currentConfiguration.forEach {
            clearText.add(it.replace("\$path", appDataDir))
        }
        writeToTextFile(
            "$appDataDir/app_data/tor/tor.conf", clearText
        )
        BridgesManager(context).reloadTorConfigurationWithBridges(context)
    }

    @SuppressLint("SetWorldReadable")
    fun writeToTextFile(filePath: String, lines: List<String?>) {
        val f = File(filePath)
        if (f.isFile) {
            f.canRead() && f.canWrite() || f.setReadable(true, false) && f.setWritable(true)
        }
        PrintWriter(filePath).use { writer ->
            for (line in lines) {
                writer.println(line)
            }
        }
    }

    fun read(f: File): ByteArray {
        val b = ByteArray(f.length().toInt())
        val `in` = FileInputStream(f)
        return `in`.use { iStream ->
            var offset = 0
            while (offset < b.size) {
                val read = iStream.read(b, offset, b.size - offset)
                if (read == -1) throw EOFException()
                offset += read
            }
            b
        }
    }

}