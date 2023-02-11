package net.veldor.flibusta_test.model.handler

import android.app.DownloadManager
import android.content.Context
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.lifecycle.MutableLiveData
import cz.msebera.android.httpclient.HttpEntity
import cz.msebera.android.httpclient.HttpResponse
import cz.msebera.android.httpclient.client.ResponseHandler
import cz.msebera.android.httpclient.client.methods.HttpGet
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient
import cz.msebera.android.httpclient.impl.client.HttpClients
import cz.msebera.android.httpclient.util.EntityUtils
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.BuildConfig
import net.veldor.flibusta_test.model.receiver.DownloadManagerReceiver
import net.veldor.flibusta_test.model.selection.UpdateInfo
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException


object UpdateHandler {
    private var downloading: Boolean = false
    private var mReceiver: DownloadManagerReceiver? = null
    var updateInfo: UpdateInfo? = null
    val liveCurrentDownloadProgress: MutableLiveData<Int> = MutableLiveData(-1)
    var updateDownloadUri: Uri? = null

    private const val GITHUB_RELEASES_URL =
        "https://api.github.com/repos/veldor/FlibustaTest/releases/latest"
    private const val GITHUB_APP_VERSION = "tag_name"
    private const val GITHUB_DOWNLOAD_LINK = "browser_download_url"


    @JvmStatic
    fun checkUpdate(): Boolean {
        var updateAvailable = false
        val httpclient: CloseableHttpClient = HttpClients.createDefault()
        val httpget = HttpGet(GITHUB_RELEASES_URL)
        try {
            // кастомный обработчик ответов
            val responseHandler: ResponseHandler<String> =
                ResponseHandler<String> { response: HttpResponse ->
                    val status = response.statusLine.statusCode
                    if (status in 200..299) {
                        val entity: HttpEntity = response.entity
                        try {
                            val body: String = EntityUtils.toString(entity)
                            val releaseInfo = JSONObject(body)
                            val lastVersion: String =
                                releaseInfo.getString(GITHUB_APP_VERSION)
                            val currentVersion: Int =
                                if (PreferencesHandler.checkUpdateAfter > 0) {
                                    PreferencesHandler.checkUpdateAfter
                                } else {
                                    BuildConfig.VERSION_CODE
                                }
                            if (lastVersion.toInt() > currentVersion) {
                                updateAvailable = true
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {
                        // неверный ответ с сервера
                        Log.d(
                            "surprise",
                            "CheckUpdateWorker handleResponse: wrong update server answer"
                        )
                    }
                    null
                }
            // выполню запрос
            httpclient.execute(httpget, responseHandler)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                // по-любому закрою клиент
                httpclient.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return updateAvailable
    }

    @JvmStatic
    fun update(updateInfo: UpdateInfo) {
        liveCurrentDownloadProgress.postValue(0)
        UpdateHandler.updateInfo = updateInfo
        val target = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            updateInfo.fileName!!
        )
        if (target.isFile) {
            target.delete()
        }

        val request = DownloadManager.Request(Uri.parse(updateInfo.link))
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setTitle(updateInfo.fileName)
            .setDescription("Load update for Flibusta downloader beta")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                updateInfo.fileName
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            request.setAllowedOverMetered(true)
        }
        val downloadManager =
            App.instance.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadID = downloadManager.enqueue(request)

        mReceiver = DownloadManagerReceiver(downloadManager, downloadID)

        App.instance.registerReceiver(
            mReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )

        Thread {
            downloading = true
            while (downloading) {
                Thread.sleep(500)
                val q = DownloadManager.Query()
                q.setFilterById(downloadID)
                val cursor: Cursor = downloadManager.query(q)
                if (cursor.moveToFirst()) {
                    val index = cursor
                        .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    if(index >= 0){
                        val bytesDownloaded: Int = cursor.getInt(index)
                        liveCurrentDownloadProgress.postValue(bytesDownloaded)
                    }
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if(statusIndex >= 0){
                        if (cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                            downloading = false
                            liveCurrentDownloadProgress.postValue(-1)
                        }
                    }
                }
                cursor.close()
            }
            liveCurrentDownloadProgress.postValue(-1)
        }.start()
    }

    @JvmName("getUpdateInfo1")
    fun getUpdateInfo(): UpdateInfo {
        val httpclient: CloseableHttpClient = HttpClients.createDefault()
        val httpget = HttpGet(GITHUB_RELEASES_URL)
        val info = UpdateInfo()
        try {
            // кастомный обработчик ответов
            val responseHandler: ResponseHandler<String> =
                ResponseHandler<String> { response: HttpResponse ->
                    val status = response.statusLine.statusCode
                    if (status in 200..299) {
                        val entity: HttpEntity = response.entity
                        try {
                            val body: String = EntityUtils.toString(entity)
                            val releaseInfo = JSONObject(body)
                            val name = releaseInfo.getString("name")
                            info.title = name
                            val infoBody = releaseInfo.getString("body")
                            info.body = infoBody
                            val version = releaseInfo.getString("tag_name")
                            info.version = version
                            val releaseAssets: JSONObject =
                                releaseInfo.getJSONArray("assets").getJSONObject(0)
                            val size = releaseAssets.getLong("size")
                            info.size = size
                            val fileName = releaseAssets.getString("name")
                            info.fileName = fileName
                            val downloadLink: String =
                                releaseAssets.getString(GITHUB_DOWNLOAD_LINK)
                            info.link = downloadLink

                        } catch (e: IOException) {
                            e.printStackTrace()
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {
                        // неверный ответ с сервера
                        Log.d(
                            "surprise",
                            "CheckUpdateWorker handleResponse: wrong update server answer"
                        )
                    }
                    null
                }
            // выполню запрос
            httpclient.execute(httpget, responseHandler)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                // по-любому закрою клиент
                httpclient.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return info
    }

    fun ignoreUpdate(updateInfo: UpdateInfo) {
        PreferencesHandler.checkUpdateAfter = updateInfo.version!!.toInt()
    }

    fun cancelUpdate() {
        downloading = false
        if (mReceiver != null) {
            mReceiver?.cancelDownload()
            App.instance.unregisterReceiver(mReceiver)
            mReceiver = null
        }
    }
}