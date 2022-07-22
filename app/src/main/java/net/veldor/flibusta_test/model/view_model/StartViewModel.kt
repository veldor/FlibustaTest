package net.veldor.flibusta_test.model.view_model

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
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
import net.veldor.flibusta_test.model.utils.FlibustaChecker
import net.veldor.flibusta_test.model.web.UniversalWebClient


class StartViewModel : ViewModel() {

    companion object {
        const val RESULT_SUCCESS = 1
        const val RESULT_FAILED = 2

        const val STAGE_AWAITING = 0
        const val STAGE_LAUCH_CLIENT = 1
        const val STAGE_PING_LIBRARY = 2
        const val STAGE_CHECK_LIBRARY_CONNECTION = 3
        const val STAGE_THIRD = 3
        const val STAGE_READY = 4
        const val STATE_LIBRARY_CONNECTION_CHECK_FAILED = 5
        const val STATE_LIBRARY_SERVER_UNAVAILABLE = 6
        const val STATE_LAUNCH_SUCCESSFUL = 7
        const val STATE_TOR_NOT_STARTS = 8
        const val STATE_LIBRARY_SERVER_AVAILABLE = 9
    }

    private var lastLaunchTime: Long = 0
    private var launchConnectionWork: Job? = null
    public val launchState = MutableLiveData(STAGE_AWAITING)

    private var startTorTask: Job? = null
    private var checkServerWork: Job? = null
    private var checkAccessWork: Job? = null

    private val _liveTorWorks: MutableLiveData<Boolean> = MutableLiveData()
    val liveTorWorks: LiveData<Boolean> = _liveTorWorks

    private val _liveStage: MutableLiveData<Int> = MutableLiveData()
    val liveStage: LiveData<Int> = _liveStage

    private val _flibustaServerCheckState = MutableLiveData<Int>().apply {}
    val flibustaServerCheckState: LiveData<Int> = _flibustaServerCheckState

    private val _flibustaCheckState = MutableLiveData<Int>().apply {}
    val flibustaCheckState: LiveData<Int> = _flibustaCheckState

    fun startTor(context: Context) {
        if (startTorTask != null && !startTorTask!!.isCompleted) {
            Log.d("surprise", "startTor: starts yet")
            return
        }
        Log.d("surprise", "startTor: launch tor")
        _liveStage.postValue(STAGE_LAUCH_CLIENT)
        startTorTask = viewModelScope.launch(Dispatchers.IO) {
            TorHandler.instance.launch(context)
        }
    }

    fun checkServer() {
        if (PreferencesHandler.instance.checkServerOnStart) {
            _liveStage.postValue(STAGE_PING_LIBRARY)
            // run check if it's not run
            if (checkServerWork == null || !checkServerWork!!.isActive) {
                checkServerWork = viewModelScope.launch(Dispatchers.IO) {
                    _flibustaServerCheckState.postValue(FlibustaChecker.STATE_RUNNING)
                    try {
                        _flibustaServerCheckState.postValue(FlibustaChecker().isAlive())
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        _flibustaServerCheckState.postValue(FlibustaChecker.STATE_PASSED)
                    }
                }
            }
        } else {
            _flibustaServerCheckState.postValue(FlibustaChecker.STATE_AVAILABLE)
        }
    }

    fun checkTor() {
        viewModelScope.launch(Dispatchers.IO) {
            if (TorHandler.instance.isTorWorks()) {
                _liveTorWorks.postValue(true)
            }
        }
    }

    fun checkFlibustaAvailability() {
        _liveStage.postValue(STAGE_THIRD)
        if (checkAccessWork == null || !checkAccessWork!!.isActive) {
            checkAccessWork = viewModelScope.launch(Dispatchers.IO) {
                val response = UniversalWebClient().rawRequest("/opds", false)
                val answer = StringHelper.streamToString(response.inputStream)
                if (answer != null && answer.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>")) {
                    _flibustaCheckState.postValue(RESULT_SUCCESS)
                } else {
                    _flibustaCheckState.postValue(RESULT_FAILED)
                }
            }
        }
    }

