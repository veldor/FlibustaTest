package net.veldor.flibusta_test.view.search_fragment

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.util.TransportUtils
import net.veldor.flibusta_test.view.SelectDirActivity
import java.io.File

abstract class DirectorySelectFragment : Fragment() {

    private val readStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    mPreparedFunction()
                    return@registerForActivityResult
                }
            } else {
                showAccessCanceledDialog()
            }
        }

    private lateinit var mPreparedFunction: () -> Unit
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
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    App.instance.contentResolver.takePersistableUriPermission(
                                        treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                    App.instance.contentResolver.takePersistableUriPermission(
                                        treeUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    )
                                }
                                PreferencesHandler.setModernRoot(dl)
                                Log.d("surprise", "DirectorySelectFragment: 62 root selected")
                                mPreparedFunction()
                                return@registerForActivityResult
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.d("surprise", "DirectorySelectFragment: 67 root select error")
                                selectCompatDownloadDir()
                                return@registerForActivityResult
                            }
                        }
                    }
                }
            }
            showAccessCanceledDialog()
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
                            // save file as temporary selected dir
                            PreferencesHandler.setCompatRoot(file)
                            mPreparedFunction()
                            return@registerForActivityResult
                        }
                    }
                }
            }
            showAccessCanceledDialog()
        }

    private fun showAccessCanceledDialog() {
        AlertDialog.Builder(requireContext(), R.style.dialogTheme)
            .setTitle(getString(R.string.access_grant_error_title))
            .setMessage(getString(R.string.access_grant_error_message))
            .setPositiveButton(R.string.select_dir_title) { _, _ ->
                selectDownloadDir(mPreparedFunction)
            }
            .setNegativeButton(R.string.deny_storage_access_title) { _, _ ->
                PreferencesHandler.storageAccessDenied = true
                mPreparedFunction()
            }
            .show()
    }

    fun prepareToDownload(onPrepared: () -> Unit) {
        Log.d("surprise", "prepareToDownload 110:  launch prepare to download")
        mPreparedFunction = onPrepared
        if (isHaveRights()) {
            Log.d("surprise", "prepareToDownload 98:  storage write rights granted")
            if (checkDownloadDir()) {
                Log.d("surprise", "prepareToDownload 101:  can write in dir")
                onPrepared()
            } else {
                showDirSelectRequiredDialog(onPrepared)
            }
        } else {
            if (PreferencesHandler.storageAccessDenied) {
                onPrepared()
            } else {
                Log.d("surprise", "prepareToDownload 121:  no have write storage rights")
                requestStoragePermission { prepareToDownload(onPrepared) }
            }
        }
    }

    private fun checkDownloadDir(): Boolean {
        if (PreferencesHandler.storageAccessDenied) {
            return true
        }
        val currentDownloadDir = PreferencesHandler.rootDownloadDir
        return currentDownloadDir.canWrite()
    }

    private fun isHaveRights(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }


    private fun requestStoragePermission(onGranted: () -> Unit) {
        showStorageAccessRequiredDialog(onGranted)
    }

    fun selectDownloadDir(onPrepared: () -> Unit) {
        PreferencesHandler.storageAccessDenied = false
        mPreparedFunction = onPrepared
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

    private fun showDirSelectRequiredDialog(onPrepared: () -> Unit) {
        AlertDialog.Builder(requireContext(), R.style.dialogTheme)
            .setTitle(getString(R.string.access_required_title))
            .setMessage(getString(R.string.access_required_message))
            .setPositiveButton(R.string.select_dir_title) { _, _ ->
                selectDownloadDir(onPrepared)
            }
            .setNegativeButton(R.string.deny_storage_access_title) { _, _ ->
                PreferencesHandler.storageAccessDenied = true
                onPrepared()
            }
            .show()
    }

    private fun showStorageAccessRequiredDialog(onPrepared: () -> Unit) {
        AlertDialog.Builder(requireContext(), R.style.dialogTheme)
            .setTitle(getString(R.string.storage_access_required_title))
            .setMessage(getString(R.string.storage_access_required_message))
            .setPositiveButton(R.string.grant_access_message) { _, _ ->
                mPreparedFunction = onPrepared
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    readStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            .setNegativeButton(R.string.deny_storage_access_title) { _, _ ->
                PreferencesHandler.storageAccessDenied = true
                onPrepared()
            }
            .show()
    }
}
