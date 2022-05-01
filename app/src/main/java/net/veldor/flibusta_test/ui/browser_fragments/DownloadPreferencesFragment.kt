package net.veldor.flibusta_test.ui.browser_fragments

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import lib.folderpicker.FolderPicker
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.utils.TransportUtils
import net.veldor.flibusta_test.ui.DownloadBookSetupActivity
import java.io.File

class DownloadPreferencesFragment : PreferenceFragmentCompat() {
    var prefChangedDelegate: DownloadBookSetupActivity? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_download, rootKey)

    }

    override fun onResume() {
        super.onResume()
        if (prefChangedDelegate != null) {
            val prefScreen: PreferenceScreen = preferenceScreen
            val prefCount: Int = prefScreen.preferenceCount
            for (i in 0 until prefCount) {
                val pref: Preference = prefScreen.getPreference(i)
                if(pref is PreferenceCategory){
                    recursiveAddListener(pref)
                }
                else{
                    pref.onPreferenceChangeListener = prefChangedDelegate
                }
            }
        }

        // handle download dir selector
        val changeDownloadFolderPreference = findPreference<Preference>("download dir location")
        if (changeDownloadFolderPreference != null) {
            // отображу текущую выбранную папку
            changeDownloadFolderPreference.summary =
                "Текущая папка: " + PreferencesHandler.instance.getDownloadDirLocation()
            changeDownloadFolderPreference.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        var intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                        intent.addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                    or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                        )
                        if (TransportUtils.intentCanBeHandled(intent)) {
                            dirSelectResultLauncher.launch(intent)
                        } else {
                            intent = Intent(requireContext(), FolderPicker::class.java)
                            intent.addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                            )
                            compatDirSelectResultLauncher.launch(intent)
                        }
                    } else {
                        val intent = Intent(requireContext(), FolderPicker::class.java)
                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                            intent.addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                            )
                        }
                        compatDirSelectResultLauncher.launch(intent)
                    }
                    false
                }
        }
    }

    private fun recursiveAddListener(pref: Preference) {
        val prefCount: Int = (pref as PreferenceCategory).preferenceCount
        for (i in 0 until prefCount) {
            val innerPref: Preference = pref.getPreference(i)
            if(innerPref is PreferenceCategory){
                recursiveAddListener(innerPref)
            }
            else{
                innerPref.onPreferenceChangeListener = prefChangedDelegate
            }
        }
    }

    private var compatDirSelectResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                if (data != null && data.extras != null && data.extras!!.containsKey("data")) {
                    val folderLocation = data.extras!!.getString("data")
                    val file = File(folderLocation)
                    if (file.isDirectory && PreferencesHandler.instance.saveDownloadFolder(
                            folderLocation
                        )
                    ) {
                        Toast.makeText(requireContext(), "Папка сохранена!", Toast.LENGTH_SHORT)
                            .show()
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

    private var dirSelectResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                if (data != null) {
                    val treeUri = data.data
                    if (treeUri != null) {
                        // проверю наличие файла
                        val dl = DocumentFile.fromTreeUri(App.instance, treeUri)
                        if (dl != null && dl.isDirectory) {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    App.instance.contentResolver.takePersistableUriPermission(
                                        treeUri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                    App.instance.contentResolver.takePersistableUriPermission(
                                        treeUri,
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    )
                                }
                                PreferencesHandler.instance.setDownloadDir(dl)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    requireContext(),
                                    "Не удалось выдать разрешения на доступ, попробуем другой метод",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val intent = Intent(requireContext(), FolderPicker::class.java)
                                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                                    intent.addFlags(
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                                or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                    )
                                }
                                compatDirSelectResultLauncher.launch(intent)
                            }
                        }
                    }
                }
            }
        }
}