    fun launchConnection(skipLibraryPing: Boolean = false) {
        if (launchConnectionWork == null || launchConnectionWork!!.isCompleted) {
            lastLaunchTime = System.currentTimeMillis()
            launchConnectionWork = viewModelScope.launch(Dispatchers.IO) {
                if(!isActive){
                    Log.d("surprise", "launchConnection: work cancelled")
                    return@launch
                }
                // пропингую библиотеку
                if (PreferencesHandler.instance.checkServerOnStart && !skipLibraryPing) {
                    if(!isActive){
                        Log.d("surprise", "launchConnection: work cancelled")
                        return@launch
                    }
                    launchState.postValue(STAGE_PING_LIBRARY)
                    try {
                        val checkResult = FlibustaChecker().isAlive()
                        if (checkResult != FlibustaChecker.STATE_AVAILABLE) {
                            Log.d("surprise", "launchConnection: oops")
                            if(!isActive){
                                Log.d("surprise", "launchConnection: work cancelled")
                                return@launch
                            }
                            launchState.postValue(STATE_LIBRARY_SERVER_UNAVAILABLE)
                            return@launch
                        } else {
                            if(!isActive){
                                Log.d("surprise", "launchConnection: work cancelled")
                                return@launch
                            }
                            launchState.postValue(STATE_LIBRARY_SERVER_AVAILABLE)
                        }
                    } catch (t: Throwable) {
                        Log.d("surprise", "launchConnection: oops 2")
                        t.printStackTrace()
                        if(!isActive){
                            Log.d("surprise", "launchConnection: work cancelled")
                            return@launch
                        }
                        launchState.postValue(STATE_LIBRARY_SERVER_UNAVAILABLE)
                        return@launch
                    }
                }
                Log.d("surprise", "launchConnection: i am here")
                if (PreferencesHandler.instance.useTor) {
                    Log.d("surprise", "launchConnection: start launch tor")
                    // стартую TOR
                    if(!isActive){
                        Log.d("surprise", "launchConnection: work cancelled")
                        return@launch
                    }
                    launchState.postValue(STAGE_LAUCH_CLIENT)
                    try {
                        TorHandler.instance.start()
                        if (!TorHandler.instance.isTorWorks()) {
                            if(!isActive){
                                Log.d("surprise", "launchConnection: work cancelled")
                                return@launch
                            }
                            launchState.postValue(STATE_TOR_NOT_STARTS)
                            return@launch
                        }
                    } catch (e: Throwable) {
                        if(!isActive){
                            Log.d("surprise", "launchConnection: work cancelled")
                            return@launch
                        }
                        launchState.postValue(STATE_TOR_NOT_STARTS)
                        return@launch
                    }
                } else {
                    Log.d("surprise", "launchConnection: skip tor launch")
                }
                // в завершение- проверю подключение к библиотеке
                if(!isActive){
                    Log.d("surprise", "launchConnection: work cancelled")
                    return@launch
                }
                launchState.postValue(STAGE_CHECK_LIBRARY_CONNECTION)
                val response = UniversalWebClient().rawRequest("/opds", false)
                Log.d("surprise", "launchConnection: ${response.statusCode}")
                val answer = StringHelper.streamToString(response.inputStream)
                Log.d("surprise", "launchConnection: $answer")
                if (answer != null && answer.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>")) {
                    // connected successful
                    if(!isActive){
                        Log.d("surprise", "launchConnection: work cancelled")
                        return@launch
                    }
                    launchState.postValue(STATE_LAUNCH_SUCCESSFUL)
                    return@launch
                }
                if(!isActive){
                    Log.d("surprise", "launchConnection: work cancelled")
                    return@launch
                }
                launchState.postValue(STATE_LIBRARY_CONNECTION_CHECK_FAILED)
            }
        } else {
            Log.d("surprise", "launchConnection: connection in progress yet")
        }
    }

    fun cancelConnection(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            launchConnectionWork?.cancel()
            if (PreferencesHandler.instance.useTor) {
                TorHandler.instance.cancelLaunch(context)
            }
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
        if(lastLaunchTime > 0){
            return (System.currentTimeMillis() - lastLaunchTime).toInt()
        }
        return 0
    }

    fun clearTimeFromLastLaunch() {
        lastLaunchTime = System.currentTimeMillis()
    }
}