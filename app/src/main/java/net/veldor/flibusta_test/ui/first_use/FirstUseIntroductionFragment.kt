package net.veldor.flibusta_test.ui.first_use

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentFirstUseIntroductionBinding
import net.veldor.flibusta_test.ui.FirstUseGuideActivity

class FirstUseIntroductionFragment : Fragment() {


    private lateinit var binding: FragmentFirstUseIntroductionBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFirstUseIntroductionBinding.inflate(inflater, container, false)
        binding.nextTestBtn.setOnClickListener {
            goToNext()
        }
        return binding.root
    }

    private fun goToNext() {
        val navController =
            Navigation.findNavController(
                requireActivity() as FirstUseGuideActivity,
                R.id.nav_host_fragment
            )
        navController.navigate(R.id.go_to_permissions_action)
    }

}