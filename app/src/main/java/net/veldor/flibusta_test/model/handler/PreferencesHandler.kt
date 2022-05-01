package net.veldor.flibusta_test.model.handler

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.helper.UrlHelper
import net.veldor.flibusta_test.ui.browser_fragments.WebViewFragment
import java.io.File

class PreferencesHandler private constructor() {

    var browserViewMode: Int
        get() = preferences.getInt(PREF_BROWSER_VIEW_MODE, WebViewFragment.VIEW_MODE_LIGHT)
        set(state) {
            preferences.edit().putInt(PREF_BROWSER_VIEW_MODE, state).apply()
        }

    var lastWebViewLink: String
        get() = preferences.getString(
            PREF_LAST_WEBVIEW_LINK,
            ""
        )!!
        set(url) {
            if (!url.contains("/favicon.ico") && !url.contains("/sites/default/files/bluebreeze_favicon.ico")) preferences.edit()
                .putString(
                    PREF_LAST_WEBVIEW_LINK, url.replace(UrlHelper.getBaseUrl(), "")
                ).apply()
        }

    fun getDownloadDirLocation(): String? {
        val dir = getDownloadDir()
        if (dir != null && dir.isDirectory) {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                UrlHelper.getPath(App.instance, dir.uri)
            } else {
                dir.uri.path
            }
        }
        val compatDir = getCompatDownloadDir()
        return if (compatDir != null && compatDir.isDirectory) {
            compatDir.absolutePath
        } else "Не распознал папку загрузок"
    }

    var strictDownloadFormat: Boolean
        get() = preferences.getBoolean(PREF_STRICT_FORMAT, false)
        set(state) {
            preferences.edit().putBoolean(PREF_STRICT_FORMAT, state).apply()
        }

    var unzipLoaded: Boolean
        get() = preferences.getBoolean(PREF_UNZIP_LOADED, false)
        set(state) {
            preferences.edit().putBoolean(PREF_UNZIP_LOADED, state).apply()
        }

    var showDownloadProgress: Boolean
        get() = preferences.getBoolean(PREF_SHOW_DOWNLOAD_PROGRESS, false)
        set(state) {
            preferences.edit().putBoolean(PREF_SHOW_DOWNLOAD_PROGRESS, state).apply()
        }

    var rememberFavoriteFormat: Boolean
        get() = preferences.getBoolean(PREF_REMEMBER_FAVORITE_FORMAT, false)
        set(state) {
            preferences.edit().putBoolean(PREF_REMEMBER_FAVORITE_FORMAT, state).apply()
        }

    var downloadAutostart: Boolean
        get() = preferences.getBoolean(PREF_DOWNLOAD_AUTOSTART, true)
        set(state) {
            preferences.edit().putBoolean(PREF_DOWNLOAD_AUTOSTART, state).apply()
        }

    var createSequenceDir: Boolean
        get() = preferences.getBoolean(PREF_CREATE_SEQUENCE_DIR, false)
        set(state) {
            preferences.edit().putBoolean(PREF_CREATE_SEQUENCE_DIR, state).apply()
        }

    var createAuthorDir: Boolean
        get() = preferences.getBoolean(PREF_CREATE_AUTHOR_DIR, false)
        set(state) {
            preferences.edit().putBoolean(PREF_CREATE_AUTHOR_DIR, state).apply()
        }

    var createDifferentDirs: Boolean
        get() = preferences.getBoolean(PREF_CREATE_DIFFERENT_DIRS, false)
        set(state) {
            preferences.edit().putBoolean(PREF_CREATE_DIFFERENT_DIRS, state).apply()
        }

    var sequencesInAuthorDir: Boolean
        get() = preferences.getBoolean(PREF_SEQUENCES_IN_AUTHORS_DIR, false)
        set(state) {
            preferences.edit().putBoolean(PREF_SEQUENCES_IN_AUTHORS_DIR, state).apply()
        }

    var hideOpdsResultsButtons: Boolean
        get() = preferences.getBoolean(PREF_HIDE_OPDS_SEARCH_RESULTS_BUTTONS, false)
        set(state) {
            preferences.edit().putBoolean(PREF_HIDE_OPDS_SEARCH_RESULTS_BUTTONS, state).apply()
        }

    var isAuthorInBookName: Boolean
        get() = preferences.getBoolean(PREF_AUTHOR_IN_BOOK_NAME, true)
        set(state) {
            preferences.edit().putBoolean(PREF_AUTHOR_IN_BOOK_NAME, state).apply()
        }

    var isSequenceInBookName: Boolean
        get() = preferences.getBoolean(PREF_SEQUENCE_IN_BOOK_NAME, true)
        set(state) {
            preferences.edit().putBoolean(PREF_SEQUENCE_IN_BOOK_NAME, state).apply()
        }

    var nightMode: String
        get() = preferences.getString(PREF_NIGHT_THEME, NIGHT_THEME_SYSTEM)!!
        set(state) {
            preferences.edit().putString(PREF_NIGHT_THEME, state).apply()
        }
    var favoriteFormat: String?
        get() = preferences.getString(PREF_FAVORITE_FORMAT, null)
        set(state) {
            Log.d("surprise", "saving favorite: $state")
            preferences.edit().putString(PREF_FAVORITE_FORMAT, state).apply()
        }

    var isLightOpdsAdapter: Boolean
        get() = preferences.getBoolean(PREF_LIGHT_OPDS_ADAPTER, false)
        set(state) {
            preferences.edit().putBoolean(PREF_LIGHT_OPDS_ADAPTER, state).apply()
        }
    var isDisplayPagerButton: Boolean
        get() = preferences.getBoolean(PREF_DISPLAY_PAGER_BUTTON, false)
        set(state) {
            preferences.edit().putBoolean(PREF_DISPLAY_PAGER_BUTTON, state).apply()
        }

    var isHideDigests: Boolean
        get() = preferences.getBoolean(HIDE_DIGESTS_PREF, false)
        set(state) {
            preferences.edit().putBoolean(HIDE_DIGESTS_PREF, state).apply()
        }

    var isHideRead: Boolean
        get() = preferences.getBoolean(PREF_HIDE_READ, false)
        set(state) {
            preferences.edit().putBoolean(PREF_HIDE_READ, state).apply()
        }

    var isHideDownloaded: Boolean
        get() = preferences.getBoolean(HIDE_DOWNLOADED_PREF, false)
        set(state) {
            preferences.edit().putBoolean(HIDE_DOWNLOADED_PREF, state).apply()
        }

    var isOnlyRussian: Boolean
        get() = preferences.getBoolean(PREF_ONLY_RUSSIAN, false)
        set(state) {
            preferences.edit().putBoolean(PREF_ONLY_RUSSIAN, state).apply()
        }

    fun bookNameStrictFilter(): Boolean {
        return preferences.getBoolean(PREF_BOOK_NAME_STRICT_FILTER, false)
    }

    fun bookAuthorStrictFilter(): Boolean {
        return preferences.getBoolean(PREF_BOOK_AUTHOR_STRICT_FILTER, false)
    }

    fun bookGenreStrictFilter(): Boolean {
        return preferences.getBoolean(PREF_BOOK_GENRE_STRICT_FILTER, false)
    }

    fun bookSequenceStrictFilter(): Boolean {
        return preferences.getBoolean(PREF_BOOK_SEQUENCE_STRICT_FILTER, false)
    }

    fun sequenceStrictFilter(): Boolean {
        return preferences.getBoolean(PREF_SEQUENCE_STRICT_FILTER, false)
    }

    fun authorStrictFilter(): Boolean {
        return preferences.getBoolean(PREF_AUTHOR_STRICT_FILTER, false)
    }

    fun genreStrictFilter(): Boolean {
        return preferences.getBoolean(PREF_GENRE_STRICT_FILTER, false)
    }

    var isOpdsUseFilter: Boolean
        get() = preferences.getBoolean(PREF_OPDS_USE_FILTER, false)
        set(state) {
            preferences.edit().putBoolean(PREF_OPDS_USE_FILTER, state).apply()
        }

    var showCovers: Boolean
        get() {
            return preferences.getBoolean(PREF_SHOW_COVERS, true)
        }
        set(state) {
            preferences.edit().putBoolean(PREF_SHOW_COVERS, state).apply()
        }

    var opdsPagingType: Boolean
        get() {
            return preferences.getBoolean(PREF_OPDS_SHOW_PAGING, true)
        }
        set(state) {
            preferences.edit().putBoolean(PREF_OPDS_SHOW_PAGING, state).apply()
        }

    val appVersion: String
        get() {
            try {
                val pInfo = App.instance.packageManager.getPackageInfo(
                    App.instance.packageName,
                    0
                )
                return pInfo.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                Log.d("surprise", "MainActivity setupUI: can't found version")
                e.printStackTrace()
            }
            return "0"
        }

    fun setDownloadDir(file: DocumentFile?): Boolean {
        if (file != null && file.isDirectory && file.canWrite()) {
            preferences.edit().putString(PREF_DOWNLOAD_LOCATION, file.uri.toString()).apply()
            val f = getDownloadDir()
            return f != null && f.isDirectory && f.canWrite()
        }
        return false
    }

    fun setDownloadDir(folderLocation: String?): Boolean {
        // ещё раз попробую создать файл
        val file = File(folderLocation)
        if (file.isDirectory) {
            preferences.edit().putString(PREF_DOWNLOAD_LOCATION, folderLocation)
                .apply()
            val f = getCompatDownloadDir()
            return f != null && f.isDirectory && f.canWrite()
        }
        return false
    }

    private fun getCompatDownloadDir(): File? {
        var dd: File? = null
        val downloadLocation = preferences.getString(PREF_DOWNLOAD_LOCATION, null)
        if (downloadLocation != null) {
            dd = File(downloadLocation)
        }
        return dd
    }

    fun getDownloadDir(): DocumentFile? {
        var dl: DocumentFile? = null
        // возвращу папку для закачек
        val downloadLocation =
            preferences.getString(PREF_DOWNLOAD_LOCATION, null)
        if (downloadLocation != null) {
            try {
                dl = DocumentFile.fromTreeUri(App.instance, Uri.parse(downloadLocation))
            } catch (e: Exception) {
            }
        }
        // верну путь к папке загрузок
        return dl
    }

    var firstUse: Boolean
        get() = preferences.getBoolean(PREF_FIRST_USE, true)
        set(state) {
            preferences.edit().putBoolean(PREF_FIRST_USE, state).apply()
        }


    private var preferences: SharedPreferences =
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(App.instance)

    var useTor: Boolean
        get() = preferences.getBoolean(PREF_USE_TOR, true)
        set(state) {
            preferences.edit().putBoolean(PREF_USE_TOR, state).apply()
        }

    var showConnectionOptions: Boolean
        get() = preferences.getBoolean(PREF_SHOW_CONNECTION_OPTIONS, true)
        set(state) {
            preferences.edit().putBoolean(PREF_SHOW_CONNECTION_OPTIONS, state).apply()
        }

    var hardwareAcceleration: Boolean
        get() = preferences.getBoolean(HW_ACCELERATION_PREF, true)
        set(state) {
            preferences.edit().putBoolean(HW_ACCELERATION_PREF, state)
                .apply()
        }

    var isEInk: Boolean
        get() = preferences.getBoolean(
            EINK_PREF,
            false
        )
        set(state) {
            preferences.edit().putBoolean(EINK_PREF, state)
                .apply()
        }

    fun isPicHide(): Boolean {
        return preferences.getBoolean(HIDE_PICS_PREF, false)
    }

    fun saveDownloadFolder(folderLocation: String?): Boolean {
        return true
    }

    var isCustomMirror: Boolean
        get() = preferences.getBoolean(
            IS_CUSTOM_MIRROR_PREF,
            false
        )
        set(state) {
            preferences.edit()
                .putBoolean(IS_CUSTOM_MIRROR_PREF, state)
                .apply()
        }

    var customMirror: String
        get() = preferences.getString(
            CUSTOM_MIRROR_PREF,
            BASE_URL
        )!!
        set(state) {
            preferences.edit()
                .putString(CUSTOM_MIRROR_PREF, state)
                .apply()
        }

    var authCookie: String?
        get() = preferences.getString(AUTH_COOKIE_VALUE_PREF, null)
        set(value) {
            if (value == null) {
                preferences.edit().remove(AUTH_COOKIE_VALUE_PREF).apply()
            } else {
                preferences.edit().putString(AUTH_COOKIE_VALUE_PREF, value).apply()
            }
        }

    companion object {
        const val BASE_URL = "http://flibusta.is"

        const val PREF_DOWNLOAD_LOCATION = "download_location"
        private const val PREF_USE_TOR = "use tor"
        private const val PREF_FIRST_USE = "first use"
        private const val PREF_SHOW_CONNECTION_OPTIONS = "show connection options"
        private const val PREF_OPDS_SHOW_PAGING = "opds show paging"
        private const val PREF_SHOW_COVERS = "show covers"

        private const val HW_ACCELERATION_PREF = "hardware acceleration"
        private const val EINK_PREF = "is eInk"
        private const val HIDE_PICS_PREF = "hide pics"
        private const val IS_CUSTOM_MIRROR_PREF = "is custom mirror"
        private const val CUSTOM_MIRROR_PREF = "custom mirror"
        private const val AUTH_COOKIE_VALUE_PREF = "auth cookie value"

        private const val HIDE_DIGESTS_PREF = "hide digests"
        private const val HIDE_DOWNLOADED_PREF = "hide downloaded"
        private const val PREF_OPDS_USE_FILTER = "use filter"
        private const val PREF_ONLY_RUSSIAN = "only russian"
        private const val PREF_BOOK_NAME_STRICT_FILTER = "strict name in books"
        private const val PREF_BOOK_AUTHOR_STRICT_FILTER = "strict author in books"
        private const val PREF_BOOK_GENRE_STRICT_FILTER = "strict genre in books"
        private const val PREF_BOOK_SEQUENCE_STRICT_FILTER = "strict sequence in books"
        private const val PREF_SEQUENCE_STRICT_FILTER = "strict sequence filter"
        private const val PREF_GENRE_STRICT_FILTER = "strict genre filter"
        private const val PREF_AUTHOR_STRICT_FILTER = "strict author filter"
        private const val PREF_HIDE_READ = "hide read"
        private const val PREF_LIGHT_OPDS_ADAPTER = "opds light adapter"
        private const val PREF_DISPLAY_PAGER_BUTTON = "display pager button"
        private const val PREF_HIDE_OPDS_SEARCH_RESULTS_BUTTONS = "no item buttons"
        private const val PREF_AUTHOR_IN_BOOK_NAME = "author in book name"
        private const val PREF_SEQUENCE_IN_BOOK_NAME = "sequence in book name"
        private const val PREF_CREATE_AUTHOR_DIR = "create author dir"
        private const val PREF_CREATE_SEQUENCE_DIR = "create sequence dir"
        private const val PREF_CREATE_DIFFERENT_DIRS = "different dirs"
        private const val PREF_SEQUENCES_IN_AUTHORS_DIR = "load sequences to author dir"
        private const val PREF_REMEMBER_FAVORITE_FORMAT = "remember favorite format"
        private const val PREF_FAVORITE_FORMAT = "favorite format"
        private const val PREF_STRICT_FORMAT = "strict format"
        private const val PREF_DOWNLOAD_AUTOSTART = "download auto start"
        private const val PREF_SHOW_DOWNLOAD_PROGRESS = "show download progress"
        private const val PREF_UNZIP_LOADED = "unzip loaded"
        private const val PREF_LAST_WEBVIEW_LINK = "last webview link"
        private const val PREF_BROWSER_VIEW_MODE = "browser view mode"

        private const val PREF_NIGHT_THEME = "night theme"
        const val NIGHT_THEME_SYSTEM = "1"
        const val NIGHT_THEME_DAY = "2"
        const val NIGHT_THEME_NIGHT = "3"

        @JvmStatic
        var instance: PreferencesHandler = PreferencesHandler()
            private set
    }

}