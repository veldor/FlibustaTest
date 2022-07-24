package net.veldor.flibusta_test.model.view_model

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.handler.TorHandler
import net.veldor.flibusta_test.model.helper.StringHelper
import net.veldor.flibusta_test.model.selections.UpdateInfo
import net.veldor.flibusta_test.model.utils.FlibustaChecker
import net.veldor.flibusta_test.model.utils.Updater
import net.veldor.flibusta_test.model.web.UniversalWebClient


open class StartViewModel : ViewModel() {

    companion object {

        const val STAGE_AWAITING = 0
        const val STAGE_LAUNCH_CLIENT = 1
        const val STAGE_PING_LIBRARY = 2
        const val STAGE_CHECK_LIBRARY_CONNECTION = 3
        const val STATE_LIBRARY_CONNECTION_CHECK_FAILED = 5
        const val STATE_LIBRARY_SERVER_UNAVAILABLE = 6
        const val STATE_LAUNCH_SUCCESSFUL = 7
        const val STATE_TOR_NOT_STARTS = 8
        const val STATE_LIBRARY_SERVER_AVAILABLE = 9

        const val STATE_UPDATE_CHECK_AWAITING = 0
        const val STATE_UPDATE_CHECK_IN_PROGRESS = 1
        const val STATE_UPDATE_AVAILABLE = 2
        const val STATE_UPDATE_NOT_REQUIRED = 3
        const val STATE_UPDATE_CHECK_FAILED = 4
    }

    private var updateInfo: UpdateInfo? = null
    private var lastLaunchTime: Long = 0
    private var launchConnectionWork: Job? = null
    val launchState = MutableLiveData(STAGE_AWAITING)
    val updateState = MutableLiveData(STATE_UPDATE_CHECK_AWAITING)


    fun launchConnection(skipLibraryPing: Boolean = false) {
        if (launchConnectionWork == null || launchConnectionWork!!.isCompleted) {
            lastLaunchTime = System.currentTimeMillis()
            launchConnectionWork = viewModelScope.launch(Dispatchers.IO) {
                if (!isActive) {
                    Log.d("surprise", "launchConnection: work cancelled")
                    return@launch
                }
                // пропингую библиотеку
                if (PreferencesHandler.instance.checkServerOnStart && !skipLibraryPing) {
                    if (!isActive) {
                        Log.d("surprise", "launchConnection: work cancelled")
                        return@launch
                    }
                    launchState.postValue(STAGE_PING_LIBRARY)
                    try {
                        val checkResult = FlibustaChecker().isAlive()
                        if (checkResult != FlibustaChecker.STATE_AVAILABLE) {
                            Log.d("surprise", "launchConnection: oops")
                            if (!isActive) {
                                Log.d("surprise", "launchConnection: work cancelled")
                                return@launch
                            }
                            launchState.postValue(STATE_LIBRARY_SERVER_UNAVAILABLE)
                            return@launch
                        } else {
                            if (!isActive) {
                                Log.d("surprise", "launchConnection: work cancelled")
                                return@launch
                            }
                            launchState.postValue(STATE_LIBRARY_SERVER_AVAILABLE)
                        }
                    } catch (t: Throwable) {
                        Log.d("surprise", "launchConnection: oops 2")
                        t.printStackTrace()
                        if (!isActive) {
                            Log.d("surprise", "launchConnection: work cancelled")
                            return@launch
                        }
                        launchState.postValue(STATE_LIBRARY_SERVER_UNAVAILABLE)
                        return@launch
                    }
                }
                if (PreferencesHandler.instance.useTor) {
                    // стартую TOR
                    if (!isActive) {
                        return@launch
                    }
                    launchState.postValue(STAGE_LAUNCH_CLIENT)
                    try {
                        TorHandler.instance.start()
                        if (!TorHandler.instance.isTorWorks()) {
                            if (!isActive) {
                                return@launch
                            }
                            launchState.postValue(STATE_TOR_NOT_STARTS)
                            return@launch
                        }
                    } catch (e: Throwable) {
                        if (!isActive) {
                            return@launch
                        }
                        launchState.postValue(STATE_TOR_NOT_STARTS)
                        return@launch
                    }
                }
                // в завершение- проверю подключение к библиотеке
                if (!isActive) {
                    return@launch
                }
                launchState.postValue(STAGE_CHECK_LIBRARY_CONNECTION)
                val response = UniversalWebClient().rawRequest("/opds", false)
                Log.d("surprise", "launchConnection: ${response.statusCode}")
                val answer = StringHelper.streamToString(response.inputStream)
                if (answer != null && answer.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>")) {
                    // connected successful
                    if (!isActive) {
                        return@launch
                    }
                    launchState.postValue(STATE_LAUNCH_SUCCESSFUL)
                    return@launch
                }
                if (!isActive) {
                    return@launch
                }
                launchState.postValue(STATE_LIBRARY_CONNECTION_CHECK_FAILED)
            }
        } else {
            Log.d("surprise", "launchConnection: connection in progress yet")
        }
    }

    fun relaunchConnection(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            launchConnectionWork?.cancel()
            launchConnectionWork = null
            try {
                TorHandler.instance.cancelLaunch(context)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            launchConnection()
        }
    }

    fun getTimeFromLastLaunch(): Int {
        if (lastLaunchTime > 0) {
            return (System.currentTimeMillis() - lastLaunchTime).toInt()
        }
        return 0
    }

    fun clearTimeFromLastLaunch() {
        lastLaunchTime = System.currentTimeMillis()
    }

    fun checkForUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            updateState.postValue(STATE_UPDATE_CHECK_IN_PROGRESS)
            try{
                if (Updater.checkUpdate()) {
                    updateInfo = Updater.getUpdateInfo()
                    updateState.postValue(STATE_UPDATE_AVAILABLE)
                } else {
                    updateState.postValue(STATE_UPDATE_NOT_REQUIRED)
                }
            }
            catch (t: Throwable){
                updateState.postValue(STATE_UPDATE_CHECK_FAILED)
            }
        }
    }

    fun getUpdateInfo(): UpdateInfo? {
        return updateInfo
    }

    fun ignoreUpdate(updateInfo: UpdateInfo) {
        Updater.ignoreUpdate(updateInfo)
    }

    fun getUpdate(updateInfo: UpdateInfo, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            Updater.update(updateInfo)
        }
    }
}