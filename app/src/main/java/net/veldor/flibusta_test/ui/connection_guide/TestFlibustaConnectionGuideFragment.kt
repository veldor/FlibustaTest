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
import net.veldor.flibusta_test.databinding.FragmentTestFlibustaConnectionBinding
import net.veldor.flibusta_test.model.handler.GrammarHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.view_model.ConnectivityGuideViewModel
import net.veldor.flibusta_test.ui.ConnectivityGuideActivity

@Suppress("DEPRECATION")
class TestFlibustaConnectionGuideFragment : Fragment() {

    private lateinit var binding: FragmentTestFlibustaConnectionBinding
    private lateinit var viewModel: ConnectivityGuideViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(ConnectivityGuideViewModel::class.java)
        binding = FragmentTestFlibustaConnectionBinding.inflate(inflater, container, false)
        binding.mirrorUrlInput.setText(PreferencesHandler.BASE_URL)
        binding.startTestBtn.setOnClickListener {
            val url = binding.mirrorUrlInput.text.toString()
            if (GrammarHandler.isValidUrl(url)) {
                binding.startTestBtn.isEnabled = false
                binding.checkProgress.visibility = View.VISIBLE
                viewModel.testLibraryConnection(url)
            } else {
                Toast.makeText(
                    requireContext(),
                    "URL must be like http(s)://address",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.skipTestBtn.setOnClickListener {
            goToNext()
        }
        binding.nextTestBtn.setOnClickListener {
            goToNext()
        }
        setupObservers()
        return binding.root
    }

    private fun goToNext() {
        val navController =
            Navigation.findNavController(
                requireActivity() as ConnectivityGuideActivity,
                R.id.nav_host_fragment
            )
        navController.navigate(R.id.from_2_to_3_test_action)
    }

    private fun setupObservers() {
        viewModel.libraryConnectionState.observe(viewLifecycleOwner) {
            if (it.equals(ConnectivityGuideViewModel.STATE_PASSED)) {
                binding.checkProgress.visibility = View.INVISIBLE
                binding.startTestBtn.isEnabled = true
                binding.nextTestBtn.isEnabled = true
                binding.testStatusText.text = getString(R.string.check_passed)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    binding.testStatusText.setTextColor(resources.getColor(R.color.genre_text_color, null))
                }
                else{
                    binding.testStatusText.setTextColor(resources.getColor(R.color.genre_text_color))
                }
                binding.errorDescriptionBtn.visibility = View.GONE
            } else if (it.equals(ConnectivityGuideViewModel.STATE_FAILED)) {
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
                binding.checkProgress.visibility = View.INVISIBLE
                binding.startTestBtn.isEnabled = true
                binding.testStatusText.text = getString(R.string.check_failed)
                binding.errorDescriptionBtn.visibility = View.VISIBLE
                binding.errorDescriptionBtn.setOnClickListener {
                    Toast.makeText(
                        requireContext(),
                        "Похоже, что сервер библиотеки не в сети.\nПроверьте правильность введения адреса\nПопробуйте проверить доступность позднее\nВы можете пропустить проверку и попытаться подключиться в любом случае",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            } else if (it.equals(ConnectivityGuideViewModel.STATE_CHECK_ERROR)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    binding.testStatusText.setTextColor(resources.getColor(R.color.book_name_color, null))
                }
                else{
                    binding.testStatusText.setTextColor(resources.getColor(R.color.book_name_color))
                }
                binding.checkProgress.visibility = View.INVISIBLE
                binding.startTestBtn.isEnabled = true
                binding.testStatusText.text = getString(R.string.check_error)
                binding.errorDescriptionBtn.visibility = View.VISIBLE
                binding.errorDescriptionBtn.setOnClickListener {
                    Toast.makeText(
                        requireContext(),
                        "Не удалось проверить подключение к библиотеке\nВозможно, проблемы с самой проверкой\nВы можете пропустить проверку и попробовать подключиться в любом случае.",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            } else if (it.equals(ConnectivityGuideViewModel.STATE_WAIT)) {
                binding.startTestBtn.isEnabled = false
                binding.testStatusText.text = ""
            }
        }
    }
}