package net.veldor.flibusta_test.view.components

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.DialogRestorePreferencesBinding
import net.veldor.flibusta_test.model.util.TransportUtils
import net.veldor.flibusta_test.model.view_model.PreferencesBackupViewModel
import net.veldor.flibusta_test.view.BaseActivity
import net.veldor.flibusta_test.view.SelectDirActivity
import java.io.File

class RestorePreferencesDialog : SelectDirDialog() {

    private lateinit var viewModel: PreferencesBackupViewModel
    private lateinit var binding: DialogRestorePreferencesBinding
    private var modernFile: DocumentFile? = null
    private var compatFile: File? = null


    private var compatFileSelectResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                if (data != null && data.extras != null && data.extras!!.containsKey("data")) {
                    val fileLocation = data.extras!!.getString("data")
                    if (fileLocation != null) {
                        compatFile = File(fileLocation)
                        if (compatFile != null && compatFile?.exists() == true) {
                            // save file as temporary selected dir
                            val options = viewModel.checkReserve(compatFile!!)
                            enableRestoreOptions(options)
                            return@registerForActivityResult
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
                            modernFile = DocumentFile.fromSingleUri(App.instance, uri)
                            if (modernFile != null) {
                                val options = viewModel.checkReserve(modernFile!!)
                                enableRestoreOptions(options)
                            }
                        }
                    }
                } else {
                    if (result != null) {
                        val data: Intent? = result.data
                        if (data != null && data.extras != null && data.extras!!.containsKey("data")) {
                            val folderLocation = data.extras!!.getString("data")
                            if(folderLocation != null){
                                compatFile = File(folderLocation)
                                if (compatFile?.isFile == true) {
                                    val options = viewModel.checkReserve(compatFile!!)
                                    enableRestoreOptions(options)
                                }
                            }
                        }
                    }
                }
            }
        }

    private fun enableRestoreOptions(options: BooleanArray) {
        if(options[0]){
            binding.backupBasePreferencesSwitcher.isChecked = true
            binding.backupBasePreferencesSwitcher.isEnabled = true
        }
        else{
            binding.backupBasePreferencesSwitcher.isChecked = false
            binding.backupBasePreferencesSwitcher.isEnabled = false
        }
        if(options[1]){
            binding.backupDownloadedBooksSwitcher.isChecked = true
            binding.backupDownloadedBooksSwitcher.isEnabled = true
        }
        else{
            binding.backupDownloadedBooksSwitcher.isEnabled = false
            binding.backupDownloadedBooksSwitcher.isChecked = false
        }
        if(options[2]){
            binding.backupReadBooksSwitcher.isEnabled = true
            binding.backupReadBooksSwitcher.isChecked = true
        }
        else{
            binding.backupReadBooksSwitcher.isEnabled = false
            binding.backupReadBooksSwitcher.isChecked = false
        }
        if(options[3]){
            binding.backupSearchAutofillSwitcher.isChecked = true
            binding.backupSearchAutofillSwitcher.isEnabled = true
        }
        else{
            binding.backupSearchAutofillSwitcher.isEnabled = false
            binding.backupSearchAutofillSwitcher.isChecked = false
        }
        if(options[4]){
            binding.backupBookmarksListSwitcher.isChecked = true
            binding.backupBookmarksListSwitcher.isEnabled = true
        }
        else{
            binding.backupBookmarksListSwitcher.isEnabled = false
            binding.backupBookmarksListSwitcher.isChecked = false
        }
        if(options[5]){
            binding.backupSubscriptionsListSwitcher.isChecked = true
            binding.backupSubscriptionsListSwitcher.isEnabled = true
        }
        else{
            binding.backupSubscriptionsListSwitcher.isEnabled = false
            binding.backupSubscriptionsListSwitcher.isChecked = false
        }
        if(options[6]){
            binding.backupFiltersListSwitcher.isChecked = true
            binding.backupFiltersListSwitcher.isEnabled = true
        }
        else{
            binding.backupFiltersListSwitcher.isEnabled = false
            binding.backupFiltersListSwitcher.isChecked = false
        }
        if(options[7]){
            binding.backupDownloadScheduleSwitcher.isChecked = true
            binding.backupDownloadScheduleSwitcher.isEnabled = true
        }
        else{
            binding.backupDownloadScheduleSwitcher.isEnabled = false
            binding.backupDownloadScheduleSwitcher.isChecked = false
        }
        binding.restoreBackupBtn.isEnabled = true
    }


    private val readStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    selectFile()
                    return@registerForActivityResult
                }
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            binding = DialogRestorePreferencesBinding.inflate(layoutInflater)
            binding.selectFileBtn.setOnClickListener {
                // select file for restore
                if(isHaveRights()){
                    selectFile()
                }
                else{
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        readStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }
            }
            binding.restoreBackupBtn.setOnClickListener {
                if(modernFile != null){
                    viewModel.restoreReserve(
                        modernFile!!,
                        binding.backupBasePreferencesSwitcher.isChecked,
                        binding.backupDownloadedBooksSwitcher.isChecked,
                        binding.backupReadBooksSwitcher.isChecked,
                        binding.backupSearchAutofillSwitcher.isChecked,
                        binding.backupBookmarksListSwitcher.isChecked,
                        binding.backupSubscriptionsListSwitcher.isChecked,
                        binding.backupFiltersListSwitcher.isChecked,
                        binding.backupDownloadScheduleSwitcher.isChecked
                    ){
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), getString(R.string.preferences_restored_message), Toast.LENGTH_SHORT).show()
                            Handler(requireActivity().mainLooper).postDelayed(BaseActivity.ResetApp(), 500)
                        }
                    }
                }
                if(compatFile != null){
                    viewModel.restoreReserve(
                        compatFile!!,
                        binding.backupBasePreferencesSwitcher.isChecked,
                        binding.backupDownloadedBooksSwitcher.isChecked,
                        binding.backupReadBooksSwitcher.isChecked,
                        binding.backupSearchAutofillSwitcher.isChecked,
                        binding.backupBookmarksListSwitcher.isChecked,
                        binding.backupSubscriptionsListSwitcher.isChecked,
                        binding.backupFiltersListSwitcher.isChecked,
                        binding.backupDownloadScheduleSwitcher.isChecked
                    ){
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), getString(R.string.preferences_restored_message), Toast.LENGTH_SHORT).show()
                            Handler(requireActivity().mainLooper).postDelayed(BaseActivity.ResetApp(), 500)
                        }
                    }
                }
            }
            viewModel = ViewModelProvider(this)[PreferencesBackupViewModel::class.java]
            val builder = AlertDialog.Builder(it, R.style.dialogTheme)
            builder.setView(binding.root)
            builder.setTitle(getString(R.string.restore_preferences_title))
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun selectFile(){
        val intent: Intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Intent(Intent.ACTION_OPEN_DOCUMENT)
            } else {
                Intent(Intent.ACTION_GET_CONTENT)
            }
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        intent.type = "application/zip"
        if (TransportUtils.intentCanBeHandled(intent)) {
            restoreFileSelectResultLauncher.launch(intent)
        } else {
            val i = Intent(requireContext(), SelectDirActivity::class.java)
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                i.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            }
            compatFileSelectResultLauncher.launch(i)
        }
    }

    companion object {
        const val TAG: String = "restore preferences"
    }


}