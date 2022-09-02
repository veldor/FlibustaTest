package net.veldor.flibusta_test.ui.connection_guide

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentConnectFlibustaViaTorBinding
import net.veldor.flibusta_test.model.handler.GrammarHandler
import net.veldor.flibusta_test.model.helper.UrlHelper
import net.veldor.flibusta_test.model.view_model.ConnectivityGuideViewModel
import net.veldor.flibusta_test.model.web.UniversalWebClient
import net.veldor.flibusta_test.ui.ConnectivityGuideActivity
import java.util.*

@Suppress("DEPRECATION")
class TestFlibustaViaTorFragment : Fragment() {

    private lateinit var binding: FragmentConnectFlibustaViaTorBinding
    private lateinit var viewModel: ConnectivityGuideViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(ConnectivityGuideViewModel::class.java)
        binding = FragmentConnectFlibustaViaTorBinding.inflate(inflater, container, false)
        binding.launchTorBtn.setOnClickListener {
            checkMirrorInput()
            viewModel.finallyTestConnection(binding.customMirrorInput.text)

        }
        binding.customMirrorInput.setText(UrlHelper.getBaseUrl())
        binding.backBtn.setOnClickListener {
            val navController =
                Navigation.findNavController(
                    requireActivity() as ConnectivityGuideActivity,
                    R.id.nav_host_fragment
                )
            navController.navigateUp()
        }

        binding.nextTestBtn.setOnClickListener {
            val navController =
                Navigation.findNavController(
                    requireActivity() as ConnectivityGuideActivity,
                    R.id.nav_host_fragment
                )
            navController.navigate(R.id.action_test_flibusta_download_via_tor)
        }
        setupObservers()
        return binding.root
    }

    private fun checkMirrorInput() {
        val value = binding.customMirrorInput.text
        if (value != null) {
            if (value.toString().isNotEmpty()) {
                if (!GrammarHandler.isValidUrl(value.toString())) {
                    binding.customMirrorInput.error = getString(R.string.wrong_url_message)
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.libraryConnectionState.observe(viewLifecycleOwner) {
            if (it != null) {
                when (it) {
                    ConnectivityGuideViewModel.STATE_PASSED -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            binding.testStatusText.setTextColor(
                                resources.getColor(
                                    R.color.genre_text_color,
                                    null
                                )
                            )
                        } else {
                            binding.testStatusText.setTextColor(resources.getColor(R.color.genre_text_color))
                        }
                        binding.checkProgress.visibility = View.INVISIBLE
                        binding.launchTorBtn.isEnabled = true
                        binding.nextTestBtn.isEnabled = true
                        binding.testStatusText.text = getString(R.string.check_passed)
                        binding.nextTestBtn.isEnabled = true
                    }
                    ConnectivityGuideViewModel.STATE_FAILED -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            binding.testStatusText.setTextColor(
                                resources.getColor(
                                    R.color.book_name_color,
                                    null
                                )
                            )
                        } else {
                            binding.testStatusText.setTextColor(resources.getColor(R.color.book_name_color))
                        }
                        binding.launchTorBtn.text = getString(R.string.try_again_message)
                        binding.launchTorBtn.isEnabled = true
                        binding.checkProgress.visibility = View.INVISIBLE
                        binding.testStatusText.text = String.format(
                            Locale.ENGLISH,
                            getString(R.string.fail_tor_message),
                            UniversalWebClient.connectionError.value?.message
                        )
                        binding.errorDescriptionBtn.visibility = View.VISIBLE
                        binding.errorDescriptionBtn.setOnClickListener {
                            Toast.makeText(
                                requireContext(),
                                "Не удалось соединиться с библиотекой\nВозможно, она не в сети, или неправильно работает встроенный клиент.\nВы можете вернуться назад, попробовать подключиться к TOR ещё раз или выбрать VPN соединение.",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    }
                    ConnectivityGuideViewModel.STATE_CHECK_ERROR -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            binding.testStatusText.setTextColor(
                                resources.getColor(
                                    R.color.book_name_color,
                                    null
                                )
                            )
                        } else {
                            binding.testStatusText.setTextColor(resources.getColor(R.color.book_name_color))
                        }
                        binding.launchTorBtn.text = getString(R.string.try_again_message)
                        binding.launchTorBtn.isEnabled = true
                        binding.checkProgress.visibility = View.INVISIBLE
                        binding.testStatusText.text = getString(R.string.check_failed)
                        binding.errorDescriptionBtn.visibility = View.VISIBLE
                        binding.errorDescriptionBtn.setOnClickListener {
                            Toast.makeText(
                                requireContext(),
                                "Не удалось соединиться с библиотекой\nВозможно, она не в сети, или неправильно работает встроенный клиент.\nВы можете вернуться назад, попробовать подключиться к TOR ещё раз или выбрать VPN соединение.",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    }
                    ConnectivityGuideViewModel.STATE_WAIT -> {
                        binding.launchTorBtn.isEnabled = false
                        binding.testStatusText.text = "Инициализация"
                        binding.checkProgress.visibility = View.VISIBLE
                        binding.launchTorBtn.text = getString(R.string.in_progress_message)
                    }
                    else -> {
                        binding.testStatusText.text = it
                    }
                }
            }
        }
    }
}