package net.veldor.flibusta_test.ui.first_use

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.veldor.flibusta_test.databinding.FragmentFirstUseFinishGuideBinding
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.ui.ConnectivityGuideActivity

class FirstUseFinishGuideFragment : Fragment() {


    private lateinit var binding: FragmentFirstUseFinishGuideBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFirstUseFinishGuideBinding.inflate(inflater, container, false)
        binding.toConnectionSettingsBtn.setOnClickListener {
            PreferencesHandler.instance.firstUse = false
            goToNext()
        }
        return binding.root
    }

    private fun goToNext() {
        val targetActivityIntent = Intent(requireContext(), ConnectivityGuideActivity::class.java)
        targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(targetActivityIntent)
        requireActivity().finish()
    }

}