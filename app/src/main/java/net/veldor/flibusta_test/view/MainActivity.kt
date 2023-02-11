package net.veldor.flibusta_test.view

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityMainBinding
import net.veldor.flibusta_test.databinding.SelectConnectionDialogBinding
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.handler.TorHandler
import net.veldor.flibusta_test.model.helper.NetworkHelper
import net.veldor.flibusta_test.model.view_model.MainViewModel
import net.veldor.flibusta_test.model.worker.LaunchTorWorker
import net.veldor.flibusta_test.view.components.TorBridgesSetupDialog
import net.veldor.flibusta_test.view.components.TorLoadProblemDialog
import net.veldor.tor_client.model.listeners.BootstrapLoadProgressListener

class MainActivity : AppCompatActivity(), BootstrapLoadProgressListener {

    private lateinit var mViewModel: MainViewModel
    private lateinit var mBinding: ActivityMainBinding
    private var isWindowActive: Boolean = false
    private var mActiveDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        if (PreferencesHandler.skipLoadScreen) {
            goToApp()
            finish()
        } else {
            mBinding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(mBinding.root)
            mViewModel = ViewModelProvider(this)[MainViewModel::class.java]
            setupUI()
            TorHandler.setBootstrapLoadProgressListener(this)
            setObservers()
            isWindowActive = true
            if (PreferencesHandler.checkUpdateOnStart) {
                mBinding.currentState.text = getString(R.string.checking_update_message)
                mViewModel.checkUpdate { haveUpdate ->
                    runOnUiThread {
                        if (haveUpdate) {
                            startActivity(Intent(this, CheckUpdateActivity::class.java))
                        } else {
                            establishConnection()
                        }
                    }
                }
            } else {
                establishConnection()
            }

            checkTorLoadTimer()
        }
    }

    private fun checkTorLoadTimer() {

    }

    override fun onResume() {
        super.onResume()
        isWindowActive = true
        mActiveDialog?.show()
    }

    override fun onPause() {
        isWindowActive = true
        super.onPause()
        mActiveDialog?.hide()
    }

    private fun setupUI() {
        mBinding.selectConnectionTypeBtn.setOnClickListener {
            showSelectConnectionTypeDialog()
        }

        mBinding.setBridgesBtn.setOnClickListener {
            val dialog = TorBridgesSetupDialog()
            TorBridgesSetupDialog.callback = null
            dialog.showNow(supportFragmentManager, TorBridgesSetupDialog.TAG)
        }

        mBinding.resetConnectionBtn.setOnClickListener {
            mViewModel.relaunch(this)
        }

        mBinding.continueInBackgroundBtn.setOnClickListener {
            goToApp()
        }

        mBinding.getTorStatusBtn.setOnClickListener {
            mViewModel.checkTorStatus { status ->
                runOnUiThread {
                    Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setObservers() {

        LaunchTorWorker.liveLaunchTime.observe(this) {
            mBinding.progressView.isIndeterminate = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mBinding.progressView.setProgress(it.toInt(), true)
            } else {
                mBinding.progressView.progress = it.toInt()
            }
        }

        LaunchTorWorker.liveStatus.observe(this) {
            when (it) {
                LaunchTorWorker.STATUS_WAITING -> {
                    mBinding.progressView.visibility = View.INVISIBLE
                    mBinding.currentState.text = getString(R.string.waiting_connection_state)
                }
                LaunchTorWorker.STATUS_LAUNCHED -> {
                    mBinding.progressView.visibility = View.VISIBLE
                    mBinding.currentState.text = getString(R.string.launched_state)
                    mBinding.progressView.isIndeterminate = true
                }
                LaunchTorWorker.STATUS_TIMEOUT -> {
                    mBinding.progressView.visibility = View.INVISIBLE
                    mBinding.currentState.text = getString(R.string.launch_timeout_state)
                    showBridgesLoadRequiredDialog()

                }
                LaunchTorWorker.STATUS_ERROR_LOAD_TELEGRAM_BRIDGES -> {
                    mViewModel.requestOfficialBridges(this)
                }
                LaunchTorWorker.STATUS_LAUNCH_INTERRUPTED -> {
                    mBinding.currentState.text = getString(R.string.launch_interrupted_state)
                }
                LaunchTorWorker.STATUS_CONNECTION_ERROR -> {
                    showConnectionErrorDialog()
                    mBinding.currentState.text = getString(R.string.connection_error_state)
                }
                LaunchTorWorker.STATUS_SUCCESS -> {
                    goToApp()
                }
            }
        }

        MainViewModel.liveOfficialBridgesCaptcha.observe(this) {
            if (it.first) {
                showCaptchaDialog(it.second!!.first, it.second!!.second)
            } else {
                // error load custom bridges

            }
        }
    }

    private fun showConnectionErrorDialog() {
        mActiveDialog?.dismiss()
        if (isWindowActive) {
            val builder = AlertDialog.Builder(this, R.style.dialogTheme)
                .setTitle(getString(R.string.onion_connection_error_title))
                .setMessage("Error connection to .onion mirror this time. You can retry connection")
            builder.setPositiveButton(getString(R.string.try_again_message)) { _, _ ->
                mViewModel.launchTor(this)
            }
            mActiveDialog = builder.show()
        }
    }

    private fun showBridgesLoadRequiredDialog() {
        val dialog = TorLoadProblemDialog()
        TorLoadProblemDialog.cb =
            {
                mViewModel.launchTor(this)
            }
        dialog.showNow(this.supportFragmentManager, TorLoadProblemDialog.TAG)
    }


    private fun showCaptchaDialog(first: Bitmap?, secretCode: String?) {
        mActiveDialog?.dismiss()
        if (isWindowActive) {
            val view = layoutInflater.inflate(R.layout.captcha_dialog, null)
            (view.findViewById<ImageView>(R.id.captchaView)).setImageBitmap(first)
            val builder = AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Send") { _, _ ->
                    val textView = view.findViewById<EditText>(R.id.captchaText)
                    val parsedValue = textView.text.toString()
                    if (parsedValue.isNotEmpty()) {
                        mActiveDialog = null
                        mViewModel.sendCaptchaAnswer(parsedValue, secretCode!!, this)
                    }
                }
            mActiveDialog = builder.show()
        }
    }

    private fun establishConnection() {
        val currentStatus = LaunchTorWorker.liveStatus.value
        Log.d("surprise", "establishConnection 166:  current status is $currentStatus")
        if (currentStatus == LaunchTorWorker.STATUS_WAITING) {
            // check connection type first. If it unspecified- show dialog window to check it
            if (PreferencesHandler.connectionType == PreferencesHandler.CONNECTION_MODE_UNSPECIFIED) {
                showSelectConnectionTypeDialog()
            } else {
                if (PreferencesHandler.connectionType == PreferencesHandler.CONNECTION_MODE_VPN && !NetworkHelper().isVpnConnected() && PreferencesHandler.showSwitchConnectionHint) {
                    showEnableTorDialog()
                } else {
                    startTorIfRequired()
                }
            }
        } else if (currentStatus == LaunchTorWorker.STATUS_TIMEOUT) {
            showBridgesLoadRequiredDialog()
        }
    }

    @SuppressLint("InflateParams")
    private fun showSelectConnectionTypeDialog() {
        val binding = SelectConnectionDialogBinding.inflate(layoutInflater)
        binding.connectionTypeRadioGroup.setOnCheckedChangeListener { _, i ->
            if (i == R.id.useTorRadio) {
                binding.connectionHintView.setText(R.string.use_tor_hint)
            } else {
                binding.connectionHintView.setText(R.string.use_vpn_hint)
            }
        }
        if (NetworkHelper().isVpnConnected()) {
            binding.useTorRadio.isChecked = true
        } else {
            binding.useTorRadio.isChecked = true
        }
        val dialogBuilder = AlertDialog.Builder(this, R.style.dialogTheme)
        dialogBuilder.setView(binding.root)
            .setCancelable(false)
            .setPositiveButton(R.string.set) { _, _ ->
                // save selection and continue
                when (binding.connectionTypeRadioGroup.checkedRadioButtonId) {
                    R.id.useVpnRadio -> {
                        PreferencesHandler.connectionType = PreferencesHandler.CONNECTION_MODE_VPN
                    }
                    R.id.useTorRadio -> {
                        PreferencesHandler.connectionType = PreferencesHandler.CONNECTION_MODE_TOR
                    }
                }
                startTorIfRequired()
            }
            .show()
    }

    private fun showEnableTorDialog() {
        AlertDialog.Builder(this, R.style.dialogTheme)
            .setMessage(getString(R.string.enable_tor_message))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                PreferencesHandler.connectionType = PreferencesHandler.CONNECTION_MODE_TOR
                startTorIfRequired()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                startTorIfRequired()
            }
            .setNeutralButton(getString(R.string.do_not_show_again_message)) { _, _ ->
                PreferencesHandler.showSwitchConnectionHint = false
                startTorIfRequired()
            }
            .show()
    }

    private fun startTorIfRequired() {
        if (PreferencesHandler.connectionType == PreferencesHandler.CONNECTION_MODE_TOR) {
            mViewModel.launchTor(this)
        } else {
            // simple go to app
            goToApp()
        }
    }

    private fun goToApp() {
        val targetActivityIntent = Intent(this, SearchActivity::class.java)
        if (intent.data != null) {
            targetActivityIntent.putExtra(SearchActivity.EXTERNAL_LINK, intent.data.toString())
        }
        targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(targetActivityIntent)
        finish()
    }

    override fun tick(totalSeconds: Int, leftSeconds: Int, lastBootstrapLog: String?) {
        if (leftSeconds == totalSeconds) {
            mBinding.progressView.isIndeterminate = true
        }
        if (totalSeconds - leftSeconds == 0) {
            mBinding.progressView.isIndeterminate = true
        }
        runOnUiThread {
            mBinding.progressView.isIndeterminate = false
            mBinding.progressView.max = totalSeconds
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mBinding.progressView.setProgress(totalSeconds - leftSeconds, true)
            } else {
                mBinding.progressView.progress = totalSeconds - leftSeconds
            }

            mBinding.currentState.text = lastBootstrapLog
        }
    }
}