package net.veldor.flibusta_test.view.components

import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.DialogSetupTorBridgesBinding
import net.veldor.flibusta_test.databinding.InputFieldBinding
import net.veldor.flibusta_test.model.listener.ActionListener
import net.veldor.flibusta_test.model.view_model.TorBridgesViewModel

class TorBridgesSetupDialog : DialogFragment(), ActionListener {


    private lateinit var viewModel: TorBridgesViewModel
    private lateinit var binding: DialogSetupTorBridgesBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            binding = DialogSetupTorBridgesBinding.inflate(layoutInflater)
            viewModel = ViewModelProvider(this)[TorBridgesViewModel::class.java]
            viewModel.listener = this
            binding.customBridgesBtn.setOnClickListener {
                viewModel.loadCustomBridges(requireContext())
            }
            binding.officialBridgesBtn.setOnClickListener {
                // load captcha
                viewModel.loadOfficialBridgesCaptcha(requireContext()) { captcha: Pair<Bitmap?, String?>? ->
                    activity?.runOnUiThread {
                        // show captcha dialog
                        showCaptchaDialog(captcha?.first) { code: String ->
                            viewModel.sendCaptchaAnswer(code, captcha!!.second!!, requireContext())
                        }
                    }
                }
            }

            binding.ownBridgesBtn.setOnClickListener {
                val areaBinding = InputFieldBinding.inflate(layoutInflater)
                val builder = AlertDialog.Builder(requireContext())
                    .setTitle(R.string.enter_own_bridges_here_title)
                    .setView(areaBinding.root)
                    .setPositiveButton(R.string.save_title) { _, _ ->
                        val textValue = areaBinding.input.text.toString()
                        if (textValue.isNotEmpty()) {
                            viewModel.saveOwnBridges(textValue, requireContext())
                        }
                    }
                builder.show()
            }

            binding.clearBridgesBtn.setOnClickListener {
                viewModel.clearBridges(requireContext()) {
                    activity?.runOnUiThread {
                        viewModel.clearBridges(requireContext()) {
                            activity?.runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    R.string.tor_bridges_clean_message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }

            binding.showCurrentBridgesBtn.setOnClickListener {
                viewModel.requestCurrentBridges(requireContext()) { bridges: String ->
                    activity?.runOnUiThread {
                        val builder = AlertDialog.Builder(requireContext())
                            .setTitle(R.string.enter_own_bridges_here_title)
                            .setTitle(R.string.briges_here_title)
                            .setMessage(bridges)
                            .setPositiveButton(R.string.close_message, null)
                        builder.show()
                    }
                }
            }

            binding.testConnectionBtn.setOnClickListener {
                viewModel.launchTextConnection(requireContext())
            }

            AlertDialog.Builder(requireActivity(), R.style.dialogTheme)
                .setTitle(getString(R.string.setup_bridges_title))
                .setView(binding.root)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (callback != null) {
                        callback?.let { it() }
                    }
                }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }


    companion object {
        const val TAG = "tor bridges setup dialog"
        var callback: (() -> Unit)? = null
    }

    override fun actionStateUpdated(message: String) {
        activity?.runOnUiThread {
            binding.statusView.text = message
        }
    }

    override fun actionFinished(isSuccess: Boolean, message: String) {
        activity?.runOnUiThread {
            binding.progressView.visibility = View.INVISIBLE
            binding.statusView.text = message
        }
    }

    override fun actionLaunched() {
        activity?.runOnUiThread {
            binding.progressView.visibility = View.VISIBLE
        }
    }


    private fun showCaptchaDialog(first: Bitmap?, callback: (String) -> Unit) {
        val view = layoutInflater.inflate(R.layout.captcha_dialog, null)
        (view.findViewById<ImageView>(R.id.captchaView)).setImageBitmap(first)
        val builder = AlertDialog.Builder(requireContext())
            .setTitle(R.string.enter_text_from_captcha_here_message)
            .setView(view)
            .setPositiveButton(R.string.send_title) { _, _ ->
                val textView = view.findViewById<EditText>(R.id.captchaText)
                callback(textView.text.toString())
            }
        builder.show()

    }
}