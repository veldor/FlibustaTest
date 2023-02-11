package net.veldor.tor_client.model.managers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import net.veldor.tor_client.model.bridges.BridgeType
import net.veldor.tor_client.model.bridges.SnowflakeConfigurator
import net.veldor.tor_client.model.control.AndroidOnionProxyContext
import net.veldor.tor_client.model.exceptions.InvalidParsedCaptchaException
import org.jsoup.Jsoup
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.CancellationException
import javax.net.ssl.HttpsURLConnection

class BridgesManager(context: Context) {
    private var bridgesFile: File = File("${context.applicationInfo.dataDir}/bridges/bridges.list")
    private val torConf: ArrayList<String> = ArrayList()

    fun clearBridges(context: Context) {
        saveBridgesToFile(context, arrayListOf())
        reloadTorConfigurationWithBridges(context)
    }

    fun loadTgBridges(context: Context): Boolean {
        val bridgesAnswer = ConnectionManager().directConnect("https://t.me/s/mosty_tor")
        if (bridgesAnswer.statusCode < 400) {
            val reader = BufferedReader(bridgesAnswer.inputStream!!.reader())
            reader.use { read ->
                val bridges = read.readText()
                val parsed = Jsoup.parse(bridges)
                val codeElements = parsed.getElementsByTag("code")
                if (codeElements.isNotEmpty()) {
                    val bridgesText = codeElements.last()?.text()?.replace("obfs4", "\nobfs4")
                    if (bridgesText?.isNotEmpty() == true) {
                        saveCustomBridges(context, bridgesText)
                        return true
                    }
                }
            }
        }
        return false
    }

    fun reloadTorConfigurationWithBridges(context: Context){
        val appDataDir: String = context.applicationInfo.dataDir
        // try to read file
        val configFile = File("$appDataDir/app_data/tor/tor.conf")
        if(!configFile.exists()){
            // install files
            val zipFileManager = ZipFileManager()
            zipFileManager.extractZipFromInputStream(
                context.assets.open("tor.mp3"),
                context.applicationInfo.dataDir
            )
            // fix files paths
            val currentConfiguration =
                StorageManager().readTextFile("${context.applicationInfo.dataDir}/app_data/tor/tor.conf")
            val clearText = ArrayList<String>(arrayListOf())
            currentConfiguration.forEach {
                clearText.add(it.replace("\$path", appDataDir))
            }
            StorageManager().writeToTextFile(
                "$appDataDir/app_data/tor/tor.conf", clearText
            )
        }
        val torConfCleaned = ArrayList<String>()
        val currentConfiguration =
            StorageManager().readTextFile("$appDataDir/app_data/tor/tor.conf")
        if (currentConfiguration.isNotEmpty()) {
            currentConfiguration.forEach {
                if ((it.contains("#") || (!it.contains("Bridge ") && !it.contains("ClientTransportPlugin ") && !it.contains(
                        "UseBridges "
                    ))) && it.isNotEmpty()
                ) {
                    torConfCleaned.add(it)
                }
            }

            val bridgesInUse = getSavedBridges()

            if (bridgesInUse.isNotEmpty()) {
                torConfCleaned.add("UseBridges 1")
                val stringBridges = bridgesInUse.joinToString(" ")
                Log.d("surprise", "BridgesManager: 107 search type in $stringBridges")
                val currentBridgesType = if (stringBridges.contains(BridgeType.obfs4.toString())) {
                    BridgeType.obfs4
                } else if (stringBridges.contains(BridgeType.obfs3.toString())) {
                    BridgeType.obfs3
                } else if (stringBridges.contains(BridgeType.scramblesuit.toString())) {
                    BridgeType.scramblesuit
                } else if (stringBridges.contains(BridgeType.meek_lite.toString())) {
                    BridgeType.meek_lite
                } else if (stringBridges.contains(BridgeType.snowflake.toString())) {
                    BridgeType.snowflake
                } else {
                    BridgeType.vanilla
                }
                Log.d("surprise", "BridgesManager: 119 $currentBridgesType")
                if (currentBridgesType != BridgeType.vanilla) {
                    val clientTransportPlugin: String =
                        if (currentBridgesType == BridgeType.snowflake) {
                            SnowflakeConfigurator(context).getConfiguration()
                        } else {
                            ("ClientTransportPlugin " + currentBridgesType + " exec " + context.applicationInfo.nativeLibraryDir + "/libobfs4proxy.so")
                        }
                    torConfCleaned.add(clientTransportPlugin)
                }
                for (currentBridge in bridgesInUse) {
                    if (currentBridgesType === BridgeType.vanilla) {
                        if (currentBridge.isNotEmpty() && !currentBridge.contains(BridgeType.obfs4.toString()) && !currentBridge.contains(
                                BridgeType.obfs3.toString()
                            ) && !currentBridge.contains(
                                BridgeType.scramblesuit.toString()
                            ) && !currentBridge.contains(BridgeType.meek_lite.toString()) && !currentBridge.contains(
                                BridgeType.snowflake.toString()
                            )
                        ) {
                            torConfCleaned.add("Bridge $currentBridge")
                        }
                    } else {
                        if (currentBridge.isNotEmpty() && currentBridge.contains(currentBridgesType.toString())) {
                            if (currentBridgesType == BridgeType.snowflake) {
                                torConfCleaned.add(
                                    "Bridge " + currentBridge + " utls-imitate=" + SnowflakeConfigurator(
                                        context
                                    ).getUtlsClientID()
                                )
                            } else {
                                torConfCleaned.add("Bridge $currentBridge")
                            }
                        }
                    }
                }
            } else {
                torConfCleaned.add("UseBridges 0")
            }

            if (torConfCleaned.size == torConf.size && torConfCleaned.containsAll(torConf)) {
                Log.d("surprise", "BridgesManager: 189 exit without saving")
                return
            }

            torConfCleaned.forEach {
                Log.d("surprise", "BridgesManager: 191 working conf item $it")
            }

            StorageManager().writeToTextFile(
                "$appDataDir/app_data/tor/tor.conf", torConfCleaned
            )
        }
    }

