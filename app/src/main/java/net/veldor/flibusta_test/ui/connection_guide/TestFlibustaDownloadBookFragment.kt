package net.veldor.flibusta_test.ui.connection_guide

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentTestFlibustaDownloadBookBinding
import net.veldor.flibusta_test.model.handler.DownloadHandler
import net.veldor.flibusta_test.model.view_model.ConnectivityGuideViewModel
import net.veldor.flibusta_test.ui.ConnectivityGuideActivity

class TestFlibustaDownloadBookFragment : Fragment() {

    private lateinit var binding: FragmentTestFlibustaDownloadBookBinding
    private lateinit var viewModel: ConnectivityGuideViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(ConnectivityGuideViewModel::class.java)
        binding = FragmentTestFlibustaDownloadBookBinding.inflate(inflater, container, false)

        binding.startTestBtn.setOnClickListener { view ->
            view.isEnabled = false
            viewModel.downloadTestBook()
            binding.checkProgress.visibility = View.VISIBLE

            DownloadHandler.instance.liveBookDownloadProgress.observe(viewLifecycleOwner) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    binding.checkProgress.max = (it.bookFullSize / 1000).toInt()
                    binding.checkProgress.isIndeterminate = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        binding.checkProgress.setProgress(
                            (it.bookLoadedSize / 1000).toInt(),
                            true
                        )
                    } else {
                        binding.checkProgress.progress = (it.bookLoadedSize / 1000).toInt()
                    }
                }
                if (it.successLoads > 0) {
                    binding.checkProgress.visibility = View.GONE
                    binding.nextTestBtn.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.success_book_load_title),
                        Toast.LENGTH_LONG
                    ).show()
                }
                else if(it.loadErrors > 0){
                    binding.checkProgress.visibility = View.GONE
                    binding.startTestBtn.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.failed_book_load_title),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        binding.skipTestBtn.setOnClickListener {
            goToNext()
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
        navController.navigate(R.id.finish_test_action)
    }
}