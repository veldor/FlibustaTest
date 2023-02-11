package net.veldor.flibusta_test.view.components

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.DialogBackupPreferencesBinding
import net.veldor.flibusta_test.model.helper.BookActionsHelper
import net.veldor.flibusta_test.model.util.TransportUtils
import net.veldor.flibusta_test.model.view_model.PreferencesBackupViewModel
import net.veldor.flibusta_test.view.SelectDirActivity
import java.io.*
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*

class BackupPreferencesDialog : SelectDirDialog() {

    private lateinit var mBackupFile: File


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
                        if (dl != null && dl.isDirectory && dl.canWrite()) {
                            try {
                                val sdf = SimpleDateFormat("yyyy/MM/dd HH-mm-ss", Locale.ENGLISH)
                                val filename = "Резервная копия Flibusta downloader от " + sdf.format(
                                    Date()
                                )
                                val previousFileVersion =
                                    dl.findFile("$filename.zip")
                                if (previousFileVersion != null && previousFileVersion.exists()) {
                                    previousFileVersion.delete()
                                }
                                val destinationFile =
                                    dl.createFile(
                                        "application/zip",
                                        "$filename.zip"
                                    )
                                val uri = destinationFile!!.uri
                                val stream = App.instance.contentResolver.openOutputStream(uri)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    Files.copy(mBackupFile.toPath(), stream)
                                } else {
                                    mBackupFile.inputStream().copyTo(stream!!)
                                }
                                dialog?.dismiss()
                                return@registerForActivityResult
                            } catch (e: Exception) {
                                selectCompatDownloadDir()
                                return@registerForActivityResult
                            }
                        }
                    }
                }
            }
        }

    private var compatDirSelectResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                if (data != null && data.extras != null && data.extras!!.containsKey("data")) {
                    val folderLocation = data.extras!!.getString("data")
                    if (folderLocation != null) {
                        val file = File(folderLocation)
                        if (file.exists() && file.isDirectory && file.canWrite()) {
                            val sdf = SimpleDateFormat("yyyy/MM/dd HH-mm-ss", Locale.ENGLISH)
                            val filename = "Резервная копия Flibusta downloader от " + sdf.format(
                                Date()
                            )
                            val dst = File(folderLocation, "$filename.zip")

                            val `is`: InputStream = FileInputStream(mBackupFile)
                            `is`.use { `in` ->
                                val out: OutputStream = FileOutputStream(dst)
                                out.use { o ->
                                    // Transfer bytes from in to out
                                    val buf = ByteArray(1024)
                                    var len: Int
                                    while (`in`.read(buf).also { len = it } > 0) {
                                        o.write(buf, 0, len)
                                    }
                                }
                            }
                            dialog?.dismiss()
                            return@registerForActivityResult
                        }
                    }
                }
            }
        }


    private val readStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    selectDownloadDir()
                    return@registerForActivityResult
                }
            }
        }


    override fun selectDownloadDir() {
        val intent: Intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
            if (TransportUtils.intentCanBeHandled(intent)) {
                dirSelectResultLauncher.launch(intent)
            } else {
                // try compat option
                selectCompatDownloadDir()

            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val file = Environment.getExternalStorageDirectory()
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            ).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
                .putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                .putExtra(Intent.EXTRA_TITLE, file.name)
            dirSelectResultLauncher.launch(intent)
        } else {
            selectCompatDownloadDir()
        }
    }

    private fun selectCompatDownloadDir() {
        val intent = Intent(requireContext(), SelectDirActivity::class.java)
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
        compatDirSelectResultLauncher.launch(intent)
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val binding = DialogBackupPreferencesBinding.inflate(layoutInflater)
            val viewModel = ViewModelProvider(this)[PreferencesBackupViewModel::class.java]
            binding.doBackupBtn.setOnClickListener {
                it.isEnabled = false
                Toast.makeText(requireContext(), "Start backup preferences", Toast.LENGTH_SHORT).show()
                viewModel.doBackup(
                    binding.backupBasePreferencesSwitcher.isChecked,
                    binding.backupDownloadedBooksSwitcher.isChecked,
                    binding.backupReadBooksSwitcher.isChecked,
                    binding.backupSearchAutofillSwitcher.isChecked,
                    binding.backupBookmarksListSwitcher.isChecked,
                    binding.backupSubscriptionsListSwitcher.isChecked,
                    binding.backupFiltersListSwitcher.isChecked,
                    binding.backupDownloadScheduleSwitcher.isChecked,
                ){ backupFile: File ->
                    mBackupFile = backupFile
                    Log.d(
                        "surprise",
                        "BackupPreferencesDialog: 35 saved ${backupFile.absoluteFile}"
                    )
                    binding.shareFileBtn.isEnabled = true
                    binding.saveFileBtn.isEnabled = true
                    binding.shareFileBtn.setOnClickListener {
                        BookActionsHelper.shareBook(backupFile)
                        dialog?.dismiss()
                    }
                    binding.saveFileBtn.setOnClickListener {
                        if(isHaveRights()){
                            selectDownloadDir()
                        }
                        else{
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                readStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            }
                        }
                    }
                }
            }
            val builder = AlertDialog.Builder(it, R.style.dialogTheme)
            builder.setView(binding.root)
            builder.setTitle(getString(R.string.backup_preferences_title))
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        const val TAG: String = "backup preferences"
    }


}