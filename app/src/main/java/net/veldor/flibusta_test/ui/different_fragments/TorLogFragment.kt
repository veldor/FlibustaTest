package net.veldor.flibusta_test.ui.different_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.msopentech.thali.toronionproxy.OnionProxyManagerEventHandler
import net.veldor.flibusta_test.databinding.FragmentTorLogBackdropBinding

class TorLogFragment : Fragment(){
    private lateinit var binding: FragmentTorLogBackdropBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTorLogBackdropBinding.inflate(inflater, container, false)
        setupUI()
        return binding.root
    }

    private fun setupUI() {
        OnionProxyManagerEventHandler.liveLastLog.observe(viewLifecycleOwner){
            binding.currentLogMessage.text = it
            if(it.contains("%")){
                binding.currentProgressMessage.text = it
            }
            binding.fullTextLog.append("$it\n")
        }
    }
}