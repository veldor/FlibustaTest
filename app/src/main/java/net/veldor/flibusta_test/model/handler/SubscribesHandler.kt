package net.veldor.flibusta_test.model.handler

import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.lifecycle.MutableLiveData
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.helper.StringHelper
import net.veldor.flibusta_test.model.parser.OpdsParser
import net.veldor.flibusta_test.model.selections.opds.FoundEntity
import net.veldor.flibusta_test.model.selections.subscribe.*
import net.veldor.flibusta_test.model.web.UniversalWebClient
import java.io.*
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class SubscribesHandler private constructor() {
    private var interrupted: Boolean = false
    val subscribeResults: ArrayList<FoundEntity> = getSerializedResults()
    val inProgress: MutableLiveData<Boolean> = MutableLiveData(false)
    val currentProgress: MutableLiveData<String> = MutableLiveData()
    val foundValue: MutableLiveData<FoundEntity?> = MutableLiveData(null)

    fun checkSubscribes(isIncremental: Boolean): String? {
        var handledBooks = 0
        var subscribeItemsFound = 0
        var nameSubscribesFound = 0
        var authorSubscribesFound = 0
        var genreSubscribesFound = 0
        var sequenceSubscribesFound = 0
        interrupted = false
        subscribeResults.clear()
        if (inProgress.value == true) {
            return null
        }
        if(isIncremental){
            currentProgress.postValue("Проверяю новые поступления")
        }
        else{
            currentProgress.postValue("Полная проверка новых поступлений")
        }
        inProgress.postValue(true)
        val lastCheckedBookId = PreferencesHandler.instance.lastCheckedForSubscription
        var lastBookFound = false
        var lastBookId: String? = null
        val bookSubscribes = SubscribeBooks.instance.getSubscribeList()
        val authorSubscribes = SubscribeAuthors.instance.getSubscribeList()
        val sequenceSubscribes = SubscribeSequences.instance.getSubscribeList()
        val genreSubscribes = SubscribeGenre.instance.getSubscribeList()
        var textValue: String
        var nextPageLink: String? = "/opds/new/0/new"
        while (nextPageLink != null) {
            if(interrupted){
                inProgress.postValue(false)
                return lastBookId
            }
            Log.d("surprise", "checkSubscribes: check next page")
            val response = UniversalWebClient().rawRequest(nextPageLink, false)
            if (response.statusCode == 200 && response.inputStream != null) {
                val answerString = StringHelper.streamToString(response.inputStream)
                if (!answerString.isNullOrEmpty()) {
                    val parser = OpdsParser(answerString)
                    val results = parser.parse()
                    nextPageLink = parser.nextPageLink
                    results.forEach { book ->
                        handledBooks++
                        currentProgress.postValue("Обработано книг: $handledBooks\nНайдено результатов: $subscribeItemsFound\nПо названию книги: $nameSubscribesFound\nПо автору: $authorSubscribesFound\nПо жанру: $genreSubscribesFound\nПо серии: $sequenceSubscribesFound")
                        if(interrupted){
                            inProgress.postValue(false)
                            return lastBookId
                        }
                        if (lastBookFound) {
                            return@forEach
                        }
                        if (lastBookId == null) {
                            lastBookId = book.id!!
                        }
                        textValue = book.name!!.lowercase()
                        bookSubscribes.forEach {
                            val accepted = if (it.name.startsWith("*")) {
                                textValue.contains(it.name.substring(1))
                            } else {
                                textValue == it.name
                            }
                            if (accepted && !book.downloaded) {
                                ++subscribeItemsFound
                                ++nameSubscribesFound
                                currentProgress.postValue("Обработано книг: $handledBooks\nНайдено результатов: $subscribeItemsFound\nПо названию книги: $nameSubscribesFound\nПо автору: $authorSubscribesFound\nПо жанру: $genreSubscribesFound\nПо серии: $sequenceSubscribesFound")
                                book.description = "Подписка по названию книги: ${it.name}"
                                foundValue.postValue(book)
                                subscribeResults.add(0, book)
                                download(book)
                            }
                        }
                        textValue = book.author!!.lowercase()
                        authorSubscribes.forEach {
                            val accepted = if (it.name.startsWith("*")) {
                                textValue.contains(it.name.substring(1))
                            } else {
                                textValue == it.name
                            }
                            if (accepted && !book.downloaded) {
                                ++subscribeItemsFound
                                ++authorSubscribesFound
                                currentProgress.postValue("Обработано книг: $handledBooks\nНайдено результатов: $subscribeItemsFound\nПо названию книги: $nameSubscribesFound\nПо автору: $authorSubscribesFound\nПо жанру: $genreSubscribesFound\nПо серии: $sequenceSubscribesFound")
                                book.description = "Подписка по автору: ${it.name}"
                                foundValue.postValue(book)
                                subscribeResults.add(0, book)
                                download(book)
                            }
                        }
                        textValue = book.sequencesComplex.lowercase()
                        sequenceSubscribes.forEach {
                            val accepted = if (it.name.startsWith("*")) {
                                textValue.contains(it.name.substring(1))
                            } else {
                                textValue == it.name
                            }
                            if (accepted && !book.downloaded) {
                                ++subscribeItemsFound
                                ++sequenceSubscribesFound
                                currentProgress.postValue("Обработано книг: $handledBooks\nНайдено результатов: $subscribeItemsFound\nПо названию книги: $nameSubscribesFound\nПо автору: $authorSubscribesFound\nПо жанру: $genreSubscribesFound\nПо серии: $sequenceSubscribesFound")
                                book.description = "Подписка по серии: ${it.name}"
                                foundValue.postValue(book)
                                subscribeResults.add(book)
                                download(book)
                            }
                        }
                        textValue = book.genreComplex!!.lowercase()
                        genreSubscribes.forEach {
                            val accepted = if (it.name.startsWith("*")) {
                                textValue.contains(it.name.substring(1))
                            } else {
                                textValue == it.name
                            }
                            if (accepted && !book.downloaded) {
                                ++subscribeItemsFound
                                ++genreSubscribesFound
                                currentProgress.postValue("Обработано книг: $handledBooks\nНайдено результатов: $subscribeItemsFound\nПо названию книги: $nameSubscribesFound\nПо автору: $authorSubscribesFound\nПо жанру: $genreSubscribesFound\nПо серии: $sequenceSubscribesFound")
                                book.description = "Подписка по жанру: ${it.name}"
                                foundValue.postValue(book)
                                subscribeResults.add(book)
                                download(book)
                            }
                        }
                        if (isIncremental && lastCheckedBookId == book.id) {
                            Log.d("surprise", "checkSubscribes: scanned for last book")
                            // дальше сканировать не надо
                            nextPageLink = null
                            lastBookFound = true
                            return@forEach

                        }
                    }
                }
            } else {
                break
            }
        }
        serializeResults(subscribeResults)
        if (subscribeResults.size > 0) {
            Log.d("surprise", "checkSubscribes: found ${subscribeResults.size} books by subscribe")
        } else {
            Log.d("surprise", "checkSubscribes: no found books for subscribe")
        }
        inProgress.postValue(false)
        return lastBookId
    }

    private fun serializeResults(subscribeResults: java.util.ArrayList<FoundEntity>) {
        val bos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(bos)
        oos.writeObject(subscribeResults)
        oos.flush()

        val file = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            File(App.instance.dataDir.toString() + "/files/subscribeCheckResults")
        } else {
            File(
                Environment.getDataDirectory()
                    .toString() + "/files/subscribeCheckResults"
            )
        }
        val fos = FileOutputStream(file)
        fos.write(bos.toByteArray())
        fos.flush()
        fos.close()
    }

    @Suppress("UNCHECKED_CAST")
    fun getSerializedResults() :ArrayList<FoundEntity>{
        val file = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            File(App.instance.dataDir.toString() + "/files/subscribeCheckResults")
        } else {
            File(
                Environment.getDataDirectory()
                    .toString() + "/files/subscribeCheckResults"
            )
        }
        if(file.isFile){
            val fis = FileInputStream(file)
            // deserialize
            val bytes = fis.readBytes()
            val bis = ByteArrayInputStream(bytes)
            val ois = ObjectInputStream(bis)
            val o = ois.readObject()
            if(o is ArrayList<*>){
                if(!o.isEmpty()){
                    if(o[0] is FoundEntity){
                        return o as ArrayList<FoundEntity>
                    }
                    return arrayListOf()
                }
            }
        }
        return arrayListOf()
    }

    private fun download(book: FoundEntity) {
        if (PreferencesHandler.instance.autoDownloadSubscriptions) {
            book.downloaded
            val link = book.getFavoriteLink()
            if (link != null) {
                DownloadLinkHandler.addDownloadLink(link)
                if (PreferencesHandler.instance.downloadAutostart) {
                    DownloadHandler.instance.startDownload()
                }
            }
        }
    }

    fun convertToPatterns(targetFile: File) {
        val text = StringBuilder()
        try {
            val br = BufferedReader(FileReader(targetFile))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                text.append(line)
            }
            val dom = SubscribeType.getDocument(br.toString())
            if (dom != null) {
                var values = dom.getElementsByTagName("book")
                var counter = 0
                while (values.item(counter) != null) {
                    values.item(counter).firstChild.nodeValue =
                        "*" + values.item(counter).firstChild.nodeValue
                    ++counter
                }
                values = dom.getElementsByTagName("author")
                counter = 0
                while (values.item(counter) != null) {
                    values.item(counter).firstChild.nodeValue =
                        "*" + values.item(counter).firstChild.nodeValue
                    ++counter
                }
                values = dom.getElementsByTagName("genre")
                counter = 0
                while (values.item(counter) != null) {
                    values.item(counter).firstChild.nodeValue =
                        "*" + values.item(counter).firstChild.nodeValue
                    ++counter
                }
                values = dom.getElementsByTagName("sequence")
                counter = 0
                while (values.item(counter) != null) {
                    values.item(counter).firstChild.nodeValue =
                        "*" + values.item(counter).firstChild.nodeValue
                    ++counter
                }
                val domSource = DOMSource(dom)
                val sWriter = StringWriter()
                val result = StreamResult(sWriter)
                val tf = TransformerFactory.newInstance()
                val transformer: Transformer
                try {
                    transformer = tf.newTransformer()
                    transformer.transform(domSource, result)
                } catch (e: TransformerConfigurationException) {
                    e.printStackTrace()
                } catch (e: TransformerException) {
                    e.printStackTrace()
                }

                try {
                    val writer = FileWriter(targetFile)
                    writer.append(sWriter.toString())
                    writer.flush()
                    writer.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun cancelCheck() {
        interrupted = true
    }

    companion object {
        @kotlin.jvm.JvmStatic
        var instance: SubscribesHandler = SubscribesHandler()
            private set
    }
}