package net.veldor.flibusta_test.model.handler

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.model.helper.UrlHelper
import net.veldor.flibusta_test.model.selection.RootDownloadDir
import net.veldor.flibusta_test.view.search_fragment.WebViewFragment
import java.io.File

object PreferencesHandler {

    var authCookie: String?
        get() = preferences.getString(AUTH_COOKIE_VALUE_PREF, null)
        set(value) {
            if (value == null) {
                preferences.edit().remove(AUTH_COOKIE_VALUE_PREF).apply()
            } else {
                preferences.edit().putString(AUTH_COOKIE_VALUE_PREF, value).apply()
            }
        }

    private fun getModernRoot(): DocumentFile? {
        val path = preferences.getString(PREF_DOWNLOADS_DIR, null)
        if (path != null) {
            return DocumentFile.fromTreeUri(App.instance, Uri.parse(path))
        }
        return null
    }

    private fun getCompatRoot(): File? {
        val path = preferences.getString(PREF_COMPAT_DOWNLOAD_LOCATION, null)
        if (path != null) {
            return File(path)
        }
        return null
    }

    fun setCompatRoot(file: File) {
        preferences.edit().putString(PREF_COMPAT_DOWNLOAD_LOCATION, file.absolutePath).apply()
    }

    fun setModernRoot(file: DocumentFile) {
        preferences.edit().putString(PREF_DOWNLOADS_DIR, file.uri.toString()).apply()
    }

    var companionAppCoordinates: String
        set(value) {
            preferences.edit().putString(PREF_COMPANION_APP_COORDINATES, value).apply()
        }
        get() {
            return preferences.getString(PREF_COMPANION_APP_COORDINATES, "") ?: ""
        }

    val rootDownloadDirPath: String
        get() {
            if (storageAccessDenied) {
                return "Storage access denied, books will be loaded in external memory"
            }
            val dir = rootDownloadDir
            if (dir.canWrite()) {
                return dir.path
            }
            return "Dir not set"
        }

    val rootDownloadDir: RootDownloadDir
        get() {
            if (storageAccessDenied) {
                return RootDownloadDir()
            }
            val result = RootDownloadDir()
            val root = getModernRoot()
            if (root != null) {
                result.root = root
            } else {
                val compatRoot = getCompatRoot()
                if (compatRoot != null) {
                    result.compatRoot = compatRoot
                }
            }
            return result
        }

    val checkUpdateOnStart: Boolean
        get() {
            return preferences.getBoolean(PREF_CHECK_UPDATE_ON_START, true)
        }

    val isConvertFb2ForCompanion: Boolean
        get() {
            return preferences.getBoolean(PREF_CONVERT_FB2_FOR_COMPANION, false)
        }

    val isConvertFb2ForKindle: Boolean
        get() {
            return preferences.getBoolean(PREF_CONVERT_FB2_FOR_KINDLE, false)
        }

    var storageAccessDenied: Boolean
        get() {
            return preferences.getBoolean(PREF_STORAGE_ACCESS_DENIED, false)
        }
        set(value) {
            preferences.edit().putBoolean(PREF_STORAGE_ACCESS_DENIED, value).apply()
        }


    var showSwitchConnectionHint: Boolean
        get() {
            return preferences.getBoolean(PREF_OFFER_CONNECTION_SWITCH, true)
        }
        set(value) {
            preferences.edit().putBoolean(PREF_OFFER_CONNECTION_SWITCH, value).apply()
        }

    var connectionType: Int
        get() {
            return preferences.getInt(PREF_CONNECTION_MODE, CONNECTION_MODE_UNSPECIFIED)
        }
        set(value) {
            preferences.edit().putInt(PREF_CONNECTION_MODE, value).apply()
        }

    var isEInk: Boolean
        get() = preferences.getBoolean(
            EINK_PREF, false
        )
        set(state) {
            preferences.edit().putBoolean(EINK_PREF, state).apply()
        }

    var hardwareAcceleration: Boolean
        get() = preferences.getBoolean(HW_ACCELERATION_PREF, true)
        set(state) {
            preferences.edit().putBoolean(HW_ACCELERATION_PREF, state).apply()
        }

