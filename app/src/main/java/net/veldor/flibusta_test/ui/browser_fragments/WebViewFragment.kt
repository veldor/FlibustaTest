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

class WebViewFragment : Fragment(), DownloadLinksDelegate, DownloadTaskAppendedDelegate {
    private var mBookCheckSnackbar: Snackbar? = null
    private var mConfirmExit: Long = 0L
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var backdropFragment: OpdsDownloadBackdropFragment? = null
    private lateinit var mBooks: ArrayList<FoundEntity>
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

    private fun showInputMethod(v: View?) {
        val imm = App.instance.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.showSoftInput(v, 0)
    }

    private fun setupUI() {

        binding.bookSearchView.setOnQueryTextFocusChangeListener { view, b ->
            if (b) {
                (activity as BrowserActivity).binding.appBarLayout.visibility = View.GONE
                (activity as BrowserActivity).binding.bottomNavView.visibility = View.GONE
                showInputMethod(view.findFocus())
            } else {
                (activity as BrowserActivity).binding.appBarLayout.visibility = View.VISIBLE
                (activity as BrowserActivity).binding.bottomNavView.visibility = View.VISIBLE
                binding.bookSearchView.visibility = View.GONE
            }
        }

        binding.bookSearchView.setOnQueryTextListener(object :
            android.widget.SearchView.OnQueryTextListener,
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(request: String?): Boolean {
                if (request == null || request.trim().isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.request_must_be_filled_message),
                        Toast.LENGTH_SHORT
                    ).show()
                    return true
                }
                binding.myWebView.loadUrl(
                    SEARCH_URL + URLEncoder.encode(request, "utf-8").replace("+", "%20")
                )
                binding.bookSearchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(request: String?): Boolean {
                return true
            }
        })

        backdropFragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.opdsBackdropFragment) as OpdsDownloadBackdropFragment?
        backdropFragment?.let {
            // Get the BottomSheetBehavior from the fragment view
            BottomSheetBehavior.from(it.requireView()).let { bsb ->
                // Set the initial state of the BottomSheetBehavior to HIDDEN
                bsb.state = BottomSheetBehavior.STATE_HIDDEN
                // Set the reference into class attribute (will be used latter)
                bottomSheetBehavior = bsb
            }
        }

        binding.viewOkBtn.setOnClickListener {
            binding.viewSwitcherContainer.visibility = View.GONE
        }
        binding.currentViewName.text = viewModes[PreferencesHandler.instance.browserViewMode]
        binding.switchViewLeftBtn.setOnClickListener {
            var currentMode = PreferencesHandler.instance.browserViewMode
            if (currentMode == 0) {
                currentMode = viewModes.size - 1
            } else {
                currentMode -= 1
            }
            PreferencesHandler.instance.browserViewMode = currentMode
            binding.currentViewName.text = viewModes[PreferencesHandler.instance.browserViewMode]
            binding.myWebView.loadUrl(PreferencesHandler.instance.lastWebViewLink)
        }
        binding.switchViewRightBtn.setOnClickListener {
            var currentMode = PreferencesHandler.instance.browserViewMode
            if (currentMode == viewModes.size - 1) {
                currentMode = 0
            } else {
                currentMode += 1
            }
            PreferencesHandler.instance.browserViewMode = currentMode
            binding.currentViewName.text = viewModes[PreferencesHandler.instance.browserViewMode]
            binding.myWebView.loadUrl(PreferencesHandler.instance.lastWebViewLink)
        }

        binding.massLoadFab.setOnClickListener {
            if (this::mBooks.isInitialized) {
                backdropFragment?.loadBooksList(mBooks)
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    private fun handleLoading() {
        binding.myWebView.setup(delegate = this)
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
            R.id.action_search -> {
                binding.bookSearchView.visibility = View.VISIBLE
                binding.bookSearchView.isIconified = false
                binding.bookSearchView.requestFocus()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        viewModel.appendDownloadAppendedDelegate(this)
    }

    override fun onPause() {
        super.onPause()
        viewModel.removeDownloadAppendedDelegate()
    }

    private fun showViewSwitcher() {
        binding.viewSwitcherContainer.visibility = View.VISIBLE
    }

    companion object {
        val viewModes = arrayListOf(
            "Normal mode",
            "Light mode",
            "Fat mode",
            "Fast mode",
            "Fast fat mode"
        )
        const val VIEW_MODE_NORMAL = 0
        const val VIEW_MODE_LIGHT = 1
        const val VIEW_MODE_FAT = 2
        const val VIEW_MODE_FAST = 3
        const val VIEW_MODE_FAST_FAT = 4
        const val NEW_BOOKS = "/new"
        const val SEARCH_URL = "/booksearch?ask="
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

    fun keyPressed(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (bottomSheetBehavior != null && bottomSheetBehavior?.state != BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                backdropFragment?.cancelBookInfoLoad()
                return true
            }
            // возвращаюсь на страницу назад в браузере
            if (binding.myWebView.canGoBack()) {
                binding.myWebView.goBack()
                return true
            }
            if (mConfirmExit > 0) {
                if (mConfirmExit > System.currentTimeMillis() - 3000) {
                    // выйду из приложения
                    Log.d("surprise", "OPDSActivity onKeyDown exit")
                    requireActivity().finish()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Нечего загружать. Нажмите ещё раз для выхода",
                        Toast.LENGTH_SHORT
                    ).show()
                    mConfirmExit = System.currentTimeMillis()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Нечего загружать. Нажмите ещё раз для выхода",
                    Toast.LENGTH_SHORT
                ).show()
                mConfirmExit = System.currentTimeMillis()
            }
            return true
        }
        return true
    }
}