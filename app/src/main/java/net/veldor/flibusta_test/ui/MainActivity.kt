package net.veldor.flibusta_test.ui

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityMainBinding
import net.veldor.flibusta_test.model.handler.GrammarHandler
import net.veldor.flibusta_test.model.handler.NetworkHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.utils.FlibustaChecker
import net.veldor.flibusta_test.model.view_model.StartViewModel
import net.veldor.flibusta_test.model.web.UniversalWebClient
import net.veldor.flibusta_test.model.worker.StartTorWorker
import net.veldor.flibusta_test.ui.different_fragments.TorLogFragment

class MainActivity : AppCompatActivity() {
    private var mConfirmExit: Long = -1
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var backdropFragment: TorLogFragment? = null
    private var link: Uri? = null
    private lateinit var binding: ActivityMainBinding
    private lateinit var errorSnackbar: Snackbar
    private lateinit var viewModel: StartViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // проверю наличие ссылки для открытия страницы результатов
        if (intent.data != null) {
            link = intent.data
        }
        // если приложение используется первый раз- запущу стартовый гайд
        if (PreferencesHandler.instance.firstUse) {
            val targetActivityIntent = Intent(this, FirstUseGuideActivity::class.java)
            targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(targetActivityIntent)
            finish()
        } else {
            //init view model
            viewModel = ViewModelProvider(this).get(StartViewModel::class.java)
            binding = ActivityMainBinding.inflate(layoutInflater)
            configureBackdrop()
            // check connection options
            checkConnectionOptions()
            setupUi()
            setupObservers()
            setContentView(binding.rootView)
        }
    }

    private fun setupObservers() {
        UniversalWebClient.connectionError.observe(this) {
            if (it != null) {
                notifyConnectionError()
            }
        }
        // observe loading staging
        viewModel.liveStage.observe(this) {
            when (it) {
                StartViewModel.STAGE_AWAITING -> {}
                StartViewModel.STAGE_FIRST -> {
                    binding.clientRunningProgress.visibility = View.VISIBLE
                }
                StartViewModel.STAGE_SECOND -> {
                    binding.clientRunningProgress.visibility = View.INVISIBLE
                    binding.testFlibustaIsUpProgress.visibility = View.VISIBLE
                }
                StartViewModel.STAGE_THIRD -> {
                    binding.testFlibustaIsUpProgress.visibility = View.INVISIBLE
                    binding.connectionTestProgress.visibility = View.VISIBLE
                }
                StartViewModel.STAGE_READY -> {
                    binding.connectionTestProgress.visibility = View.INVISIBLE
                    readyToGo()
                }
            }
        }

        // буду отслеживать завершение работы запуска TOR
        val info =
            WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(StartTorWorker.TAG)
        info.observe(this) { list ->
            list.forEach {
                if (it.state == WorkInfo.State.SUCCEEDED) {
                    viewModel.checkTor()
                }
            }
        }

        viewModel.liveTorWorks.observe(this) {
            if (it) {
                binding.clientProgressText.setTextColor(
                    ResourcesCompat.getColor(resources, R.color.white, theme)
                )
                // tor client loaded
                binding.clientProgressText.text = GrammarHandler.getColoredString(
                    getString(R.string.tor_loaded),
                    Color.parseColor("#0c6126"),
                    this
                )
                binding.clientRunningProgress.visibility = View.INVISIBLE
                binding.testFlibustaIsUpText.visibility = View.VISIBLE
                binding.testFlibustaIsUpProgress.visibility = View.VISIBLE
                viewModel.checkServer()
            }
        }

        viewModel.flibustaServerCheckState.observe(this) {
            when (it) {
                FlibustaChecker.STATE_PASSED -> {
                    binding.testFlibustaIsUpProgress.visibility = View.INVISIBLE
                    binding.testFlibustaIsUpText.setTextColor(
                        ResourcesCompat.getColor(resources, R.color.white, theme)
                    )
                    binding.testFlibustaIsUpText.text =
                        GrammarHandler.getColoredString(
                            getString(R.string.cant_check_flibusta_message),
                            Color.parseColor("#5403ad"),
                            this
                        )
                    flibustaServerChecked()
                }
                FlibustaChecker.STATE_AVAILABLE -> {
                    binding.testFlibustaIsUpText.setTextColor(
                    ResourcesCompat.getColor(resources, R.color.white, theme)
                )
                    binding.testFlibustaIsUpProgress.visibility = View.INVISIBLE
                    binding.testFlibustaIsUpText.text =
                        GrammarHandler.getColoredString(
                            getString(R.string.flibusta_server_is_up),
                            Color.parseColor("#0c6126"),
                            this
                        )
                    flibustaServerChecked()
                }
                FlibustaChecker.STATE_UNAVAILABLE -> {
                    binding.testFlibustaIsUpProgress.visibility = View.INVISIBLE
                    binding.testFlibustaIsUpText.setTextColor(
                        ResourcesCompat.getColor(resources, R.color.white, theme)
                    )
                    binding.testFlibustaIsUpText.text =
                        GrammarHandler.getColoredString(
                            getString(R.string.flibusta_server_is_down),
                            Color.parseColor("#881515"),
                            this
                        )
                    showFlibustaIsDownDialog()
                }
            }
        }
        viewModel.flibustaCheckState.observe(this) {
            when (it) {
                StartViewModel.RESULT_SUCCESS -> {
                    // ready to go
                    readyToGo()
                }
                StartViewModel.RESULT_FAILED -> {
                    // show error
                    notifyConnectionError()
                }
            }
        }
    }

    private fun readyToGo() {
        binding.connectionTestText.setTextColor(
            ResourcesCompat.getColor(resources, R.color.white, theme)
        )
        binding.connectionTestText.text = GrammarHandler.getColoredString(
            getString(R.string.connected_message),
            Color.parseColor("#0c6126"),
            this
        )
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

    private fun notifyConnectionError() {
        if (!this::errorSnackbar.isInitialized) {
            errorSnackbar = Snackbar.make(
                binding.rootView,
                getString(R.string.connection_error_message),
                Snackbar.LENGTH_INDEFINITE
            )
            errorSnackbar.setAction(getString(R.string.retry_request_title)) {
                binding.connectionTestText.text = getString(R.string.client_progress)
                binding.connectionTestProgress.visibility = View.GONE
                binding.testFlibustaIsUpText.text = getString(R.string.test_flibusta_is_up)
                binding.testFlibustaIsUpProgress.visibility = View.GONE
                binding.clientRunningProgress.visibility = View.VISIBLE
                launchConnection()
            }
            if(PreferencesHandler.instance.isEInk){
                errorSnackbar.setBackgroundTint(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.always_white,
                        theme
                    )
                )
                errorSnackbar.setActionTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.black,
                        theme
                    )
                )
            }
            else{
                errorSnackbar.setActionTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.genre_text_color,
                        null
                    )
                )
            }
            errorSnackbar.show()
        }
    }

    private fun showFlibustaIsDownDialog() {
        val dialogBuilder = AlertDialog.Builder(this, R.style.dialogTheme)
        dialogBuilder.setTitle(getString(R.string.flibusta_server_is_down))
            .setMessage(getString(R.string.flibusta_down_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.skip_inspection_item)) { _, _ ->
                flibustaServerChecked()
            }
            .setNegativeButton(getString(R.string.try_again_message)) { _, _ ->
                binding.testFlibustaIsUpProgress.visibility = View.VISIBLE
                viewModel.checkServer()
            }
            .show()
    }

    private fun flibustaServerChecked() {
        binding.testFlibustaIsUpProgress.visibility = View.INVISIBLE
        binding.connectionTestText.visibility = View.VISIBLE
        binding.connectionTestProgress.visibility = View.VISIBLE
        viewModel.checkFlibustaAvailability()
    }

    private fun setupUi() {
        binding.isEbook.isChecked =
            PreferencesHandler.instance.isEInk
        binding.isEbook.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.instance.isEInk = state
            recreate()
        }
        binding.useHardwareAccelerationSwitcher.isChecked =
            PreferencesHandler.instance.hardwareAcceleration
        binding.useHardwareAccelerationSwitcher.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.instance.hardwareAcceleration = state
            if (state) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                )
            } else {
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                )
            }
        }

        binding.appVersion.text = PreferencesHandler.instance.appVersion
        if (PreferencesHandler.instance.hardwareAcceleration) {
            // проверю аппаратное ускорение
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
        }

        binding.showTorLogBtn?.setOnClickListener {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        // setup theme
        if (PreferencesHandler.instance.isEInk) {
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

            binding.isEbook.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.e_ink_text_color,
                    theme
                )
            )
            binding.isEbook.setShadowLayer(0F, 0F, 0F, R.color.transparent)

            binding.useHardwareAccelerationSwitcher.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.e_ink_text_color,
                    theme
                )
            )
            binding.useHardwareAccelerationSwitcher.setShadowLayer(0F, 0F, 0F, R.color.transparent)

            binding.startConnectionTestBtn.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.e_ink_text_color,
                    theme
                )
            )
            binding.startConnectionTestBtn.setShadowLayer(0F, 0F, 0F, R.color.transparent)

            binding.showTorLogBtn?.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.e_ink_text_color,
                    theme
                )
            )
            binding.showTorLogBtn?.setShadowLayer(0F, 0F, 0F, R.color.transparent)

            binding.clientProgressText.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.e_ink_text_color,
                    theme
                )
            )
            binding.clientProgressText.setShadowLayer(0F, 0F, 0F, R.color.transparent)

            binding.connectionTestText.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.e_ink_text_color,
                    theme
                )
            )
            binding.connectionTestText.setShadowLayer(0F, 0F, 0F, R.color.transparent)

            binding.testFlibustaIsUpText.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.e_ink_text_color,
                    theme
                )
            )
            binding.testFlibustaIsUpText.setShadowLayer(0F, 0F, 0F, R.color.transparent)

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
        binding.startConnectionTestBtn.setOnClickListener {
            val targetActivityIntent = Intent(this, ConnectivityGuideActivity::class.java)
            targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(targetActivityIntent)
            finish()
        }

        binding.testStartApp.setOnClickListener {
            readyToGo()
        }
    }

    override fun onResume() {
        super.onResume()
        if (PreferencesHandler.instance.firstUse) {
            val targetActivityIntent = Intent(this, FirstUseGuideActivity::class.java)
            targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(targetActivityIntent)
            finish()
        }
        checkConnectionOptions()
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
            Log.d("surprise", "MainActivity.kt 344: test connection")
            if (NetworkHandler().isVpnConnected()) {
                if (PreferencesHandler.instance.useTor) {
                    showDisableTorDialog()
                } else {
                    launchConnection()
                }
            } else if (!PreferencesHandler.instance.useTor) {
                showEnableTorDialog()
            } else {
                launchConnection()
            }
        } else {
            launchConnection()
        }
    }

    private fun launchConnection() {
        if (PreferencesHandler.instance.useTor) {
            launchTor()
            binding.showTorLogBtn?.visibility = View.VISIBLE
        } else {
            notifyVpnUse()
            binding.showTorLogBtn?.visibility = View.GONE
        }
    }

    private fun showDisableTorDialog() {
        AlertDialog.Builder(this, R.style.dialogTheme)
            .setMessage(getString(R.string.disable_tor_message))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                PreferencesHandler.instance.useTor = false
                launchConnection()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> launchConnection() }
            .setNeutralButton(getString(R.string.do_not_show_again_message)) { _, _ ->
                PreferencesHandler.instance.showConnectionOptions = false
                launchConnection()
            }
            .show()
    }

    private fun showEnableTorDialog() {
        AlertDialog.Builder(this, R.style.dialogTheme)
            .setMessage(getString(R.string.enable_tor_message))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                PreferencesHandler.instance.useTor = true
                launchConnection()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> launchConnection() }
            .setNeutralButton(getString(R.string.do_not_show_again_message)) { _, _ ->
                PreferencesHandler.instance.showConnectionOptions = false
                launchConnection()
            }
            .show()
    }

    private fun notifyVpnUse() {
        binding.clientProgressText.text =
            GrammarHandler.getColoredString(
                getString(R.string.vpn_use),
                Color.parseColor("#5403ad")
            , this)
        binding.clientProgressText.setTextColor(
            ResourcesCompat.getColor(resources, R.color.white, theme)
        )
        binding.clientRunningProgress.visibility = View.INVISIBLE
        binding.testFlibustaIsUpText.visibility = View.VISIBLE
        binding.testFlibustaIsUpProgress.visibility = View.VISIBLE
        viewModel.checkServer()
    }

    private fun launchTor() {
        viewModel.startTor(this)
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
}