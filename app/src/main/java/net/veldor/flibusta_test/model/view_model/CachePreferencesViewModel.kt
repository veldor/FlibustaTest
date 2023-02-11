package net.veldor.flibusta_test.model.view_model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.model.handler.DownloadHandler
import net.veldor.flibusta_test.model.handler.DownloadLinkHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.handler.SubscribesHandler
import net.veldor.flibusta_test.model.selection.DownloadLink
import net.veldor.flibusta_test.model.util.CacheUtils

class CachePreferencesViewModel : ViewModel() {
    fun clearCache(context: Context, callback: () -> Unit?) {
        viewModelScope.launch(Dispatchers.IO) {
            CacheUtils.clearAllCache(context)
            callback()
        }
    }

}