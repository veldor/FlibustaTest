package net.veldor.flibusta_test.ui.browser_fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentWebViewBinding
import net.veldor.flibusta_test.model.delegate.DownloadLinksDelegate
import net.veldor.flibusta_test.model.delegate.DownloadTaskAppendedDelegate
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.selections.DownloadLink
import net.veldor.flibusta_test.model.selections.opds.FoundEntity
import net.veldor.flibusta_test.model.view_model.WebViewViewModel
import net.veldor.flibusta_test.ui.BrowserActivity
import java.io.InputStream
import java.net.URLEncoder

class WebViewBackdropFragment : WebViewFragment() {
    private var mBookCheckSnackbar: Snackbar? = null
    private lateinit var mBooks: ArrayList<FoundEntity>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(WebViewViewModel::class.java)
        binding = FragmentWebViewBinding.inflate(inflater, container, false)
        setupUI()
        return binding.root
    }

    private fun setupUI() {
        binding.massLoadFab.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        viewModel.appendDownloadAppendedDelegate(this)
    }

    override fun onPause() {
        super.onPause()
        viewModel.removeDownloadAppendedDelegate()
    }

    override fun linkClicked(link: String) {
        // download book by link
        showCheckBookSnackbar()
        viewModel.addDownload(link)
    }

    private fun showCheckBookSnackbar() {
        if (mBookCheckSnackbar == null) {
            mBookCheckSnackbar =
                Snackbar.make(binding.root, "Check book link", Snackbar.LENGTH_INDEFINITE)
        }
        mBookCheckSnackbar?.show()
    }

    override fun textReceived(textStream: InputStream) {
        viewModel.searchLinksInText(textStream)
    }

    override fun taskAppended(link: DownloadLink) {
        activity?.runOnUiThread {
            Toast.makeText(
                requireContext(),
                String.format(getString(R.string.book_added_to_queue_pattern), link.name),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun booksParsed(result: ArrayList<FoundEntity>) {
        mBooks = result
        if (result.isNotEmpty()) {
            binding.massLoadFab.show()
        } else {
            binding.massLoadFab.hide()
        }
    }

    override fun taskAppendFailed() {
        mBookCheckSnackbar?.dismiss()
        Toast.makeText(
            App.instance,
            App.instance.getString(R.string.no_download_links_title),
            Toast.LENGTH_SHORT
        ).show()
    }
}