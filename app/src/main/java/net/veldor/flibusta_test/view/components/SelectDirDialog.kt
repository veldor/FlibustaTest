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
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.DialogSelectDirBinding
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.util.TransportUtils
import net.veldor.flibusta_test.view.SelectDirActivity
import java.io.File

open class SelectDirDialog : DialogFragment() {

    private lateinit var mLayout: DialogSelectDirBinding
    private var mCallback: () -> Unit = { }

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
                                Log.d("surprise", "SelectDirDialog: 53 access confirmed")
                                PreferencesHandler.setModernRoot(dl)
                                PreferencesHandler.storageAccessDenied = false
                                dialog?.dismiss()
                                mCallback()
                                return@registerForActivityResult
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.d("surprise", "SelectDirDialog: 61 access promise error")
                                selectCompatDownloadDir()
                                return@registerForActivityResult
                            }
                        }
                    }
                    else{
                        Log.d("surprise", "SelectDirDialog: 68 tree url is null")
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
                            // save file as temporary selected dir
                            PreferencesHandler.setCompatRoot(file)
                            PreferencesHandler.storageAccessDenied = false
                            dialog?.dismiss()
                            mCallback()
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
                    mLayout.grantAccessLayout.visibility = View.GONE
                    mLayout.selectDirLayout.visibility = View.VISIBLE
                    return@registerForActivityResult
                }
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d("surprise", "SelectDirDialog: 103 dialog created")
        return activity?.let {
            mLayout = DialogSelectDirBinding.inflate(layoutInflater)
            if(isHaveRights()){
                mLayout.grantAccessLayout.visibility = View.GONE
            }
            else{
                mLayout.selectDirLayout.visibility = View.GONE
            }
            mLayout.grantAccessBtn.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    readStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            mLayout.selectDirBtn.setOnClickListener {
                selectDownloadDir()
            }
            mLayout.revokeAccessBtn.setOnClickListener {
                PreferencesHandler.storageAccessDenied = true
                Toast.makeText(requireContext(), getString(R.string.access_revoced_title), Toast.LENGTH_SHORT).show()
                dialog?.dismiss()
                mCallback()
            }
            AlertDialog.Builder(requireActivity(), R.style.dialogTheme)
                .setTitle(getString(R.string.select_dir_title))
                .setView(mLayout.root)
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        const val TAG: String = "SELECT DIR DIALOG"
    }

    protected fun isHaveRights(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun showNow(supportFragmentManager: FragmentManager, tag: String, callback: () -> Unit) {
        showNow(supportFragmentManager, tag)
        mCallback = callback
    }


    open fun selectDownloadDir() {
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
}