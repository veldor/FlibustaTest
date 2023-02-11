package net.veldor.flibusta_test.model.worker

import android.content.Context
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.connection.CompanionAppSocketClient
import net.veldor.flibusta_test.model.exception.CompatClientSocketClosedException
import net.veldor.flibusta_test.model.handler.CompanionAppHandler
import net.veldor.flibusta_test.model.handler.GrammarHandler
import net.veldor.flibusta_test.model.handler.NotificationHandler
import net.veldor.flibusta_test.model.selection.SocketMultiFile
import net.veldor.flibusta_test.model.util.MyFileReader
import java.io.File
import java.util.*

class SendBookToCompanionWorker(val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val info = createForegroundInfo()
        setForegroundAsync(info)
        val sourceFile = File(inputData.getString(DATA_BOOK)!!)
        val file = MyFileReader.readTempFile(sourceFile)
        val fileName = inputData.getString(DATA_BOOK_NAME)
        try{
            if (fileName != null) {
                if (file.length > CompanionAppSocketClient.MAX_SEND_VIA_SOCKET_SIZE) {
                    val messagesQueue: ArrayList<SocketMultiFile> = arrayListOf()
                    var message: SocketMultiFile
                    val parts = GrammarHandler.explodeFile(
                        file,
                        CompanionAppSocketClient.MAX_SEND_VIA_SOCKET_SIZE
                    )
                    var partIndex = 0
                    val transferId = GrammarHandler.longRandom.toString()
                    parts.forEach {
                        message = SocketMultiFile(
                            "multi_book",
                            fileName,
                            transferId,
                            it,
                            partIndex,
                            parts.size
                        )
                        messagesQueue.add(message)
                        ++partIndex
                    }
                    try {

                        CompanionAppHandler.send(messagesQueue) { state: Boolean, index: Int ->
                            if (state) {
                                setForegroundAsync(
                                    updateForegroundInfo(
                                        String.format(
                                            Locale.ENGLISH,
                                            context.getString(R.string.send_file_part_pattern),
                                            index
                                        ),
                                        messagesQueue.size,
                                        index
                                    )
                                )
                            }
                        }
                    }
                    catch (e: CompatClientSocketClosedException){
                        NotificationHandler.showCompanionTransferErrorNotification(fileName, context.getString(
                            R.string.companion_connection_lost_message))
                        return Result.success()
                    }
                }
                else{
                    CompanionAppHandler.send(file, fileName)
                }
                NotificationHandler.showCompanionTransferFinishedNotification(fileName)
            }
        }
        catch (t: Throwable){
            NotificationHandler.showCompanionTransferErrorNotification(sourceFile.absolutePath, fileName, t.message)
        }
        return Result.success()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        // Build a notification
        val notification = NotificationHandler.createSendToCompanionNotification()
        return ForegroundInfo(NotificationHandler.SEND_TO_COMPANION_NOTIFICATION, notification)
    }

    private fun updateForegroundInfo(message: String, size: Int, done: Int): ForegroundInfo {
        // Build a notification
        val notification =
            NotificationHandler.createSendToCompanionNotification(message, size, done)
        return ForegroundInfo(NotificationHandler.SEND_TO_COMPANION_NOTIFICATION, notification)
    }

    companion object {
        const val TAG = "send book to companion worker"
        const val DATA_BOOK = "data book"
        const val DATA_BOOK_NAME = "data book name"
    }
}