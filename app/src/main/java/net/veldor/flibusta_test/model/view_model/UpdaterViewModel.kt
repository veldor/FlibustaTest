package net.veldor.flibusta_test.model.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.model.handler.UpdateHandler
import net.veldor.flibusta_test.model.listener.CheckUpdateListener
import net.veldor.flibusta_test.model.selection.UpdateInfo


class UpdaterViewModel : ViewModel(){
    fun checkUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            try{
                if (UpdateHandler.checkUpdate()) {
                    listener?.haveUpdate(UpdateHandler.getUpdateInfo())
                }
                else{
                    listener?.haveUpdate(null)
                }
            }
            catch (t: Throwable){
                t.printStackTrace()
                listener?.checkError(t.message)
            }
        }
    }

    fun installUpdate(updateInfo: UpdateInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            UpdateHandler.update(updateInfo)
        }
    }

    var listener: CheckUpdateListener? = null
}