    val appVersion: String
        get() {
            try {
                val pInfo = App.instance.packageManager.getPackageInfo(
                    App.instance.packageName, 0
                )
                return pInfo.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                Log.d("surprise", "MainActivity setupUI: can't found version")
                e.printStackTrace()
            }
            return "0"
        }

    var rememberFavoriteFormat: Boolean
        get() = preferences.getBoolean(PREF_REMEMBER_FAVORITE_FORMAT, false)
        set(state) {
            preferences.edit().putBoolean(PREF_REMEMBER_FAVORITE_FORMAT, state).apply()
        }

    var favoriteFormat: String?
        get() = preferences.getString(PREF_FAVORITE_FORMAT, null)
        set(state) {
            Log.d("surprise", "saving favorite: $state")
            preferences.edit().putString(PREF_FAVORITE_FORMAT, state).apply()
        }

    var showFoundBookGenres: Boolean
        get() = preferences.getBoolean(PREF_FOUND_ITEM_SHOW_GENRES, true)
        set(state) {
            preferences.edit().putBoolean(PREF_FOUND_ITEM_SHOW_GENRES, state).apply()
        }
    var showFoundBookSequences: Boolean
        get() = preferences.getBoolean(PREF_FOUND_ITEM_SHOW_SEQUENCES, true)
        set(state) {
            preferences.edit().putBoolean(PREF_FOUND_ITEM_SHOW_SEQUENCES, state).apply()
        }

    var showAuthors: Boolean
        get() = preferences.getBoolean(PREF_FOUND_ITEM_SHOW_AUTHORS, true)
        set(state) {
            preferences.edit().putBoolean(PREF_FOUND_ITEM_SHOW_AUTHORS, state).apply()
        }

    var showFoundBookTranslators: Boolean
        get() = preferences.getBoolean(PREF_FOUND_ITEM_SHOW_TRANSLATORS, true)
        set(state) {
            preferences.edit().putBoolean(PREF_FOUND_ITEM_SHOW_TRANSLATORS, state).apply()
        }

    var strictDownloadFormat: Boolean
        get() = preferences.getBoolean(PREF_STRICT_FORMAT, false)
        set(state) {
            preferences.edit().putBoolean(PREF_STRICT_FORMAT, state).apply()
        }

