package net.veldor.flibusta_test.model.view_model

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.handler.TorHandler
import net.veldor.flibusta_test.model.listener.ActionListener
import net.veldor.flibusta_test.model.worker.LaunchTorWorker
import net.veldor.tor_client.model.connection.TorClient
import net.veldor.tor_client.model.exceptions.InvalidParsedCaptchaException
import net.veldor.tor_client.model.managers.BridgesManager
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


class TorBridgesViewModel : ViewModel() {
    fun loadCustomBridges(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            listener?.actionLaunched()
            listener?.actionStateUpdated(context.getString(R.string.start_load_bridges))
            try {
                if (BridgesManager(context).loadTgBridges(context)) {
                    listener?.actionFinished(true, context.getString(R.string.bridges_loaded))
                } else {
                    listener?.actionFinished(false, context.getString(R.string.bridges_load_error))
                }
            } catch (t: Throwable) {
                listener?.actionFinished(
                    false,
                    String.format(
                        Locale.ENGLISH,
                        context.getString(R.string.error_template),
                        t.message
                    )
                )
            }
        }
    }

    fun loadOfficialBridgesCaptcha(context: Context, callback: (Pair<Bitmap?, String?>?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            listener?.actionLaunched()
            listener?.actionStateUpdated(context.getString(R.string.request_official_bridges_capcha))
            try {
                val altLink = URL("https://bridges.torproject.org/bridges/?transport=obfs4")
                val connection = altLink.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 30000
                    readTimeout = 30000
                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 6.1; rv:60.0) Gecko/20100101 Firefox/60.0"
                    )
                    connect()
                }
                val result = BridgesManager(context).parseCaptchaImage(connection.inputStream)
                callback(result)
                listener?.actionFinished(true, context.getString(R.string.captcha_loaded))
            } catch (t: Throwable) {
                listener?.actionFinished(
                    false,
                    String.format(
                        Locale.ENGLISH,
                        context.getString(R.string.error_template),
                        t.message
                    )
                )
            }
        }
    }


    fun sendCaptchaAnswer(parsedValue: String, secretCode: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            listener?.actionLaunched()
            listener?.actionStateUpdated(context.getString(R.string.request_official_bridges_title))
            try {
                if (BridgesManager(context).getOfficialBridges(parsedValue, secretCode, context)) {
                    listener?.actionFinished(true, context.getString(R.string.bridges_loaded))
                }
            } catch (e: InvalidParsedCaptchaException) {
                listener?.actionFinished(
                    false,
                    context.getString(R.string.invalid_captcha_code)
                )
            } catch (t: Throwable) {
                listener?.actionFinished(
                    false,
                    String.format(
                        Locale.ENGLISH,
                        context.getString(R.string.error_template),
                        t.message
                    )
                )
                t.printStackTrace()
            }
        }
    }

    fun saveOwnBridges(bridges: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            listener?.actionLaunched()
            BridgesManager(context).saveCustomBridges(context, bridges)
            listener?.actionFinished(true, context.getString(R.string.bridges_loaded))
        }
    }

    fun clearBridges(context: Context, callback: () -> Unit?) {
        viewModelScope.launch(Dispatchers.IO) {
            listener?.actionLaunched()
            val bridgesManager = BridgesManager(context)
            bridgesManager.clearBridges(context)
            listener?.actionFinished(true, context.getString(R.string.tor_bridges_clean_message))
            callback()
        }
    }

    fun requestCurrentBridges(context: Context, callback: (String) -> Unit?) {
        viewModelScope.launch(Dispatchers.IO) {
            listener?.actionLaunched()
            val bridgesManager = BridgesManager(context)
            val currentBridges = bridgesManager.getSavedBridges()
            callback(currentBridges.joinToString("\n"))
            listener?.actionFinished(
                true,
                context.getString(R.string.tor_bridges_list_received_message)
            )
        }
    }

    fun launchTextConnection(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                listener?.actionLaunched()
                listener?.actionStateUpdated(context.getString(R.string.tor_launching_message))
                if (TorHandler.isTorRunning) {
                    listener?.actionStateUpdated(context.getString(R.string.stopping_current_tor_instance_message))
                    LaunchTorWorker.isInterrupted = true
                    TorHandler.stopTor()
                    listener?.actionStateUpdated(context.getString(R.string.current_tor_instance_stopped_message))
                }
                if (TorHandler.launchTor()) {
                    val checkStartTime = System.currentTimeMillis()
                    val intervalForBootstrap = TorHandler.getTorConnectionTime()
                    while (true) {
                        if (TorHandler.liveTorBootstrapped.value == true) {
                            listener?.actionStateUpdated(context.getString(R.string.check_tor_connection_message))
                            val answer =
                                TorClient().rawRequest("http://2gzyxa5ihm7nsggfxnu52rck2vv4rvmdlkiu3zzui5du4xyclen53wid.onion")
                            if (answer.statusCode == 200) {
                                listener?.actionFinished(
                                    true,
                                    context.getString(R.string.tor_connected_message)
                                )
                                return@launch
                            } else {
                                listener?.actionFinished(
                                    false,
                                    context.getString(R.string.tor_connected_message)
                                )
                                return@launch
                            }
                        }
                        if (checkStartTime + intervalForBootstrap < System.currentTimeMillis()) {
                            listener?.actionFinished(
                                false,
                                context.getString(R.string.tor_bootstapped_message)
                            )
                            return@launch
                        }
                        Thread.sleep(1000)
                    }
                } else {
                    listener?.actionFinished(
                        false,
                        context.getString(R.string.establish_control_connection_error_title)
                    )
                }
            } catch (t: Throwable) {
                listener?.actionFinished(
                    false,
                    String.format(
                        Locale.ENGLISH,
                        context.getString(R.string.error_template),
                        t.message
                    )
                )
            }
        }
    }

    var listener: ActionListener? = null
}