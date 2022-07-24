package net.veldor.flibusta_test.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import androidx.preference.*
import com.google.android.material.snackbar.Snackbar
import lib.folderpicker.FolderPicker
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityPreferencesBinding
import net.veldor.flibusta_test.model.handler.GrammarHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.helper.BookActionsHelper
import net.veldor.flibusta_test.model.utils.CacheUtils
import net.veldor.flibusta_test.model.utils.TransportUtils
import net.veldor.flibusta_test.model.utils.Updater
import net.veldor.flibusta_test.model.view_model.PreferencesViewModel
import net.veldor.flibusta_test.model.view_model.StartViewModel
import java.io.File
import java.util.*


@Suppress("unused")
class PreferencesActivity : BaseActivity() {
    private lateinit var binding: ActivityPreferencesBinding
    private lateinit var viewModel: PreferencesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(PreferencesViewModel::class.java)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.drawerLayout)
        setupUI()
        // добавлю главный фрагмент
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.preferences, SettingsFragment())
                .commit()
        }
    }

    override fun setupUI() {
        super.setupUI()
        if (PreferencesHandler.instance.isEInk) {
            // тут фикс, так как почему-то не применяется светлая тема при выборе eInk
            findViewById<Toolbar>(R.id.einkToolbar)?.setBackgroundColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    theme
                )
            )
        }
        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goToSettings)
        item.isEnabled = false
        item.isChecked = true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_root, rootKey)
        }
    }

    class TestPreferencesFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_test, rootKey)
        }
    }

    class UpdatePreferencesFragment : PreferenceFragmentCompat() {
        private var mUpdateCheckSnackbar: Snackbar? = null
        private var mUpdateDownloadProgressView: ProgressBar? = null
        private var mUpdateDownloadProgressDialog: AlertDialog? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_update, rootKey)


            val checkUpdateNowPref =
                findPreference<Preference>("check update now")
            checkUpdateNowPref?.setOnPreferenceClickListener {
                (requireActivity() as PreferencesActivity).viewModel.checkForUpdates()
                return@setOnPreferenceClickListener true
            }

            val showBetaReleasesPref =
                findPreference<Preference>("show all beta updates")
            showBetaReleasesPref?.setOnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://github.com/veldor/FlibustaTest/releases")
                startActivity(intent)
                return@setOnPreferenceClickListener true
            }
            val showStableReleasesPref =
                findPreference<Preference>("show all stable updates")
            showStableReleasesPref?.setOnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://github.com/veldor/FlibustaBookLoader/releases")
                startActivity(intent)
                return@setOnPreferenceClickListener true
            }

        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {

            (requireActivity() as PreferencesActivity).viewModel.updateState.observe(
                viewLifecycleOwner
            ) {
                when (it) {
                    StartViewModel.STATE_UPDATE_CHECK_AWAITING -> {
                    }
                    StartViewModel.STATE_UPDATE_CHECK_IN_PROGRESS -> {
                        showUpdateCheckSnackbar()
                    }
                    StartViewModel.STATE_UPDATE_AVAILABLE -> {
                        hideUpdateCheckSnackbar()
                        showUpdateAvailableDialog()
                    }
                    StartViewModel.STATE_UPDATE_NOT_REQUIRED -> {
                        hideUpdateCheckSnackbar()
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.you_use_latest_version_label),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    StartViewModel.STATE_UPDATE_CHECK_FAILED -> {
                        hideUpdateCheckSnackbar()
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.update_check_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            Updater.liveCurrentDownloadProgress.observe(viewLifecycleOwner) {
                if (it >= 0) {
                    updateUpdateDownloadProgress(it)
                } else {
                    hideUpdateDownloadProgressDialog()
                }
            }
            return super.onCreateView(inflater, container, savedInstanceState)
        }

        private fun updateUpdateDownloadProgress(progress: Int?) {
            if (progress != null && progress > 0 && Updater.updateInfo != null) {
                if (mUpdateDownloadProgressDialog == null) {
                    val view =
                        layoutInflater.inflate(
                            R.layout.update_download_progress_layout,
                            null,
                            false
                        )
                    mUpdateDownloadProgressView = view.findViewById(R.id.progressBar)
                    mUpdateDownloadProgressDialog =
                        AlertDialog.Builder(requireContext(), R.style.dialogTheme)
                            .setTitle(getString(R.string.downloading_update_dialog))
                            .setView(view)
                            .setCancelable(false)
                            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                                Updater.cancelUpdate()
                            }
                            .create()
                }
                mUpdateDownloadProgressDialog?.show()
                val currentProgress = (progress / Updater.updateInfo!!.size) * 100
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mUpdateDownloadProgressView?.setProgress(currentProgress.toInt(), true)
                } else {
                    mUpdateDownloadProgressView?.progress = currentProgress.toInt()
                }
            } else {
                hideUpdateDownloadProgressDialog()
            }
        }

        private fun hideUpdateDownloadProgressDialog() {
            if (mUpdateDownloadProgressDialog != null) {
                mUpdateDownloadProgressDialog?.dismiss()
            }
        }

        private fun showUpdateAvailableDialog() {
            val updateInfo = (requireActivity() as PreferencesActivity).viewModel.getUpdateInfo()
            if (updateInfo?.link != null) {
                AlertDialog.Builder(requireContext(), R.style.dialogTheme)
                    .setTitle(getString(R.string.state_update_available))
                    .setMessage(
                        String.format(
                            Locale.ENGLISH,
                            "%s\n%s\nSize: %s",
                            updateInfo.title,
                            updateInfo.body,
                            GrammarHandler.humanReadableByteCountBin(updateInfo.size)
                        )
                    )
                    .setPositiveButton(getString(R.string.download_update_title)) { _, _ ->
                        (requireActivity() as PreferencesActivity).viewModel.getUpdate(
                            updateInfo,
                            requireContext()
                        )
                    }
                    .setNegativeButton(getString(R.string.not_now_title)) { _, _ ->
                    }
                    .setNeutralButton(getString(R.string.ingrore_this_update_title)) { _, _ ->
                        (requireActivity() as PreferencesActivity).viewModel.ignoreUpdate(updateInfo)
                    }
                    .setCancelable(false)
                    .show()
            }
        }

        private fun showUpdateCheckSnackbar() {
            if (mUpdateCheckSnackbar == null) {
                mUpdateCheckSnackbar = Snackbar.make(
                    requireContext(),
                    (requireActivity() as PreferencesActivity).binding.root,
                    "Check for update",
                    Snackbar.LENGTH_INDEFINITE
                )
            }
            mUpdateCheckSnackbar?.show()
        }

        private fun hideUpdateCheckSnackbar() {
            mUpdateCheckSnackbar?.dismiss()
        }
    }

    class ConnectionPreferencesFragment : PreferenceFragmentCompat() {

        private var setBridgesLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_connection, rootKey)

            val useMirrorPref =
                findPreference<SwitchPreferenceCompat>("use custom mirror")
            useMirrorPref?.setOnPreferenceChangeListener { _: Preference?, _: Any? ->
                if (PreferencesHandler.instance.customMirror == PreferencesHandler.BASE_URL) {
                    Toast.makeText(
                        context,
                        getString(R.string.enter_address_warning),
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnPreferenceChangeListener false
                }
                true
            }

            val mirrorAddress =
                findPreference<EditTextPreference>("custom flibusta mirror")
            mirrorAddress?.summary =
                if (PreferencesHandler.instance.customMirror == PreferencesHandler.BASE_URL) getString(
                    R.string.custom_mirror_hint
                ) else PreferencesHandler.instance.customMirror
            mirrorAddress?.setOnPreferenceChangeListener { _: Preference?, value: Any? ->
                val newValue = value as String
                if (newValue.isEmpty()) {
                    PreferencesHandler.instance.customMirror = PreferencesHandler.BASE_URL
                    PreferencesHandler.instance.isCustomMirror = false
                    mirrorAddress.summary = getString(R.string.custom_mirror_hint)
                    return@setOnPreferenceChangeListener false
                } else {
                    if (GrammarHandler.isValidUrl(newValue)) {
                        mirrorAddress.summary = newValue
                    } else {
                        Toast.makeText(
                            context,
                            "$newValue - неверный формат Url. Введите ещё раз в формате http://flibusta.is",
                            Toast.LENGTH_LONG
                        ).show()
                        mirrorAddress.summary = getString(R.string.custom_mirror_hint)
                        return@setOnPreferenceChangeListener false
                    }
                }
                true
            }

            val useTorBridgesPref = findPreference<SwitchPreferenceCompat>("use custom bridges")
            useTorBridgesPref?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    setBridgesLauncher.launch(
                        Intent(
                            requireContext(),
                            SetTorBridgesActivity::class.java
                        )
                    )
                }
                return@setOnPreferenceChangeListener true
            }

            val torBridgesPref =
                findPreference<Preference>("custom bridges")
            torBridgesPref?.setOnPreferenceClickListener {
                setBridgesLauncher.launch(
                    Intent(
                        requireContext(),
                        SetTorBridgesActivity::class.java
                    )
                )
                return@setOnPreferenceClickListener true
            }
        }
    }

    class ViewPreferencesFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_view, rootKey)
        }

        override fun onResume() {
            super.onResume()
            val switchViewPref = findPreference<Preference>("is eInk")
            val switchNightModePref = findPreference<DropDownPreference>("night theme")
            if (switchViewPref != null) {
                switchViewPref.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _: Preference?, _: Any? ->
                        requireActivity().recreate()
                        true
                    }
            }
            if (switchNightModePref != null) {
                switchNightModePref.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _, new_value ->
                        when (new_value as String) {
                            PreferencesHandler.NIGHT_THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            )
                            PreferencesHandler.NIGHT_THEME_DAY -> AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_NO
                            )
                            PreferencesHandler.NIGHT_THEME_NIGHT -> AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_YES
                            )
                        }
                        requireActivity().recreate()
                        true
                    }
            }
        }
    }

    class OpdsPreferencesFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_opds, rootKey)
        }
    }

    class CachePreferencesFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_cache, rootKey)
        }

        override fun onResume() {
            super.onResume()
            val clearCachePref =
                findPreference<Preference>("clear cache now")
            clearCachePref?.summary = "Занято: " + CacheUtils.getTotalCacheSize(requireContext())
            clearCachePref?.setOnPreferenceClickListener {
                Toast.makeText(requireContext(), "Clear", Toast.LENGTH_SHORT).show()
                CacheUtils.clearAllCache(requireContext())
                clearCachePref.summary = "Занято: " + CacheUtils.getTotalCacheSize(requireContext())
                return@setOnPreferenceClickListener true
            }

            val maxCacheSizePref =
                findPreference<SeekBarPreference>("max cache size")
            maxCacheSizePref?.summary = "${PreferencesHandler.instance.maxCacheSize} мб"
            maxCacheSizePref?.setOnPreferenceChangeListener { _: Preference?, value: Any? ->
                maxCacheSizePref.summary = (value as Int).toString() + " мб"
                true
            }
            maxCacheSizePref?.showSeekBarValue = true
        }
    }

    class ReservePreferencesFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_reserve, rootKey)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // получу ссылку на резервирование настроек
            val settingsBackupPref = findPreference<Preference>("backup settings")
            val settingsRestorePref =
                findPreference<Preference>("restore settings")
            if (settingsBackupPref != null) {
                settingsBackupPref.onPreferenceClickListener =
                    Preference.OnPreferenceClickListener {
                        Toast.makeText(
                            context,
                            "Выберите папку для сохранения резервной копии",
                            Toast.LENGTH_SHORT
                        ).show()

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            var intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            intent.addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                        or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                            )
                            if (TransportUtils.intentCanBeHandled(intent)) {
                                backupDirSelectResultLauncher.launch(intent)
                            } else {
                                intent = Intent(context, FolderPicker::class.java)
                                intent.addFlags(
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                            or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                )
                                //compatBackupDirSelectResultLauncher.launch(intent)
                            }
                        } else {
                            val intent = Intent(context, FolderPicker::class.java)
                            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                                intent.addFlags(
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                            or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                )
                            }
                            compatBackupDirSelectResultLauncher.launch(intent)
                        }
                        false
                    }
            }
            if (settingsRestorePref != null) {
                settingsRestorePref.onPreferenceClickListener =
                    Preference.OnPreferenceClickListener {
                        Toast.makeText(
                            context,
                            "Выберите сохранённый ранее файл с настройками.",
                            Toast.LENGTH_LONG
                        ).show()
                        // открою окно выбота файла для восстановления
                        val intent: Intent =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                Intent(Intent.ACTION_OPEN_DOCUMENT)
                            } else {
                                Intent(Intent.ACTION_GET_CONTENT)
                            }
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        intent.type = "application/zip"
                        if (TransportUtils.intentCanBeHandled(intent)) {
                            Toast.makeText(context, "Восстанавливаю настройки.", Toast.LENGTH_LONG)
                                .show()
                            restoreFileSelectResultLauncher.launch(intent)
                        } else {
                            Toast.makeText(
                                context,
                                "Упс, не нашлось приложения, которое могло бы это сделать.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        false
                    }
            }
        }


        private var backupDirSelectResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    if (result != null) {
                        val treeUri = result.data!!.data
                        if (treeUri != null) {
                            // проверю наличие файла
                            val dl = DocumentFile.fromTreeUri(App.instance, treeUri)
                            if (dl != null && dl.isDirectory) {
                                val builder = AlertDialog.Builder(requireContext())
                                // покажу список с выбором того, что нужно резервировать
                                val backupOptions = arrayOf(
                                    "Базовые настройки", // 0
                                    "Загруженные книги", // 1
                                    "Прочитанные книги", // 2
                                    "Автозаполнение поиска", // 3
                                    "Список закладок", // 4
                                    "Подписки", // 5
                                    "Фильтры",// 13
                                    "Список книг для загрузки", // 14
                                )
                                val checkedOptions = booleanArrayOf(
                                    true,
                                    true,
                                    true,
                                    true,
                                    true,
                                    true,
                                    true,
                                    true
                                )
                                builder.setMultiChoiceItems(
                                    backupOptions, checkedOptions
                                ) { _, which, isChecked ->
                                    checkedOptions[which] = isChecked
                                }
                                    .setTitle("Выберите элементы для резервирования")
                                // Set the positive/yes button click listener

                                // Set the positive/yes button click listener
                                builder.setPositiveButton("OK") { _, _ ->
                                    (requireActivity() as PreferencesActivity).viewModel.backup(
                                        dl,
                                        checkedOptions
                                    )
                                    (requireActivity() as PreferencesActivity).viewModel.liveBackupData.observe(
                                        viewLifecycleOwner
                                    ) {
                                        if (it != null) {
                                            // send file
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                                BookActionsHelper.shareBook(it)
                                            }
                                            (requireActivity() as PreferencesActivity).viewModel.liveBackupData.removeObservers(
                                                viewLifecycleOwner
                                            )
                                        } else {
                                            Toast.makeText(
                                                requireContext(),
                                                "Can't create backup file, try again!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                                builder.show()
                            }
                        }
                    }
                }
            }

        private var compatBackupDirSelectResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    if (result != null) {
                        val data: Intent? = result.data
                        if (data != null && data.extras != null && data.extras!!.containsKey("data")) {
                            val folderLocation = data.extras!!.getString("data")
                            val file = File(folderLocation)
                            if (file.isDirectory) {
                                val builder = AlertDialog.Builder(requireContext())
                                // покажу список с выбором того, что нужно резервировать
                                val backupOptions = arrayOf(
                                    "Базовые настройки", // 0
                                    "Загруженные книги", // 1
                                    "Прочитанные книги", // 2
                                    "Автозаполнение поиска", // 3
                                    "Список закладок", // 4
                                    "Подписки", // 5
                                    "Фильтры",
                                    "Список книг для загрузки" // 14
                                )

                                val checkedOptions = booleanArrayOf(
                                    true, // 0
                                    true, // 1
                                    true, // 2
                                    true, // 3
                                    true, // 4
                                    true, // 5
                                    true, // 6
                                    true, // 7
                                )
                                builder.setMultiChoiceItems(
                                    backupOptions, checkedOptions
                                ) { _, which, isChecked ->
                                    checkedOptions[which] = isChecked
                                }
                                    .setTitle("Выберите элементы для резервирования")
                                // Set the positive/yes button click listener

                                // Set the positive/yes button click listener
                                builder.setPositiveButton("OK") { _, _ ->
                                    (requireActivity() as PreferencesActivity).viewModel.backup(
                                        file,
                                        checkedOptions
                                    )
                                    (requireActivity() as PreferencesActivity).viewModel.liveCompatBackupData.observe(
                                        viewLifecycleOwner
                                    ) {
                                        if (it != null) {
                                            // send file
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                                BookActionsHelper.shareBook(
                                                    it
                                                )
                                            }
                                            (requireActivity() as PreferencesActivity).viewModel.liveCompatBackupData.removeObservers(
                                                viewLifecycleOwner
                                            )
                                        } else {
                                            Toast.makeText(
                                                requireContext(),
                                                "Can't create backup file, try again!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                                builder.show()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Не удалось сохранить папку, попробуйте ещё раз!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }

        private var restoreFileSelectResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val uri: Uri?
                        if (result != null) {
                            uri = result.data?.data
                            if (uri != null) {
                                val dl = DocumentFile.fromSingleUri(App.instance, uri)
                                if (dl != null) {
                                    // буду восстанавливать настройки
                                    // попробую получить список файлов в архиве
                                    val builder = AlertDialog.Builder(requireContext())
                                    val checkResults =
                                        (requireActivity() as PreferencesActivity).viewModel.checkReserve(
                                            dl
                                        )
                                    // покажу список с выбором того, что нужно резервировать
                                    val backupOptions = arrayOf(
                                        "Базовые настройки", // 0
                                        "Загруженные книги", // 1
                                        "Прочитанные книги", // 2
                                        "Автозаполнение поиска", // 3
                                        "Список закладок", // 4
                                        "Подписки", // 5
                                        "Фильтры",// 13
                                        "Список книг для загрузки" // 14
                                    )
                                    builder.setMultiChoiceItems(
                                        backupOptions, checkResults
                                    ) { _, which, isChecked ->
                                        checkResults[which] = isChecked
                                    }
                                        .setTitle("Выберите элементы для резервирования")
                                        .setPositiveButton("OK") { _, _ ->
                                            (requireActivity() as PreferencesActivity).viewModel.restore(
                                                dl, checkResults
                                            )
                                            (requireActivity() as PreferencesActivity).viewModel.livePrefsRestored.observe(
                                                viewLifecycleOwner
                                            ) {
                                                if (it) {
                                                    Toast.makeText(
                                                        requireContext(),
                                                        "Preferences restored, reboot app",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    Handler().postDelayed(ResetApp(), 3000)
                                                }
                                            }
                                        }
                                        .show()

                                }
                            }
                        }
                    } else {
                        if (result != null) {
                            val data: Intent? = result.data
                            if (data != null && data.extras != null && data.extras!!.containsKey("data")) {
                                val folderLocation = data.extras!!.getString("data")
                                val file = File(folderLocation)
                                if (file.isFile) {
                                    // буду восстанавливать настройки
                                    // попробую получить список файлов в архиве
                                    val builder = AlertDialog.Builder(requireContext())
                                    val checkResults =
                                        (requireActivity() as PreferencesActivity).viewModel.checkReserve(
                                            file
                                        )
                                    // покажу список с выбором того, что нужно резервировать
                                    val backupOptions = arrayOf(
                                        "Базовые настройки", // 0
                                        "Загруженные книги", // 1
                                        "Прочитанные книги", // 2
                                        "Автозаполнение поиска", // 3
                                        "Список закладок", // 4
                                        "Подписки", // 5
                                        "Фильтры",  // 9
                                        "Список книг для загрузки" // 14
                                    )
                                    builder.setMultiChoiceItems(
                                        backupOptions, checkResults
                                    ) { _, which, isChecked ->
                                        checkResults[which] = isChecked
                                    }
                                        .setTitle("Выберите элементы для резервирования")
                                        .setPositiveButton("OK") { _, _ ->
                                            (requireActivity() as PreferencesActivity).viewModel.restore(
                                                file, checkResults
                                            )
                                            (requireActivity() as PreferencesActivity).viewModel.livePrefsRestored.observe(
                                                viewLifecycleOwner
                                            ) {
                                                if (it) {
                                                    Toast.makeText(
                                                        requireContext(),
                                                        "Preferences restored, reboot app",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    Handler().postDelayed(ResetApp(), 3000)
                                                }
                                            }
                                        }
                                        .show()
                                }
                            }
                        }
                    }
                }
            }

        companion object {
            private const val BACKUP_FILE_REQUEST_CODE = 10
            private const val REQUEST_CODE = 1
            private const val READ_REQUEST_CODE = 2
        }
    }
}