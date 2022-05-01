package net.veldor.flibusta_test.ui.first_use

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentFirstUseViewGuideBinding
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.ui.FirstUseGuideActivity

class FirstUseViewModificationsGuideFragment : Fragment() {
    private lateinit var binding: FragmentFirstUseViewGuideBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFirstUseViewGuideBinding.inflate(inflater, container, false)
        binding.nextTestBtn.setOnClickListener {
            goToNext()
        }

        binding.useHardwareAccelerationSwitcher.isChecked =
            PreferencesHandler.instance.hardwareAcceleration
        binding.isEbook.isChecked = PreferencesHandler.instance.isEInk
        binding.useHardwareAccelerationSwitcher.setOnCheckedChangeListener { _, isChecked ->
            PreferencesHandler.instance.hardwareAcceleration = isChecked
        }
        binding.isEbook.setOnCheckedChangeListener { _, isChecked ->
            PreferencesHandler.instance.isEInk = isChecked
        }
        return binding.root
    }

    private fun goToNext() {
        val navController =
            Navigation.findNavController(
                requireActivity() as FirstUseGuideActivity,
                R.id.nav_host_fragment
            )
        navController.navigate(R.id.go_to_finish_first_setup_action)
    }
}