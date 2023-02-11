package net.veldor.flibusta_test.model.handler

import android.os.Build
import android.util.Log
import net.veldor.flibusta_test.model.converter.Fb2ToEpubConverter
import net.veldor.flibusta_test.model.helper.BookActionsHelper
import net.veldor.flibusta_test.model.selection.RootDownloadDir


class SendToKindleHandler {
    fun send(rootDownloadDir: RootDownloadDir) {
        if (rootDownloadDir.destinationFile != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (PreferencesHandler.isConvertFb2ForKindle && rootDownloadDir.destinationFile?.name?.endsWith(
                        ".fb2"
                    ) == true
                ) {
                    val epubFile =
                        Fb2ToEpubConverter().getEpubFile(rootDownloadDir.destinationFile!!)
                    Log.d("surprise", "SendToKindleHandler: 20 send converted fb2 to kindle ${epubFile.name} ${epubFile.length()}")
                    BookActionsHelper.shareBookToKindle(epubFile)
                } else {
                    Log.d("surprise", "SendToKindleHandler: 21 simple load to kindle")
                    BookActionsHelper.shareBookToKindle(rootDownloadDir.destinationFile!!)
                }
            }
        } else if (rootDownloadDir.compatDestinationFile != null) {
            if (PreferencesHandler.isConvertFb2ForKindle && rootDownloadDir.compatDestinationFile?.name?.endsWith(
                    ".fb2"
                ) == true
            ) {
                val epubFile =
                    Fb2ToEpubConverter().getEpubFile(rootDownloadDir.compatDestinationFile!!)
                BookActionsHelper.shareBookToKindle(epubFile)
            } else {
                BookActionsHelper.shareBook(rootDownloadDir.compatDestinationFile!!)
            }
        }
    }
}