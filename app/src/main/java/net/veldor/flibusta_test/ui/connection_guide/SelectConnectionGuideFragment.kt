package net.veldor.flibusta_test.ui.connection_guide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentSelectConnectionTypeBinding
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.view_model.ConnectivityGuideViewModel
import net.veldor.flibusta_test.ui.ConnectivityGuideActivity

class SelectConnectionGuideFragment : Fragment() {

    private lateinit var binding: FragmentSelectConnectionTypeBinding
    private lateinit var viewModel: ConnectivityGuideViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(ConnectivityGuideViewModel::class.java)
        binding = FragmentSelectConnectionTypeBinding.inflate(inflater, container, false)

        if (!PreferencesHandler.instance.useTor) {
            binding.selectGroup.check(R.id.use_vpn_button)
            binding.testStatusText.text = getString(R.string.use_vpn_info)
        } else {
            binding.selectGroup.check(R.id.use_tor_button)
            binding.testStatusText.text = getString(R.string.use_tor_info)
        }

        binding.selectGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.use_vpn_button) {
                PreferencesHandler.instance.useTor = false
                binding.testStatusText.text = getString(R.string.use_vpn_info)
            } else {
                PreferencesHandler.instance.useTor = true
                binding.testStatusText.text = getString(R.string.use_tor_info)
            }
        }

        binding.nextTestBtn.setOnClickListener {
            goToNext()
        }
        return binding.root
    }

    private fun goToNext() {
        val navController =
            Navigation.findNavController(
                requireActivity() as ConnectivityGuideActivity,
                R.id.nav_host_fragment
            )
        if(!PreferencesHandler.instance.useTor){
            navController.navigate(R.id.test_vpn_connection_action)
        }
        else{
            navController.navigate(R.id.test_tor_connection_action)
        }
    }

}