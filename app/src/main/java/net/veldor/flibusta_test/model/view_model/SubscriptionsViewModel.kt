package net.veldor.flibusta_test.model.view_model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.model.handler.DownloadHandler
import net.veldor.flibusta_test.model.handler.DownloadLinkHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.handler.SubscribesHandler
import net.veldor.flibusta_test.model.selections.DownloadLink
import net.veldor.flibusta_test.model.selections.opds.FoundEntity

class SubscriptionsViewModel : ViewModel() {

    fun fullCheckSubscribes() {
        viewModelScope.launch(Dispatchers.IO) {
            SubscribesHandler.instance.checkSubscribes(false)
        }
    }

    fun fastCheckSubscribes() {
        viewModelScope.launch(Dispatchers.IO) {
            PreferencesHandler.instance.lastCheckedForSubscription = SubscribesHandler.instance.checkSubscribes(true)
        }
    }

    fun addToDownloadQueue(selectedLink: DownloadLink?) {
        if (selectedLink != null) {
            DownloadLinkHandler.addDownloadLink(selectedLink)
            if (PreferencesHandler.instance.downloadAutostart) {
                DownloadHandler.instance.startDownload()
            }
        }
    }

    fun cancelCheck() {
        SubscribesHandler.instance.cancelCheck()
    }
}