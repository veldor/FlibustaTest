package net.veldor.flibusta_test.model.view_model

import android.content.Context
import android.net.ConnectivityManager
import android.text.Editable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.handler.*
import net.veldor.flibusta_test.model.helper.StringHelper
import net.veldor.flibusta_test.model.selections.DownloadLink
import net.veldor.flibusta_test.model.selections.web.WebResponse
import net.veldor.flibusta_test.model.utils.FlibustaChecker
import net.veldor.flibusta_test.model.utils.FlibustaChecker.Companion.STATE_AVAILABLE
import net.veldor.flibusta_test.model.utils.FlibustaChecker.Companion.STATE_UNAVAILABLE
import net.veldor.flibusta_test.model.web.UniversalWebClient
import java.net.InetAddress


class ConnectivityGuideViewModel : ViewModel() {
    private lateinit var testConnectionJob: Job
    private lateinit var testLibraryJob: Job
    private lateinit var testAccessJob: Job
    private var initTorJob: Job? = null

    private val _testConnectionState = MutableLiveData<String>()
    val testConnectionState: LiveData<String> = _testConnectionState

    private val _libraryConnectionState = MutableLiveData<String>()
    val libraryConnectionState: LiveData<String> = _libraryConnectionState

    private val _testTorInit = MutableLiveData<String?>()
    val testTorInit: LiveData<String?> = _testTorInit

    fun testInternetConnection() {
        _testConnectionState.postValue(STATE_WAIT)
        testConnectionJob = viewModelScope.launch(Dispatchers.IO) {
            val cm =
                App.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            if (cm == null || cm.activeNetworkInfo == null || !cm.activeNetworkInfo.isConnected) {
                _testConnectionState.postValue(STATE_FAILED)
                return@launch
            }
            val ipAddr: InetAddress = InetAddress.getByName("google.com")
            if (ipAddr.equals("")) {
                _testConnectionState.postValue(STATE_FAILED)
                return@launch
            }
            _testConnectionState.postValue(STATE_PASSED)
        }
    }

    fun testLibraryConnection(url: String) {
        testLibraryJob = viewModelScope.launch(Dispatchers.IO) {
            _libraryConnectionState.postValue(STATE_WAIT)
            when (FlibustaChecker().isAlive(url)) {
                STATE_AVAILABLE -> {
                    _libraryConnectionState.postValue(STATE_PASSED)
                }
                STATE_UNAVAILABLE -> {
                    _libraryConnectionState.postValue(STATE_FAILED)
                }
                else -> {
                    _libraryConnectionState.postValue(STATE_CHECK_ERROR)
                }
            }
        }
    }

    fun finallyTestConnection(text: Editable?) {
        testAccessJob = viewModelScope.launch(Dispatchers.IO) {
            _libraryConnectionState.postValue(STATE_WAIT)
            try {
                val response: WebResponse = if (text.toString().isNotEmpty()) {
                    val customMirrorUrl = text.toString()
                    Log.d(
                        "surprise",
                        "ConnectivityGuideViewModel.kt 89 finallyTestConnection mirror is $customMirrorUrl"
                    )
                    if (GrammarHandler.isValidUrl(customMirrorUrl)) {
                        // test with this mirror
                        UniversalWebClient().rawRequest(customMirrorUrl, "/opds", false)
                    } else {
                        UniversalWebClient().rawRequest("/opds", false)
                    }
                } else {
                    UniversalWebClient().rawRequest("/opds", false)
                }
                val answer = StringHelper.streamToString(response.inputStream)
                if (answer.isNullOrEmpty() || !answer.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>")) {
                    _libraryConnectionState.postValue(STATE_FAILED)
                } else {
                    _libraryConnectionState.postValue(STATE_PASSED)
                    if (!text.isNullOrEmpty()) {
                        Log.d(
                            "surprise",
                            "ConnectivityGuideViewModel.kt 106 finallyTestConnection saving custom mirror $text"
                        )
                        // save it as custom mirror
                        PreferencesHandler.instance.customMirror = text.toString()
                        PreferencesHandler.instance.isCustomMirror = true
                    }
                }
            } catch (e: Exception) {
                _libraryConnectionState.postValue(STATE_CHECK_ERROR)
            }
        }
    }

    fun initTor() {
        initTorJob = viewModelScope.launch(Dispatchers.IO) {
            _testTorInit.postValue(STATE_WAIT)
            TorHandler.instance.start()
            if (TorHandler.instance.isTorWorks()) {
                _testTorInit.postValue(STATE_PASSED)
            } else {
                _testTorInit.postValue(STATE_FAILED)
            }
        }
    }

    fun cancelTorLaunch(context: Context) {
        try{
            TorHandler.instance.cancelLaunch(context)
        }
        catch (t: Throwable){
            t.printStackTrace()
        }
        initTorJob?.cancel()
        _testTorInit.value = null
    }

    fun loadTorBridges() {
        viewModelScope.launch(Dispatchers.IO) {
            BridgesHandler().getBridges()
        }
    }

    fun saveBridges(text: Editable?) {
        if (text != null && text.isNotEmpty()) {
            BridgesHandler().saveBridgesToFile(text.toString())
        }
    }

    fun downloadTestBook() {
        val downloadLink = DownloadLink()
        downloadLink.id = "229751"
        downloadLink.author = "Оруэлл Джордж"
        downloadLink.authorDirName = "Оруэлл Джордж"
        downloadLink.name = "1984 [ru]"
        downloadLink.size = "Размер: 948 Kb"
        downloadLink.mime = "application/fb2+zip"
        downloadLink.reservedSequenceName = ""
        downloadLink.sequenceDirName = "1984  ru версии"
        downloadLink.url = "/b/229751/fb2"
        DownloadLinkHandler.addDownloadLink(downloadLink)
        if (PreferencesHandler.instance.downloadAutostart) {
            DownloadHandler.instance.startDownload()
        }
    }

    companion object {
        const val STATE_WAIT = "wait"
        const val STATE_PASSED = "passed"
        const val STATE_FAILED = "failed"
        const val STATE_CHECK_ERROR = "check error"
    }
}