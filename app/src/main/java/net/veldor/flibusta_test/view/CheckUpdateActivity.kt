package net.veldor.flibusta_test.view

import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityUpdaterBinding
import net.veldor.flibusta_test.model.handler.UpdateHandler
import net.veldor.flibusta_test.model.listener.CheckUpdateListener
import net.veldor.flibusta_test.model.selection.UpdateInfo
import net.veldor.flibusta_test.model.view_model.UpdaterViewModel
import java.util.*

class CheckUpdateActivity : AppCompatActivity(), CheckUpdateListener {

    private lateinit var viewModel: UpdaterViewModel
    private lateinit var binding: ActivityUpdaterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityUpdaterBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[UpdaterViewModel::class.java]
        viewModel.listener = this
        setContentView(binding.root)
        binding.checkUpdateBtn.setOnClickListener {
            startCheckUpdate()
        }
        if (savedInstanceState?.getBoolean("auto_update_done") == true) {
            binding.progressView.visibility = View.GONE
            binding.checkingUpdateTextView.visibility = View.GONE
        }
        else{
            viewModel.checkUpdate()
        }

        UpdateHandler.liveCurrentDownloadProgress.observe(this){
            if(it >= 0){
                val currentProgress = (it.toDouble() / UpdateHandler.updateInfo!!.size.toDouble()) * 100
                binding.updateDownloadProgressView.visibility = View.VISIBLE
                binding.updateDownloadStateView.visibility = View.VISIBLE
                binding.updateDownloadStateView.text = String.format(Locale.ENGLISH, getString(R.string.update_download_percentage_template), it)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    binding.updateDownloadProgressView.setProgress(currentProgress.toInt(), true)
                }
                else{
                    binding.updateDownloadProgressView.progress = currentProgress.toInt()
                }
            }
            else{
                binding.updateDownloadProgressView.visibility = View.INVISIBLE
                binding.updateDownloadProgressView.progress = 0
                binding.updateDownloadStateView.visibility = View.INVISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.listener = null
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putBoolean("auto_update_done", true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("auto_update_done", true)
    }

    override fun haveUpdate(updateInfo: UpdateInfo?) {
        runOnUiThread {
            binding.progressView.visibility = View.GONE
            binding.checkingUpdateTextView.visibility = View.GONE
            binding.checkUpdateBtn.isEnabled = true
            if(updateInfo != null){
                binding.updateAvailableView.visibility = View.VISIBLE
                binding.installUpdateBtn.visibility = View.VISIBLE
                binding.updateDescription.visibility = View.VISIBLE
                binding.newUpdateVersion.visibility = View.VISIBLE
                binding.newUpdateVersion.text = updateInfo.title
                binding.updateDescription.text = updateInfo.body

                binding.installUpdateBtn.setOnClickListener {
                    viewModel.installUpdate(updateInfo)
                    it.isEnabled = false
                    binding.checkUpdateBtn.isEnabled = false
                }
            }
            else{
                AlertDialog.Builder(this, R.style.dialogTheme)
                    .setTitle(R.string.no_update_available)
                    .setMessage(getString(R.string.latest_version_message))
                    .setPositiveButton(R.string.retry_title){_,_->
                        startCheckUpdate()
                    }
                    .show()
            }
        }
    }

    override fun checkError(message: String?) {
        runOnUiThread {
            binding.checkUpdateBtn.isEnabled = true
            runOnUiThread {
                AlertDialog.Builder(this, R.style.dialogTheme)
                    .setTitle(R.string.check_update_errror)
                    .setMessage(message)
                    .setPositiveButton(R.string.retry_title){_,_->
                        startCheckUpdate()
                    }
                    .show()
            }
        }
    }

    private fun startCheckUpdate() {
        binding.updateAvailableView.visibility = View.INVISIBLE
        binding.installUpdateBtn.visibility = View.INVISIBLE
        binding.updateDescription.visibility = View.INVISIBLE
        binding.newUpdateVersion.visibility = View.INVISIBLE
        binding.progressView.visibility = View.VISIBLE
        binding.checkingUpdateTextView.visibility = View.VISIBLE
        viewModel.checkUpdate()
    }

}