package net.veldor.flibusta_test.ui

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityMainBinding
import net.veldor.flibusta_test.model.handler.NetworkHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.handler.TorHandler
import net.veldor.flibusta_test.model.view_model.StartViewModel
import net.veldor.flibusta_test.ui.different_fragments.TorLogFragment

class MainActivity : AppCompatActivity() {
    private var mCdt: CountDownTimer? = null
    private var mProgressCounter: Int = 0
    private var mConfirmExit: Long = -1
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var backdropFragment: TorLogFragment? = null
    private var link: Uri? = null
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: StartViewModel

    private var firstUseLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.launchConnection()
                resetTimer()
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, getString(R.string.must_finish_setup_message), Toast.LENGTH_SHORT).show()
                viewModel.launchConnection()
                resetTimer()
            }
        }

    private var connectionSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.launchConnection()
                resetTimer()
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                launchFirstRunSettings()
            }
        }

    private fun launchFirstRunSettings() {
        firstUseLauncher.launch(Intent(this, FirstUseGuideActivity::class.java))
    }

    private var setBridgesLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.launchConnection()
                resetTimer()
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                viewModel.launchConnection()
                resetTimer()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // проверю наличие ссылки для открытия страницы результатов
        if (intent.data != null) {
            link = intent.data
        }
        viewModel = ViewModelProvider(this).get(StartViewModel::class.java)
        //viewModel.startTor(this)
        // если приложение используется первый раз- запущу стартовый гайд
        if (PreferencesHandler.instance.firstUse) {
            val targetActivityIntent = Intent(this, FirstUseGuideActivity::class.java)
            firstUseLauncher.launch(targetActivityIntent)
        } else {
            viewModel.launchConnection()
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.rootView)

        setupInterface()
        setupObservers()
    }

    private fun setupInterface() {

        //force start
        binding.testStartApp.setOnClickListener {
            readyToGo()
        }

        // app version
        binding.appVersion.text = PreferencesHandler.instance.appVersion

        // hardware acceleration
        if (PreferencesHandler.instance.hardwareAcceleration) {
            // проверю аппаратное ускорение
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
        }

        // setup theme
        if (PreferencesHandler.instance.isEInk) {

            binding.currentStateProgress.rotation = 0F
            binding.currentStateProgress.background =
                ResourcesCompat.getDrawable(resources, R.drawable.eink_progressbar_background, null)
            binding.currentStateProgress.progressDrawable =
                ResourcesCompat.getDrawable(resources, R.drawable.eink_progressbar, null)


            // prepare window for eInk
            checkWiFiEnabled()
            binding.appVersion.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.e_ink_text_color,
                    theme
                )
            )
            binding.appVersion.setShadowLayer(0F, 0F, 0F, R.color.transparent)

            binding.testStartApp.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.e_ink_text_color,
                    theme
                )
            )
            binding.testStartApp.setShadowLayer(0F, 0F, 0F, R.color.transparent)


            binding.showTorLogBtn.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.e_ink_text_color,
                    theme
                )
            )
            binding.showTorLogBtn.setShadowLayer(0F, 0F, 0F, R.color.transparent)
        } else {
            if (!PreferencesHandler.instance.isPicHide()) {
                // назначу фон
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    binding.rootView.background = ContextCompat.getDrawable(this, R.drawable.back_3)
                } else {
                    binding.rootView.background =
                        ResourcesCompat.getDrawable(resources, R.drawable.back_3, theme)
                }
            }
        }

        binding.showTorLogBtn.setOnClickListener {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        if (!PreferencesHandler.instance.useTor) {
            binding.connectionTypeSwitcher.isChecked = true
            binding.showTorLogBtn.visibility = View.VISIBLE
        } else {
            binding.showTorLogBtn.visibility = View.GONE
        }
        binding.connectionTypeSwitcher.setOnCheckedChangeListener { _, b ->
            PreferencesHandler.instance.useTor = !b
            viewModel.relaunchConnection(this)
            resetTimer()
        }
        configureBackdrop()
    }

    private fun setupObservers() {
        // check launch state
        viewModel.launchState.observe(this) {
            when (it) {
                StartViewModel.STAGE_AWAITING -> {
                    binding.currentState.text = getString(R.string.prepare_connection)
                }
                StartViewModel.STAGE_PING_LIBRARY -> {
                    binding.currentState.text = getString(R.string.state_ping_library)
                }
                StartViewModel.STAGE_CHECK_LIBRARY_CONNECTION -> {
                    binding.currentState.text = getString(R.string.state_check_library_connection)
                }
                StartViewModel.STATE_LIBRARY_SERVER_UNAVAILABLE -> {
                    binding.currentState.text = getString(R.string.server_check_error)
                    showServerCheckErrorDialog()
                }
                StartViewModel.STATE_LIBRARY_SERVER_AVAILABLE -> {
                    binding.currentState.text = getString(R.string.state_flibusta_ping_success)
                }
                StartViewModel.STAGE_LAUCH_CLIENT -> {
                    binding.currentState.text = getString(R.string.state_launch_tor)
                }
                StartViewModel.STATE_LIBRARY_CONNECTION_CHECK_FAILED -> {
                    binding.currentState.text = getString(R.string.state_library_connection_falied)
                    showLibraryConnectionErrorDialog()
                }
                StartViewModel.STATE_TOR_NOT_STARTS -> {
                    binding.currentState.text = getString(R.string.state_tor_launch_error)
                    showTorLaunchErrorDialog()
                }
                StartViewModel.STATE_LAUNCH_SUCCESSFUL -> {
                    binding.currentState.text = getString(R.string.success_message)
                    readyToGo()
                }
            }
        }
    }

    private fun showTorLaunchErrorDialog() {
        AlertDialog.Builder(this, R.style.dialogTheme)
            .setTitle(getString(R.string.state_tor_launch_error))
            .setMessage(getString(R.string.tor_launch_error_message))
            .setPositiveButton(getString(R.string.retry_launch_title)) { _, _ ->
                viewModel.launchConnection()
                resetTimer()
            }
            .setNegativeButton(getString(R.string.go_to_connection_settings_message)) { _, _ ->
                connectionSettingsLauncher.launch(
                    Intent(
                        this,
                        ConnectivityGuideActivity::class.java
                    )
                )
            }
            .setNeutralButton(getString(R.string.setup_tor_custom_bridges)) { _, _ ->
                setBridgesLauncher.launch(Intent(this, SetTorBridgesActivity::class.java))
            }
            .show()
    }

    private fun showLibraryConnectionErrorDialog() {
        val message = if (PreferencesHandler.instance.useTor) {
            getString(R.string.tor_error_library_connection_message)
        } else {
            getString(R.string.vpn_error_library_connection_message)
        }
        AlertDialog.Builder(this, R.style.dialogTheme)
            .setTitle(getString(R.string.no_library_connection_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.retry_launch_title)) { _, _ ->
                viewModel.launchConnection()
                resetTimer()
            }
            .setNegativeButton(getString(R.string.go_to_connection_settings_message)) { _, _ ->
                connectionSettingsLauncher.launch(
                    Intent(
                        this,
                        ConnectivityGuideActivity::class.java
                    )
                )
            }
            .show()
    }

    private fun showServerCheckErrorDialog() {
        AlertDialog.Builder(this, R.style.dialogTheme)
            .setTitle(getString(R.string.server_check_error))
            .setMessage(getString(R.string.server_check_error_text))
            .setPositiveButton(getString(R.string.retry_launch_title)) { _, _ ->
                viewModel.launchConnection()
                resetTimer()
            }
            .setNegativeButton(getString(R.string.skip_test_btn_text)) { _, _ ->
                viewModel.launchConnection(
                    true
                )
                resetTimer()
            }
            .show()
    }

    private fun readyToGo() {
        Handler().postDelayed({
            // проверю очередь скачивания. Если она не пуста- предложу продолжить закачку
            // проверю, не запущено ли приложение с помощью интента. Если да- запущу программу в webView режиме
            val targetActivityIntent = Intent(this, BrowserActivity::class.java)
            if (link != null) {
                targetActivityIntent.putExtra(BrowserActivity.EXTERNAL_LINK, link.toString())
            }
            targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(targetActivityIntent)
            finish()
        }, 500)
    }

    override fun onResume() {
        super.onResume()
        checkConnectionOptions()
        startTimer()
    }

    override fun onPause() {
        super.onPause()
        mCdt?.cancel()
    }

    private fun checkWiFiEnabled() {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) {
            showEnableWifiDialog()
        }
    }

    private fun showEnableWifiDialog() {
        if (!this@MainActivity.isFinishing) {
            val dialogBuilder = AlertDialog.Builder(this, R.style.dialogTheme)
            dialogBuilder
                .setTitle(getString(R.string.enable_wifi_title))
                .setMessage(getString(R.string.wifi_enable_message))
                .setPositiveButton(getString(android.R.string.ok)) { _: DialogInterface?, _: Int ->
                    val wifiManager = applicationContext.getSystemService(
                        WIFI_SERVICE
                    ) as WifiManager
                    wifiManager.isWifiEnabled = true
                }
                .setNegativeButton(getString(android.R.string.cancel)) { dialogInterface: DialogInterface, _: Int -> dialogInterface.dismiss() }
            val dialog = dialogBuilder.create()
            lifecycle.addObserver(DialogDismissLifecycleObserver(dialog))
            dialog.show()
        }
    }

    // observer
    @Suppress("unused")
    class DialogDismissLifecycleObserver(private var dialog: Dialog?) : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun onPause() {
            dialog?.dismiss()
            dialog = null
        }
    }

    private fun checkConnectionOptions() {
        if (PreferencesHandler.instance.showConnectionOptions) {
            if (NetworkHandler().isVpnConnected()) {
                if (PreferencesHandler.instance.useTor) {
                    showDisableTorDialog()
                }
            } else if (!PreferencesHandler.instance.useTor) {
                showEnableTorDialog()
            }
        }
    }


    private fun showDisableTorDialog() {
        AlertDialog.Builder(this, R.style.dialogTheme)
            .setMessage(getString(R.string.disable_tor_message))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                PreferencesHandler.instance.useTor = false
                binding.connectionTypeSwitcher.performClick()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .setNeutralButton(getString(R.string.do_not_show_again_message)) { _, _ ->
                PreferencesHandler.instance.showConnectionOptions = false
            }
            .show()
    }

    private fun showEnableTorDialog() {
        AlertDialog.Builder(this, R.style.dialogTheme)
            .setMessage(getString(R.string.enable_tor_message))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                PreferencesHandler.instance.useTor = true
                binding.connectionTypeSwitcher.performClick()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .setNeutralButton(getString(R.string.do_not_show_again_message)) { _, _ ->
                PreferencesHandler.instance.showConnectionOptions = false
            }
            .show()
    }


    private fun configureBackdrop() {
// Get the download state fragment reference
        backdropFragment =
            supportFragmentManager.findFragmentById(R.id.torLogFragment) as TorLogFragment?
        backdropFragment?.let {
            // Get the BottomSheetBehavior from the fragment view
            BottomSheetBehavior.from(it.requireView()).let { bsb ->
                // Set the initial state of the BottomSheetBehavior to HIDDEN
                bsb.state = BottomSheetBehavior.STATE_HIDDEN
                // Set the reference into class attribute (will be used latter)
                bottomSheetBehavior = bsb
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (bottomSheetBehavior != null && bottomSheetBehavior?.state != BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                return true
            }
            if (mConfirmExit != 0L) {
                if (mConfirmExit > System.currentTimeMillis() - 3000) {
                    // выйду из приложения
                    val startMain = Intent(Intent.ACTION_MAIN)
                    startMain.addCategory(Intent.CATEGORY_HOME)
                    startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(startMain)
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.press_back_again_for_exit_title),
                        Toast.LENGTH_SHORT
                    ).show()
                    mConfirmExit = System.currentTimeMillis()
                }
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.press_back_again_for_exit_title),
                    Toast.LENGTH_SHORT
                ).show()
                mConfirmExit = System.currentTimeMillis()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        mCdt?.cancel()
    }

    // таймер отсчёта прогресса запуска
    private fun startTimer() {
        val timeLeft = viewModel.getTimeFromLastLaunch()
        binding.currentStateProgress.max = TorHandler.TOTAL_SECONDS_PER_TOR_STARTUP
        mProgressCounter = if (timeLeft > 0) {
            timeLeft / 1000
        } else {
            0
        }
        val waitingTime =
            TorHandler.TOTAL_SECONDS_PER_TOR_STARTUP * 1000 - timeLeft // 3 minute in milli seconds
        if (mProgressCounter < TorHandler.TOTAL_SECONDS_PER_TOR_STARTUP) {
            mCdt = object : CountDownTimer(waitingTime.toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    mProgressCounter++
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        binding.currentStateProgress.progress = mProgressCounter
                    }
                }

                override fun onFinish() {
                    // tor не загрузился, покажу сообщение с предложением подождать или перезапустить процесс
                    showTooLongConnectionDialog()
                }
            }
            mCdt?.start()
        } else {
            showTooLongConnectionDialog()
        }
    }

    private fun showTooLongConnectionDialog() {
        AlertDialog.Builder(this, R.style.dialogTheme)
            .setTitle(getString(R.string.too_long_launch_title))
            .setMessage(getString(R.string.too_long_launch_message))
            .setPositiveButton(R.string.wait_more_title) { _, _ ->
                viewModel.clearTimeFromLastLaunch()
                resetTimer()
            }
            .setNegativeButton(getString(R.string.retry_launch_title)) { _, _ ->
                viewModel.relaunchConnection(this)
                resetTimer()
            }
            .show()
    }

    private fun resetTimer(){
        mCdt?.cancel()
        mProgressCounter = 0
        startTimer()
    }
}