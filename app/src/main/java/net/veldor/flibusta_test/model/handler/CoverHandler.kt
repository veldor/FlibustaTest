package net.veldor.flibusta_test.model.handler

import android.graphics.Bitmap
import android.util.Log
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.connection.Connector
import net.veldor.flibusta_test.model.selection.FoundEntity
import net.veldor.tor_client.model.managers.ConnectionManager
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.SocketTimeoutException
import kotlin.concurrent.thread


class CoverHandler {

    companion object {
        fun dropPreviousLoading() {
            queue = arrayListOf()
        }

        var queue = ArrayList<FoundEntity>()
        var loadInProgress = false
    }

    fun loadPic(foundedEntity: FoundEntity) {
        // put image to load queue
        queue.add(foundedEntity)
        initLoad()
    }

    private fun initLoad() {
        if (!loadInProgress) {
            loadInProgress = true
            loadPics()
        }
    }

    private fun loadPics() {
        thread {
            var foundedEntity: FoundEntity
            while (queue.isNotEmpty()) {
                try {
                    foundedEntity = queue.removeAt(0)
                    downloadPic(foundedEntity)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            loadInProgress = false
        }
    }

    private fun downloadPic(
        foundedEntity: FoundEntity
    ) {
        if (foundedEntity.cover != null) {
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = Connector().rawRequest(foundedEntity.coverUrl!!, false)
                val status = response.statusCode
                if (status < 400) {
                    var tempFile: File? = null
                    if (response.headers["Content-Type"]?.contains("image/jpeg") == true) {
                        tempFile =
                            File.createTempFile(GrammarHandler.longRandom.toString(), "jpg")
                        tempFile.deleteOnExit()
                        val out: OutputStream = FileOutputStream(tempFile)
                        var read: Int
                        val buffer = ByteArray(1024)
                        while (response.inputStream!!.read(buffer)
                                .also { read = it } > 0
                        ) {
                            out.write(buffer, 0, read)
                        }
                        out.close()
                        response.inputStream?.close()
                    } else if (response.headers["Content-Type"]?.contains("image/png") == true) {
                        tempFile =
                            File.createTempFile(GrammarHandler.longRandom.toString(), "png")
                        tempFile.deleteOnExit()
                        val out: OutputStream = FileOutputStream(tempFile)
                        var read: Int
                        val buffer = ByteArray(1024)
                        while (response.inputStream!!.read(buffer)
                                .also { read = it } > 0
                        ) {
                            out.write(buffer, 0, read)
                        }
                        out.close()
                        response.inputStream?.close()
                    }

                    if (tempFile != null && tempFile.isFile && tempFile.exists() && tempFile.canRead() && tempFile.length() > 0) {
                        val compressedImageFile = Compressor.compress(App.instance, tempFile) {
                            resolution(100, 143)
                            quality(80)
                            format(Bitmap.CompressFormat.JPEG)
                        }
                        foundedEntity.cover = compressedImageFile
                        tempFile.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("surprise", "PicHandler.kt 127 downloadPic error load pic")
            }
        }
    }

    fun downloadFullPic(item: FoundEntity) {
        val response = Connector().rawRequest(item.coverUrl!!, false)
        val status = response.statusCode
        if (status < 400) {
            try {
                var tempFile: File? = null
                if (response.headers["Content-Type"]?.contains("image/jpeg") == true) {

                    tempFile = File.createTempFile(GrammarHandler.longRandom.toString(), "jpg")
                    tempFile.deleteOnExit()
                    val out: OutputStream = FileOutputStream(tempFile)
                    var read: Int
                    val buffer = ByteArray(1024)
                    while (response.inputStream!!.read(buffer).also { read = it } > 0) {
                        out.write(buffer, 0, read)
                    }
                    out.close()
                    response.inputStream?.close()
                } else if (response.headers["Content-Type"]?.contains("image/png") == true) {

                    tempFile = File.createTempFile(GrammarHandler.longRandom.toString(), "png")
                    tempFile.deleteOnExit()
                    val out: OutputStream = FileOutputStream(tempFile)
                    var read: Int
                    val buffer = ByteArray(1024)
                    while (response.inputStream!!.read(buffer).also { read = it } > 0) {
                        out.write(buffer, 0, read)
                    }
                    out.close()
                    response.inputStream?.close()
                }
                if (tempFile != null && tempFile.isFile && tempFile.length() > 0) {
                    item.cover = tempFile
                }
            } catch (_: SocketTimeoutException) {
            }
        }
    }
}