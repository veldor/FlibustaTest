package net.veldor.flibusta_test.model.handler

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.connection.CompanionAppSocketClient
import net.veldor.flibusta_test.model.converter.Fb2ToEpubConverter
import net.veldor.flibusta_test.model.selection.SocketMultiFile
import net.veldor.flibusta_test.model.util.MyFileReader
import net.veldor.flibusta_test.model.worker.SendBookToCompanionWorker
import java.io.File
import java.net.URI

object CompanionAppHandler {
    fun send(context: Context, file: DocumentFile, fileName: String) {
        val coordinates = PreferencesHandler.companionAppCoordinates
        if (coordinates.isNotEmpty()) {
            if(PreferencesHandler.isConvertFb2ForCompanion && file.name?.endsWith(".fb2") == true){
                Log.d("surprise", "CompanionAppHandler: 22 convert fb2 to epub for send")
                val epubFile = Fb2ToEpubConverter().getEpubFile(file)
                val encodedFile = GrammarHandler.toBase64(epubFile)
                sendViaWorker(encodedFile, fileName.replace("fb2", "epub"))
            }
            else{
                val encodedFile = GrammarHandler.toBase64(context, file)
                sendViaWorker(encodedFile, fileName)
            }
        }
    }

    fun send(file: File, fileName: String) {
        val coordinates = PreferencesHandler.companionAppCoordinates
        if (coordinates.isNotEmpty()) {
            if(PreferencesHandler.isConvertFb2ForCompanion && file.name.endsWith(".fb2")){
                Log.d("surprise", "CompanionAppHandler: 22 convert fb2 to epub for send")
                val epubFile = Fb2ToEpubConverter().getEpubFile(file)
                val encodedFile = GrammarHandler.toBase64(epubFile)
                sendViaWorker(encodedFile, fileName.replace("fb2", "epub"))
            }
            else{
                val encodedFile = GrammarHandler.toBase64(file)
                sendViaWorker(encodedFile, fileName)
            }
        }
    }

    internal fun sendViaWorker(encodedFile: String, name: String) {
        Log.d("surprise", "CompanionAppHandler: 36 $name")
        val builder = Data.Builder()
        val tempFile = File.createTempFile("tmp", "data")
        tempFile.deleteOnExit()
        MyFileReader.saveTempFile(tempFile, encodedFile)
        builder.putString(SendBookToCompanionWorker.DATA_BOOK, tempFile.absolutePath)
        builder.putString(SendBookToCompanionWorker.DATA_BOOK_NAME, name)
        val worker = OneTimeWorkRequest.Builder(
            SendBookToCompanionWorker::class.java
        )
            .addTag(SendBookToCompanionWorker.TAG)
            .setInputData(builder.build())
            .build()
        WorkManager.getInstance(App.instance)
            .enqueue(worker)
    }

    fun send(file: String, fileName: String) {
        val coordinates = PreferencesHandler.companionAppCoordinates
        if (coordinates.isNotEmpty()) {
            val client = CompanionAppSocketClient(URI(coordinates))
            client.establishConnection {
                client.sendFile(file, fileName)
                client.close()
            }
        }
    }

    fun send(messagesQueue: ArrayList<SocketMultiFile>, callback: (Boolean, Int) -> Unit) {
        val coordinates = PreferencesHandler.companionAppCoordinates
        if (coordinates.isNotEmpty()) {
            val client = CompanionAppSocketClient(URI(coordinates))
            client.establishConnection {
                messagesQueue.forEach {
                    val status = client.sendMultiFilePart(it)
                    callback(status, it.currentFileIndex + 1)
                }
            }
        }
    }

}