package net.veldor.flibusta_test.ui.browser_fragments

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentWebViewBinding
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.view_model.WebViewViewModel

class WebViewFragment : Fragment() {
    lateinit var binding: FragmentWebViewBinding
    private lateinit var viewModel: WebViewViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity?.invalidateOptionsMenu()
        setHasOptionsMenu(true)
        viewModel = ViewModelProvider(this).get(WebViewViewModel::class.java)
        binding = FragmentWebViewBinding.inflate(inflater, container, false)
        handleLoading()
        setupUI()
        return binding.root
    }

    private fun setupUI() {
        binding.viewOkBtn.setOnClickListener {
            binding.viewSwitcherContainer.visibility = View.GONE
        }
        binding.currentViewName.text = viewModes[PreferencesHandler.instance.browserViewMode]
        binding.switchViewLeftBtn.setOnClickListener {
            var currentMode = PreferencesHandler.instance.browserViewMode
            if (currentMode == 0) {
                currentMode = viewModes.size
            } else {
                currentMode -= 1
            }
            PreferencesHandler.instance.browserViewMode = currentMode
            binding.currentViewName.text = viewModes[PreferencesHandler.instance.browserViewMode]
            binding.myWebView.loadUrl(PreferencesHandler.instance.lastWebViewLink)
        }
        binding.switchViewLeftBtn.setOnClickListener {
            var currentMode = PreferencesHandler.instance.browserViewMode
            if (currentMode == viewModes.size) {
                currentMode = 0
            } else {
                currentMode += 1
            }
            PreferencesHandler.instance.browserViewMode = currentMode
            binding.currentViewName.text = viewModes[PreferencesHandler.instance.browserViewMode]
            binding.myWebView.loadUrl(PreferencesHandler.instance.lastWebViewLink)
        }
    }

    private fun handleLoading() {
        binding.myWebView.setup()
        Log.d(
            "surprise",
            "startBrowsing: last loaded is ${PreferencesHandler.instance.lastWebViewLink}"
        )
        binding.myWebView.loadUrl(PreferencesHandler.instance.lastWebViewLink)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.browser_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_view -> {
                showViewSwitcher()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showViewSwitcher() {
        binding.viewSwitcherContainer.visibility = View.VISIBLE
    }

    companion object {
        const val VIEW_MODE_NORMAL = 0
        const val VIEW_MODE_LIGHT = 1
        const val VIEW_MODE_FAT = 2
        const val VIEW_MODE_FAST = 3
        const val VIEW_MODE_FAST_FAT = 4
        val viewModes = arrayListOf(
            "Normal mode",
            "Light mode",
            "Fat mode",
            "Fast mode",
            "Fast fat mode"
        )
        const val NEW_BOOKS = "/new"
        const val SEARCH_URL = "/booksearch?ask="


    }
}