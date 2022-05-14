package net.veldor.flibusta_test.model.view_model

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.model.delegate.DownloadTaskAppendedDelegate
import net.veldor.flibusta_test.model.handler.DownloadHandler
import net.veldor.flibusta_test.model.handler.DownloadLinkHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.handler.XMLHandler
import java.io.InputStream


class WebViewViewModel : OpdsViewModel() {

    private var taskAppendedDelegate: DownloadTaskAppendedDelegate? = null

    fun addDownload(link: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // add new download link
            // get info about book
            val downloadLink = DownloadLinkHandler.createDownloadLinkFromHref(link)
            if(downloadLink != null){
                DownloadLinkHandler.addDownloadLink(downloadLink)
                if (PreferencesHandler.instance.downloadAutostart) {
                    DownloadHandler.instance.startDownload()
                }
                taskAppendedDelegate?.taskAppended(downloadLink)
            }
            else{
                taskAppendedDelegate?.taskAppendFailed()
            }
        }
    }

    fun appendDownloadAppendedDelegate(delegate: DownloadTaskAppendedDelegate) {
        taskAppendedDelegate = delegate
    }

    fun removeDownloadAppendedDelegate() {
        taskAppendedDelegate = null
    }

    fun searchLinksInText(textStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = XMLHandler.searchDownloadLinks(textStream)
            taskAppendedDelegate?.booksParsed(result)
        }
    }
}