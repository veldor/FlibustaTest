package net.veldor.flibusta_test.ui.first_use

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentFirstUsePermissionsBinding
import net.veldor.flibusta_test.model.view_model.FirstUseViewModel
import net.veldor.flibusta_test.ui.FirstUseGuideActivity


class FirstUseGrantPermissionsFragment : Fragment() {


    private lateinit var viewModel: FirstUseViewModel
    private lateinit var binding: FragmentFirstUsePermissionsBinding

    private val mPermissionResult = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (!viewModel.permissionsGranted()) {
            Toast.makeText(
                requireContext(),
                "Для продолжения работы необходимо предоставить все разрешения. Нажмите кнопку ниже.",
                Toast.LENGTH_SHORT
            ).show()
            goToNext()
        } else {
            permissionsGranted()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFirstUsePermissionsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(FirstUseViewModel::class.java)
        // if permissions granted yet- automatically go to next action
        if (viewModel.permissionsGranted()) {
            permissionsGranted()
        }

        binding.grantPermissionsBtn.setOnClickListener {
            mPermissionResult.launch(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        }
        return binding.root
    }

    private fun goToNext() {
        val navController =
            Navigation.findNavController(
                requireActivity() as FirstUseGuideActivity,
                R.id.nav_host_fragment
            )
        navController.navigate(R.id.go_to_select_dir_action)
    }

    private fun permissionsGranted() {
        Toast.makeText(
            requireContext(),
            "Разрешения на доступ к памяти выданы, переходим к следующему шагу",
            Toast.LENGTH_SHORT
        ).show()
        goToNext()
    }
}