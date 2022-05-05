package net.veldor.flibusta_test.model.view_model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.model.handler.TorHandler
import net.veldor.flibusta_test.model.helper.StringHelper
import net.veldor.flibusta_test.model.utils.FlibustaChecker
import net.veldor.flibusta_test.model.web.UniversalWebClient


class StartViewModel : ViewModel() {

    companion object {
        const val RESULT_SUCCESS = 1
        const val RESULT_FAILED = 2
        const val RESULT_WAIT = 0

        const val STAGE_AWAITING = 0
        const val STAGE_FIRST = 1
        const val STAGE_SECOND = 2
        const val STAGE_THIRD = 3
        const val STAGE_READY = 4
    }

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
        _liveStage.postValue(STAGE_FIRST)
        viewModelScope.launch(Dispatchers.IO) {
            TorHandler.instance.launch(context)
        }
    }

    fun checkServer() {
        _liveStage.postValue(STAGE_SECOND)
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

}