    fun requestOfficialBridgesCaptcha(): Pair<Bitmap?, String?> {
        val altLink = URL("https://bridges.torproject.org/bridges/?transport=obfs4")
        val connection = altLink.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            connectTimeout = 30000
            readTimeout = 30000
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 6.1; rv:60.0) Gecko/20100101 Firefox/60.0"
            )
            connect()
        }
        return parseCaptchaImage(connection.inputStream)
    }


    @Throws(IOException::class)
    fun parseCaptchaImage(inputStream: InputStream?): Pair<Bitmap?, String?> {
        BufferedReader(InputStreamReader(inputStream)).use { bufferedReader ->
            var codeImage: Bitmap? = null
            val captchaChallengeFieldValue: String
            var inputLine: String
            var imageFound = false
            while (bufferedReader.readLine().also { inputLine = it } != null
                && !Thread.currentThread().isInterrupted
            ) {
                if (inputLine.contains("data:image/jpeg;base64") && !imageFound) {
                    val imgCodeBase64 =
                        inputLine.replace("data:image/jpeg;base64,", "").split("\"".toRegex())
                            .toTypedArray()
                    check(imgCodeBase64.size >= 4) { "Tor Project web site error" }
                    val data =
                        Base64.decode(imgCodeBase64[3], Base64.DEFAULT)
                    codeImage = BitmapFactory.decodeByteArray(data, 0, data.size)
                    imageFound = true
                    checkNotNull(codeImage) {
                        LaunchLogManager.addToLog("Get official captcha: Tor Project website error")
                        "Tor Project web site error"
                    }
                } else if (inputLine.contains("captcha_challenge_field") && inputLine.contains("value")) {
                    val secretCodeArr =
                        inputLine.split("\"".toRegex()).toTypedArray()
                    return if (secretCodeArr.size > 5) {
                        captchaChallengeFieldValue = secretCodeArr[5]
                        Pair(
                            codeImage,
                            captchaChallengeFieldValue
                        )
                    } else {
                        LaunchLogManager.addToLog("Get official captcha: Tor Project website error")
                        throw IllegalStateException("Tor Project website error")
                    }
                }
            }
        }
        LaunchLogManager.addToLog("Get official captcha: Possible Tor Project website data scheme changed")
        throw CancellationException("Possible Tor Project website data scheme changed")
    }


    fun getOfficialBridges(parsedValue: String, secretCode: String, context: Context): Boolean {
        LaunchLogManager.addToLog("Send encoded captcha to TOR server")
        val altLink = URL("https://bridges.torproject.org/bridges/?transport=obfs4")
        val data = linkedMapOf<String, String>().apply {
            put("captcha_challenge_field", secretCode)
            put("captcha_response_field", parsedValue)
            put("submit", "submit")
        }
        val connection = altLink.openConnection() as HttpsURLConnection

        try {
            val query = mapToQuery(data)

            connection.apply {
                requestMethod = "POST"
                setRequestProperty("User-Agent", TOR_BROWSER_USER_AGENT)
                setRequestProperty(
                    "Content-Length",
                    query.toByteArray().size.toString()
                )
                doOutput = true
                connectTimeout = 30000
                readTimeout = 30000
            }.connect()

            connection.outputStream.bufferedWriter().use {
                it.write(query)
                it.flush()
            }

            val response = connection.responseCode
            if (response == HttpURLConnection.HTTP_OK) {
                parseAnswer(connection.inputStream, context)
                return true
            } else {
                throw IOException("HttpsConnectionManager $altLink response code $response")
            }
        } finally {
            connection.disconnect()
        }
    }


    private fun parseAnswer(inputStream: InputStream?, context: Context): Boolean {
        BufferedReader(InputStreamReader(inputStream)).use { bufferedReader ->
            var codeImage: Bitmap?
            var inputLine: String
            var keyWordBridge = false
            var wrongImageCode = false
            var imageFound = false
            val newBridges: MutableList<String> =
                LinkedList()
            val sb = StringBuilder()
            while (bufferedReader.readLine().also { inputLine = it } != null
                && !Thread.currentThread().isInterrupted
            ) {
                if (inputLine.contains("id=\"bridgelines\"") && !wrongImageCode) {
                    keyWordBridge = true
                } else if (inputLine.contains("<br />")
                    && keyWordBridge
                    && !wrongImageCode
                ) {
                    newBridges.add(inputLine.replace("<br />", "").trim { it <= ' ' })
                } else if (inputLine.contains("</div>")
                    && keyWordBridge
                    && !wrongImageCode
                ) {
                    break
                } else if (inputLine.contains("bridgedb-captcha-container")) {
                    wrongImageCode = true
                } else if (wrongImageCode) {
                    if (inputLine.contains("data:image/jpeg;base64") && !imageFound) {
                        val imgCodeBase64 =
                            inputLine.replace("data:image/jpeg;base64,", "").split("\"".toRegex())
                                .toTypedArray()
                        check(imgCodeBase64.size >= 4) { "Tor Project web site error" }
                        val data =
                            Base64.decode(
                                imgCodeBase64[3],
                                Base64.DEFAULT
                            )
                        codeImage = BitmapFactory.decodeByteArray(data, 0, data.size)
                        imageFound = true
                        checkNotNull(codeImage) { "Tor Project web site error" }
                    } else if (inputLine.contains("captcha_challenge_field") && inputLine.contains(
                            "value"
                        )
                    ) {
                        throw InvalidParsedCaptchaException()
                    }
                }
            }
            if (keyWordBridge && !wrongImageCode) {
                for (bridge in newBridges) {
                    sb.append(bridge).append(10.toChar())
                }
                saveCustomBridges(context, sb.toString())
                return true
            } else check(!(!keyWordBridge && !wrongImageCode)) { "Tor Project web site error!" }
        }

        throw CancellationException("Possible Tor Project website data scheme changed")
    }

    private fun mapToQuery(data: Map<String, String>) = data.entries.joinToString("&") {
        "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
    }


    fun saveCustomBridges(context: Context, bridges: String) {
        val bridgesArray = bridges.split("\n")
        val bridgesInUse: ArrayList<String> = arrayListOf()
        bridgesArray.forEach { bridge ->
            var line = bridge
            if (line.isNotEmpty()) {
                if (line.contains(BridgeType.snowflake.toString())) {
                    line = line.replace("utls-imitate.+?( |\\z)".toRegex(), "")
                }
                bridgesInUse.add(line.replace("Bridge ", "").trim { it <= ' ' })
            }
        }
        saveBridgesToFile(context, bridgesInUse)
        // delete existent configuration
        AndroidOnionProxyContext(context).deleteAllFilesButHiddenServices()
        reloadTorConfigurationWithBridges(context)
    }

    companion object {
        var TOR_BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 6.1; rv:60.0) Gecko/20100101 Firefox/60.0"
    }

    private fun saveBridgesToFile(context: Context, bridges: ArrayList<String>) {
        if (bridgesFile.parentFile?.exists() != true) {
            bridgesFile.parentFile?.mkdirs()
        }
        StorageManager().writeToTextFile(bridgesFile.absolutePath, bridges)
    }

    fun getSavedBridges(): List<String> {
        return StorageManager().readTextFile(bridgesFile.path)
    }
}