    var showDownloadProgress: Boolean
        get() = preferences.getBoolean(PREF_SHOW_DOWNLOAD_PROGRESS, false)
        set(state) {
            preferences.edit().putBoolean(PREF_SHOW_DOWNLOAD_PROGRESS, state).apply()
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

    var createSequenceDir: Boolean
        get() = preferences.getBoolean(PREF_CREATE_SEQUENCE_DIR, false)
        set(state) {
            preferences.edit().putBoolean(PREF_CREATE_SEQUENCE_DIR, state).apply()
        }

    var opdsPagingType: Boolean
        get() {
            return preferences.getBoolean(PREF_OPDS_SHOW_PAGING, true)
        }
        set(state) {
            preferences.edit().putBoolean(PREF_OPDS_SHOW_PAGING, state).apply()
        }

    var createAuthorDir: Boolean
        get() = preferences.getBoolean(PREF_CREATE_AUTHOR_DIR, false)
        set(state) {
            preferences.edit().putBoolean(PREF_CREATE_AUTHOR_DIR, state).apply()
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


    var isClearCache: Boolean
        get() = preferences.getBoolean("auto clear cache", false)
        set(state) {
            preferences.edit().putBoolean("auto clear cache", state).apply()
        }


    val longMaxCacheSize: Long
        get() = maxCacheSize.toLong() * 1000 * 1000

    var maxCacheSize: Int
        get() = preferences.getInt("max cache size", 1)
        set(state) {
            preferences.edit().putInt("max cache size", state).apply()
        }

    var disableHistoryMessageViewed: Boolean
        get() = preferences.getBoolean("disable catalog history message", false)
        set(state) {
            preferences.edit().putBoolean("disable catalog history message", state).apply()
        }

    var saveOpdsHistory: Boolean
        get() = preferences.getBoolean(PREF_OPDS_HISTORY, true)
        set(state) {
            preferences.edit().putBoolean(PREF_OPDS_HISTORY, state).apply()
        }

    var showFilterStatistics: Boolean
        get() = preferences.getBoolean(PREF_SHOW_FILTER_STATISTICS, false)
        set(state) {
            preferences.edit().putBoolean(PREF_SHOW_FILTER_STATISTICS, state).apply()
        }

    var showCovers: Boolean
        get() {
            return preferences.getBoolean(PREF_SHOW_COVERS, true)
        }
        set(state) {
            preferences.edit().putBoolean(PREF_SHOW_COVERS, state).apply()
        }
    var showCoversByRequest: Boolean
        get() {
            return preferences.getBoolean(PREF_SHOW_COVERS_BY_REQUEST, false)
        }
        set(state) {
            preferences.edit().putBoolean(PREF_SHOW_COVERS_BY_REQUEST, state).apply()
        }

    var showFoundBookAvailableFormats: Boolean
        get() = preferences.getBoolean(PREF_FOUND_ITEM_SHOW_AVAILABLE_FORMATS, true)
        set(state) {
            preferences.edit().putBoolean(PREF_FOUND_ITEM_SHOW_AVAILABLE_FORMATS, state).apply()
        }

    var showFoundBookReadBtn: Boolean
        get() = preferences.getBoolean(PREF_FOUND_ITEM_SHOW_READ_BTN, true)
        set(state) {
            preferences.edit().putBoolean(PREF_FOUND_ITEM_SHOW_READ_BTN, state).apply()
        }

    var showFoundBookDownloadBtn: Boolean
        get() = preferences.getBoolean(PREF_FOUND_ITEM_SHOW_DLOAD_BTN, true)
        set(state) {
            preferences.edit().putBoolean(PREF_FOUND_ITEM_SHOW_DLOAD_BTN, state).apply()
        }
    var showElementDescription: Boolean
        get() = preferences.getBoolean(PREF_FOUND_ITEM_SHOW_ELEMENT_DESCRIPTION, true)
        set(state) {
            preferences.edit().putBoolean(PREF_FOUND_ITEM_SHOW_ELEMENT_DESCRIPTION, state).apply()
        }

    var showFoundBookFormat: Boolean
        get() = preferences.getBoolean(PREF_FOUND_ITEM_SHOW_FORMAT, true)
        set(state) {
            preferences.edit().putBoolean(PREF_FOUND_ITEM_SHOW_FORMAT, state).apply()
        }

    var showFoundBookDownloads: Boolean
        get() = preferences.getBoolean(PREF_FOUND_ITEM_SHOW_DOWNLOADS, true)
        set(state) {
            preferences.edit().putBoolean(PREF_FOUND_ITEM_SHOW_DOWNLOADS, state).apply()
        }

    var showFoundBookSize: Boolean
        get() = preferences.getBoolean(PREF_FOUND_ITEM_SHOW_SIZE, true)
        set(state) {
            preferences.edit().putBoolean(PREF_FOUND_ITEM_SHOW_SIZE, state).apply()
        }

    var hideOpdsResultsButtons: Boolean
        get() = preferences.getBoolean(PREF_HIDE_OPDS_SEARCH_RESULTS_BUTTONS, false)
        set(state) {
            preferences.edit().putBoolean(PREF_HIDE_OPDS_SEARCH_RESULTS_BUTTONS, state).apply()
        }


    var addFilterByLongClick: Boolean
        get() = preferences.getBoolean(PREF_LONG_CLICK_TO_FILTER, false)
        set(state) {
            preferences.edit().putBoolean(PREF_LONG_CLICK_TO_FILTER, state).apply()
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

    var isOpdsUseFilter: Boolean
        get() = preferences.getBoolean(PREF_OPDS_USE_FILTER, false)
        set(state) {
            preferences.edit().putBoolean(PREF_OPDS_USE_FILTER, state).apply()
        }

    var coversMessageViewed: Boolean
        get() = preferences.getBoolean("covers message viewed", false)
        set(state) {
            preferences.edit().putBoolean("covers message viewed", state).apply()
        }

    var opdsLayoutRowsCount: Int
        get() = preferences.getInt(PREF_OPDS_ROWS, 0)
        set(state) {
            preferences.edit().putInt(PREF_OPDS_ROWS, state).apply()
        }

    var nightMode: String
        get() = preferences.getString(PREF_NIGHT_THEME, NIGHT_THEME_SYSTEM)!!
        set(state) {
            preferences.edit().putString(PREF_NIGHT_THEME, state).apply()
        }


    val toolbarSearchShown: Boolean
        get() = preferences.getBoolean(PREF_TOOLBAR_SEARCH_VISIBILITY, true)
    val toolbarSortShown: Boolean
        get() = preferences.getBoolean(PREF_TOOLBAR_SORT_VISIBILITY, true)
    val toolbarBlockedShown: Boolean
        get() = preferences.getBoolean(PREF_TOOLBAR_BLOCKED_VISIBILITY, true)
    val toolbarDloadStateShown: Boolean
        get() = preferences.getBoolean(PREF_TOOLBAR_DLOAD_STATE_VISIBILITY, true)
    val toolbarThemeShown: Boolean
        get() = preferences.getBoolean(PREF_TOOLBAR_THEME_VISIBILITY, true)
    val toolbarEinkShown: Boolean
        get() = preferences.getBoolean(PREF_TOOLBAR_EINK_VISIBILITY, true)
    val toolbarBookmarkShown: Boolean
        get() = preferences.getBoolean(PREF_TOOLBAR_BOOKMARK_VISIBILITY, true)
    val toolbarViewConfigShown: Boolean
        get() = preferences.getBoolean(PREF_TOOLBAR_VIEW_CONFIG_VISIBILITY, true)


    var skipDownloadSetup: Boolean
        get() = preferences.getBoolean(PREF_SKIP_DOWNLOAD_SETUP, false)
        set(state) {
            preferences.edit().putBoolean(PREF_SKIP_DOWNLOAD_SETUP, state).apply()
        }


    var lastWebViewLink: String?
        get() = preferences.getString(
            PREF_LAST_WEBVIEW_LINK, ""
        )!!
        set(url) {
            if (url?.contains("/favicon.ico") == false && !url.contains("/sites/default/files/bluebreeze_favicon.ico")) preferences.edit()
                .putString(
                    PREF_LAST_WEBVIEW_LINK, url.replace(UrlHelper.getBaseUrl(), "")
                ).apply()
        }

    var savingLogs: Boolean
        get() = preferences.getBoolean(PREF_SAVING_LOGS, false)
        set(state) {
            preferences.edit().putBoolean(PREF_SAVING_LOGS, state).apply()
        }

    var useTorMirror
        get() = preferences.getBoolean(
            IS_TOR_MIRROR_PREF, false
        )
        set(state) {
            preferences.edit().putBoolean(IS_TOR_MIRROR_PREF, state).apply()
        }

    var isCustomMirror: Boolean
        get() = preferences.getBoolean(
            IS_CUSTOM_MIRROR_PREF, false
        )
        set(state) {
            preferences.edit().putBoolean(IS_CUSTOM_MIRROR_PREF, state).apply()
        }

    var customMirror: String
        get() = preferences.getString(
            CUSTOM_MIRROR_PREF, BASE_URL
        )!!
        set(state) {
            preferences.edit().putString(CUSTOM_MIRROR_PREF, state).apply()
        }


    var isHideDigests: Boolean
        get() = preferences.getBoolean(HIDE_DIGESTS_PREF, false)
        set(state) {
            preferences.edit().putBoolean(HIDE_DIGESTS_PREF, state).apply()
        }

    var isOnlyRussian: Boolean
        get() = preferences.getBoolean(PREF_ONLY_RUSSIAN, false)
        set(state) {
            preferences.edit().putBoolean(PREF_ONLY_RUSSIAN, state).apply()
        }

    var downloadAutostart: Boolean
        get() = preferences.getBoolean(PREF_DOWNLOAD_AUTOSTART, true)
        set(state) {
            preferences.edit().putBoolean(PREF_DOWNLOAD_AUTOSTART, state).apply()
        }

    val sendToKindle: Boolean
        get() = preferences.getBoolean(PREF_SEND_TO_KINDLE, false)

    val unzipLoaded: Boolean
        get() = preferences.getBoolean(PREF_UNZIP_LOADED, false)


    var browserViewMode: Int
        get() = preferences.getInt(PREF_BROWSER_VIEW_MODE, WebViewFragment.VIEW_MODE_FAST)
        set(state) {
            preferences.edit().putInt(PREF_BROWSER_VIEW_MODE, state).apply()
        }


    var lastCheckedForSubscription: String?
        get() = preferences.getString(PREF_LAST_CHECKED_FOR_SUBSCRIPTION, null)
        set(state) {
            preferences.edit().putString(PREF_LAST_CHECKED_FOR_SUBSCRIPTION, state).apply()
        }

    var autoDownloadSubscriptions: Boolean
        get() = preferences.getBoolean(PREF_AUTO_DOWNLOAD_SUBSCRIPTIONS, false)
        set(state) {
            preferences.edit().putBoolean(PREF_AUTO_DOWNLOAD_SUBSCRIPTIONS, state).apply()
        }

    var autoCheckSubscriptions: Boolean
        get() = preferences.getBoolean(PREF_AUTO_CHECK_SUBSCRIPTIONS, false)
        set(state) {
            preferences.edit().putBoolean(PREF_AUTO_CHECK_SUBSCRIPTIONS, state).apply()
        }

    var skipLoadScreen: Boolean
        get() = preferences.getBoolean(PREF_SKIP_LOAD_SCREEN, false)
        set(state) {
            preferences.edit().putBoolean(PREF_SKIP_LOAD_SCREEN, state).apply()
        }

    var checkUpdateAfter: Int
        get() = preferences.getInt(PREF_CHECK_UPDATE_AFTER, -1)
        set(state) {
            preferences.edit().putInt(PREF_CHECK_UPDATE_AFTER, state).apply()
        }

    var useCompanionApp: Boolean
        get() = preferences.getBoolean(PREF_USE_COMPANION_APP, false)
        set(state) {
            preferences.edit().putBoolean(PREF_USE_COMPANION_APP, state).apply()
        }

    var autosendToCompanionApp: Boolean
        get() = preferences.getBoolean(AUTOSEND_TO_COMPANION_APP, false)
        set(state) {
            preferences.edit().putBoolean(AUTOSEND_TO_COMPANION_APP, state).apply()
        }

    private var preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(App.instance)
    private const val PREF_CONNECTION_MODE = "connection mode"
    private const val PREF_OFFER_CONNECTION_SWITCH = "offer connection switch"
    private const val HW_ACCELERATION_PREF = "hardware acceleration"
    private const val PREF_REMEMBER_FAVORITE_FORMAT = "remember favorite format"
    private const val PREF_FAVORITE_FORMAT = "favorite format"
    private const val PREF_FOUND_ITEM_SHOW_AUTHORS = "found item show authors"
    private const val PREF_FOUND_ITEM_SHOW_TRANSLATORS = "found item show translators"
    private const val PREF_FOUND_ITEM_SHOW_SEQUENCES = "found item show sequences"
    private const val PREF_FOUND_ITEM_SHOW_GENRES = "found item show genres"
    private const val PREF_STRICT_FORMAT = "strict format"
    private const val PREF_SHOW_DOWNLOAD_PROGRESS = "show download progress"
    private const val PREF_OPDS_SHOW_PAGING = "opds show paging"
    private const val EINK_PREF = "is eInk"
    private const val PREF_CREATE_DIFFERENT_DIRS = "different dirs"
    private const val PREF_SEQUENCES_IN_AUTHORS_DIR = "load sequences to author dir"
    private const val PREF_CREATE_SEQUENCE_DIR = "create sequence dir"
    private const val PREF_CREATE_AUTHOR_DIR = "create author dir"
    private const val PREF_AUTHOR_IN_BOOK_NAME = "author in book name"
    private const val PREF_SEQUENCE_IN_BOOK_NAME = "sequence in book name"
    private const val PREF_OPDS_HISTORY = "enable opds history"
    private const val PREF_SHOW_FILTER_STATISTICS = "show filter statistics"
    private const val PREF_SHOW_COVERS = "show covers"
    private const val PREF_SHOW_COVERS_BY_REQUEST = "show covers by request"
    private const val PREF_FOUND_ITEM_SHOW_FORMAT = "found item show format"
    private const val PREF_FOUND_ITEM_SHOW_DOWNLOADS = "found item show downloads"
    private const val PREF_FOUND_ITEM_SHOW_SIZE = "found item show size"
    private const val PREF_FOUND_ITEM_SHOW_AVAILABLE_FORMATS = "found item show available formats"
    private const val PREF_FOUND_ITEM_SHOW_READ_BTN = "found item show read button"
    private const val PREF_FOUND_ITEM_SHOW_DLOAD_BTN = "found item show download button"
    private const val PREF_FOUND_ITEM_SHOW_ELEMENT_DESCRIPTION =
        "found item show element description"
    private const val PREF_HIDE_OPDS_SEARCH_RESULTS_BUTTONS = "no item buttons"
    private const val PREF_LONG_CLICK_TO_FILTER = "add to filter on long click"
    private const val HIDE_DOWNLOADED_PREF = "hide download"
    private const val PREF_OPDS_USE_FILTER = "use filter"
    private const val PREF_HIDE_READ = "hide read"
    private const val PREF_OPDS_ROWS = "opds rows"
    private const val PREF_NIGHT_THEME = "night theme"
    const val NIGHT_THEME_SYSTEM = "1"
    const val NIGHT_THEME_DAY = "2"
    const val NIGHT_THEME_NIGHT = "3"
    private const val PREF_TOOLBAR_SEARCH_VISIBILITY = "show toolbar search"
    private const val PREF_TOOLBAR_SORT_VISIBILITY = "show toolbar sort"
    private const val PREF_TOOLBAR_BLOCKED_VISIBILITY = "show toolbar blocked"
    private const val PREF_TOOLBAR_DLOAD_STATE_VISIBILITY = "show toolbar download state"
    private const val PREF_TOOLBAR_THEME_VISIBILITY = "show toolbar theme"
    private const val PREF_TOOLBAR_EINK_VISIBILITY = "show toolbar eink"
    private const val PREF_TOOLBAR_BOOKMARK_VISIBILITY = "show toolbar bookmark"
    private const val PREF_TOOLBAR_VIEW_CONFIG_VISIBILITY = "show toolbar view config"
    private const val PREF_SKIP_DOWNLOAD_SETUP = "skip download setup"
    private const val PREF_LAST_WEBVIEW_LINK = "last webview link"
    private const val PREF_SAVING_LOGS = "saving log files"
    private const val IS_TOR_MIRROR_PREF = "use tor mirror"
    private const val IS_CUSTOM_MIRROR_PREF = "use custom mirror"
    private const val CUSTOM_MIRROR_PREF = "custom flibusta mirror"
    private const val HIDE_DIGESTS_PREF = "hide digests"
    private const val PREF_ONLY_RUSSIAN = "only russian"
    private const val PREF_DOWNLOAD_AUTOSTART = "download auto start"
    private const val PREF_DOWNLOADS_DIR = "downloads dir"
    private const val PREF_STORAGE_ACCESS_DENIED = "storage access denied"
    private const val PREF_COMPAT_DOWNLOAD_LOCATION = "compat download_location"
    private const val PREF_UNZIP_LOADED = "unzip loaded"
    private const val PREF_SEND_TO_KINDLE = "send to kindle"
    private const val PREF_BROWSER_VIEW_MODE = "browser view mode"
    private const val PREF_AUTO_DOWNLOAD_SUBSCRIPTIONS = "auto download subscriptions"
    private const val PREF_LAST_CHECKED_FOR_SUBSCRIPTION = "last checked for subscription"
    private const val PREF_AUTO_CHECK_SUBSCRIPTIONS = "auto check subscriptions"
    private const val PREF_SKIP_LOAD_SCREEN = "skip load screen"
    private const val PREF_CHECK_UPDATE_AFTER = "check update after"
    private const val PREF_USE_COMPANION_APP = "use companion app"
    private const val AUTOSEND_TO_COMPANION_APP = "auto send to companion app"
    private const val PREF_COMPANION_APP_COORDINATES = "companion app coordinates"
    private const val AUTH_COOKIE_VALUE_PREF = "auth cookie value"
    private const val PREF_CHECK_UPDATE_ON_START = "check update on start"
    private const val PREF_CONVERT_FB2_FOR_COMPANION = "convert fb2 on companion"
    private const val PREF_CONVERT_FB2_FOR_KINDLE = "convert fb2 for kindle"
    const val CONNECTION_MODE_TOR = 1
    const val CONNECTION_MODE_VPN = 2
    const val CONNECTION_MODE_UNSPECIFIED = 0

    const val BASE_URL = "http://flibusta.is"
    const val BASE_TOR_URL = "http://flibustaongezhld6dibs2dps6vm4nvqg2kp7vgowbu76tzopgnhazqd.onion"
}