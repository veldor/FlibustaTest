package net.veldor.flibusta_test.model.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.model.handler.DownloadHandler
import net.veldor.flibusta_test.model.handler.DownloadLinkHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.handler.SubscribesHandler
import net.veldor.flibusta_test.model.selection.DownloadLink

class SubscriptionsViewModel : ViewModel() {

    fun fullCheckSubscribes() {
        viewModelScope.launch(Dispatchers.IO) {
            SubscribesHandler.checkSubscribes(false)
        }
    }

    fun fastCheckSubscribes() {
        viewModelScope.launch(Dispatchers.IO) {
            PreferencesHandler.lastCheckedForSubscription = SubscribesHandler.checkSubscribes(true)
        }
    }

    fun addToDownloadQueue(selectedLink: DownloadLink?) {
        if (selectedLink != null) {
            DownloadLinkHandler.addDownloadLink(selectedLink)
            if (PreferencesHandler.downloadAutostart) {
                DownloadHandler.startDownload()
            }
        }
    }

    fun cancelCheck() {
        SubscribesHandler.cancelCheck()
    }
}