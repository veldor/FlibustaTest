package net.veldor.flibusta_test.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.msopentech.thali.toronionproxy.OnionProxyManagerEventHandler
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivitySetTorBridgesBinding
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.view_model.BridgesViewModel
import java.util.*

class SetTorBridgesActivity : AppCompatActivity() {
    private var ipTemplate: Regex = Regex("\\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3}")
    private var bootstappedTemplate: Regex = Regex("Bootstrapped \\d+%")
    private lateinit var viewModel: BridgesViewModel
    private lateinit var binding: ActivitySetTorBridgesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        binding = ActivitySetTorBridgesBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this).get(BridgesViewModel::class.java)

        viewModel.liveCheckState.observe(this) {
            when (it) {
                BridgesViewModel.STATE_SUCCESS -> {
                    Toast.makeText(
                        this,
                        getString(R.string.bridges_saved_title),
                        Toast.LENGTH_SHORT
                    ).show()
                    this.setResult(Activity.RESULT_OK)
                    finish()
                }
                BridgesViewModel.STATE_FAILED -> {
                    Toast.makeText(this, "Test failed", Toast.LENGTH_SHORT).show()
                    testFinished()
                }
            }
        }

        setupUI()
        setContentView(binding.root)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            viewModel.cancelTorLaunch(this)
            this.setResult(Activity.RESULT_CANCELED)
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun setupUI() {
        binding.statusWrapper.setInAnimation(this, android.R.anim.slide_in_left)
        binding.statusWrapper.setOutAnimation(this, android.R.anim.slide_out_right)

        OnionProxyManagerEventHandler.liveLastLog.observe(this) {
            if (it.contains("unable to connect OR connection")) {
                Log.d("surprise", "setupUI: have ip connection error!")
                val match = ipTemplate.find(it)
                if (match != null) {
                    binding.currentState.text = String.format(
                        Locale.ENGLISH,
                        "Unable connect to bridge %s",
                        match.groupValues[0]
                    )
                }
            }
            if (bootstappedTemplate.find(it) != null) {
                Log.d("surprise", "setupUI: have bootstrapped template")
                binding.currentState.text = it
            }
            binding.statusWrapper.setText(it)
        }

        if (viewModel.currentBridgesCheckWork != null && !viewModel.currentBridgesCheckWork?.isCompleted!!) {
            testBegin()

        }

        binding.cancelCheckBtn.setOnClickListener {
            testFinished()
            viewModel.cancelTorLaunch(this)
        }

        binding.getBridgesLinkBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://www.google.com/search?q=получить+мосты+tor&ie=UTF-8")
            startActivity(intent)
        }

        binding.checkBridgesBtn.setOnClickListener {
            viewModel.checkBridges(binding.bridgesInput.text, this)
            testBegin()
        }

        binding.switchToVpnBtn.setOnClickListener {
            PreferencesHandler.instance.useTor = false
            this.setResult(Activity.RESULT_OK)
            this.finish()
        }

        binding.bridgesInput.addTextChangedListener { value ->
            // проверю введённый текст на соответствие шаблону
            val template = Regex(
                "obfs4 (\\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3}):\\d+.*cert=.*iat-mode=\\d+",
                setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
            )
            if (!value.isNullOrEmpty()) {
                val ips = arrayListOf<String>()
                val matches = template.findAll(value).toList()
                if (matches.isNotEmpty()) {
                    matches.forEach {
                        ips.add(it.groupValues[1])
                    }
                }
                if (ips.isNotEmpty()) {
                    binding.bridgesStateView.text = String.format(
                        Locale.ENGLISH,
                        "Found bridges IP:\n%s",
                        ips.joinToString("\n")
                    )
                    binding.checkBridgesBtn.isEnabled = true
                } else {
                    binding.bridgesStateView.text = getString(R.string.no_bridge_ip_ttile)
                    binding.checkBridgesBtn.isEnabled = false
                }
            }
        }
    }

    private fun testBegin() {
        binding.currentState.visibility = View.VISIBLE
        binding.currentState.setText(getString(R.string.try_bridges_title))
        binding.statusWrapper.visibility = View.VISIBLE
        binding.cancelCheckBtn.visibility = View.VISIBLE
        binding.textInputLayout.visibility = View.GONE
        binding.checkBridgesBtn.visibility = View.GONE
    }

    private fun testFinished() {
        binding.currentState.visibility = View.GONE
        binding.statusWrapper.visibility = View.GONE
        binding.cancelCheckBtn.visibility = View.GONE
        binding.textInputLayout.visibility = View.VISIBLE
        binding.checkBridgesBtn.visibility = View.VISIBLE
    }
}