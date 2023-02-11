package net.veldor.flibusta_test.view.search_fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MenuProvider
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentWebViewBinding
import net.veldor.flibusta_test.model.delegate.DownloadLinksDelegate
import net.veldor.flibusta_test.model.delegate.DownloadTaskAppendedDelegate
import net.veldor.flibusta_test.model.handler.BookmarkHandler
import net.veldor.flibusta_test.model.handler.GrammarHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.interfaces.BookCheckInterface
import net.veldor.flibusta_test.model.selection.DownloadLink
import net.veldor.flibusta_test.model.selection.FoundEntity
import net.veldor.flibusta_test.model.view_model.WebViewViewModel
import net.veldor.flibusta_test.view.SearchActivity
import net.veldor.flibusta_test.view.components.AddBookmarkDialog
import net.veldor.flibusta_test.view.components.BookDownloadSetupDialog
import net.veldor.flibusta_test.view.components.LoginDialog
import java.io.InputStream
import java.net.URLEncoder


open class WebViewFragment : DirectorySelectFragment(), DownloadLinksDelegate, DownloadTaskAppendedDelegate,
    BookCheckInterface {
    private lateinit var menuProvider: MenuProvider
    private lateinit var errorSnackbar: Snackbar
    private var isViewSetupOpened: Boolean = false
    private var isFullscreen: Boolean = false
    private var mBookCheckSnackbar: Snackbar? = null
    private var mConfirmExit: Long = 0L
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var backdropFragment: OpdsDownloadBackdropFragment? = null
    private lateinit var mBooks: HashMap<String, FoundEntity>
    lateinit var binding: FragmentWebViewBinding
    internal lateinit var viewModel: WebViewViewModel

    private var shortAnimationDuration: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Retrieve and cache the system's default "short" animation time.
        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)

        viewModel = ViewModelProvider(this)[WebViewViewModel::class.java]
        binding = FragmentWebViewBinding.inflate(inflater, container, false)
        handleLoading()
        val isFullscreen = savedInstanceState?.getBoolean("Fullscreen")
        if (isFullscreen != null && isFullscreen) {
            enableFullscreen()
        }
        val isViewSetup = savedInstanceState?.getBoolean("ViewSetup")
        if (isViewSetup != null && isViewSetup) {
            isViewSetupOpened = true

            binding.viewSwitcherContainer.visibility = View.VISIBLE
        }
        setupUI()
        return binding.root
    }


    private fun setMenu() {
        requireActivity().invalidateOptionsMenu()
        menuProvider = object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.browser_menu, menu)
                val loginCookie = menu.findItem(R.id.action_login)
                if (PreferencesHandler.authCookie != null) {
                    loginCookie.isVisible = false
                }
                if (binding.myWebView.url != null) {
                    // check when request link in bookmarks list
                    if (BookmarkHandler.bookmarkInList(
                            binding.myWebView.url!!.substring(
                                binding.myWebView.url!!.indexOf(
                                    "/",
                                    8
                                )
                            )
                        )
                    ) {
                        val item = menu.findItem(R.id.action_add_bookmark)
                        item.icon = ResourcesCompat.getDrawable(
                            resources,
                            R.drawable.ic_baseline_bookmark_border_24,
                            requireActivity().theme
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            item.icon!!.setTint(
                                ResourcesCompat.getColor(
                                    resources,
                                    R.color.white,
                                    requireActivity().theme
                                )
                            )
                        }
                        item.title = getString(R.string.remove_bookmark_title)
                    }
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection

                when (menuItem.itemId) {
                    R.id.action_view -> {
                        showViewSwitcher()
                    }
                    R.id.action_search -> {
                        binding.bookSearchView.visibility = View.VISIBLE
                        binding.bookSearchView.isIconified = false
                        binding.bookSearchView.requestFocus()
                        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                    }
                    R.id.action_fullscreen -> {
                        enableFullscreen()
                        isFullscreen = true
                    }
                    R.id.action_add_bookmark -> {
                        if (BookmarkHandler.bookmarkInList(
                                binding.myWebView.url!!.substring(
                                    binding.myWebView.url!!.indexOf(
                                        "/",
                                        8
                                    )
                                )
                            )
                        ) {
                            viewModel.removeBookmark()
                            Toast.makeText(
                                requireActivity(),
                                getString(R.string.bookmark_removed_title),
                                Toast.LENGTH_SHORT
                            ).show()
                            activity?.invalidateOptionsMenu()
                        } else {
                            showAddBookmarkDialog()
                        }
                    }
                    R.id.action_login -> {
                        val dialog = LoginDialog()
                        LoginDialog.callback = { login, password ->
                            viewModel.login(login, password) { result ->
                                Log.d("surprise", "OpdsFragment: 138 login result is $result")
                                activity?.runOnUiThread {
                                    if (result) {
                                        activity?.invalidateOptionsMenu()
                                        Toast.makeText(
                                            requireContext(),
                                            getString(R.string.successful_login_title),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            getString(R.string.failed_login_title),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                        dialog.showNow(requireActivity().supportFragmentManager, LoginDialog.TAG)
                    }
                }
                return true
            }
        }
        activity?.addMenuProvider(menuProvider)
    }

    private fun removeMenu() {
        activity?.removeMenuProvider(menuProvider)
    }

    private fun showInputMethod(v: View?) {
        val imm =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.showSoftInput(v, 0)
    }

    private fun setupUI() {
        binding.myWebView.setBackgroundColor(
            ResourcesCompat.getColor(
                resources,
                R.color.background_color,
                requireActivity().theme
            )
        )
        binding.hideFullscreenBtn.setOnClickListener {
            disableFullscreen()
            it.visibility = View.GONE
        }

        binding.bookSearchView.setOnQueryTextFocusChangeListener { view, b ->
            if (b) {
                (activity as SearchActivity).mBinding.includedToolbar.appBarLayout.visibility =
                    View.GONE
                (activity as SearchActivity).mBinding.includedBnv.bottomNavView.visibility =
                    View.GONE
                showInputMethod(view.findFocus())
            } else {
                (activity as SearchActivity).mBinding.includedToolbar.appBarLayout.visibility =
                    View.VISIBLE
                (activity as SearchActivity).mBinding.includedBnv.bottomNavView.visibility =
                    View.VISIBLE
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
            isViewSetupOpened = false
            crossfadeHide()
        }
        binding.currentViewName.text = viewModes[PreferencesHandler.browserViewMode]
        binding.switchViewLeftBtn.setOnClickListener {
            var currentMode = PreferencesHandler.browserViewMode
            if (currentMode == 0) {
                currentMode = viewModes.size - 1
            } else {
                currentMode -= 1
            }
            PreferencesHandler.browserViewMode = currentMode
            binding.currentViewName.text = viewModes[PreferencesHandler.browserViewMode]
            requireActivity().recreate()
            if(PreferencesHandler.lastWebViewLink != null){
                binding.myWebView.loadUrl(PreferencesHandler.lastWebViewLink!!)
            }
        }
        binding.switchViewRightBtn.setOnClickListener {
            var currentMode = PreferencesHandler.browserViewMode
            if (currentMode == viewModes.size - 1) {
                currentMode = 0
            } else {
                currentMode += 1
            }
            PreferencesHandler.browserViewMode = currentMode
            binding.currentViewName.text = viewModes[PreferencesHandler.browserViewMode]
            requireActivity().recreate()
            if(PreferencesHandler.lastWebViewLink != null){
                binding.myWebView.loadUrl(PreferencesHandler.lastWebViewLink!!)
            }
        }

        binding.massLoadFab.setOnClickListener {
            if (this::mBooks.isInitialized) {
                backdropFragment?.loadBooksList(ArrayList(mBooks.values.toList()))
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    private fun disableFullscreen() {
        binding.myWebView.client.isFullscreen = false
        if(PreferencesHandler.lastWebViewLink != null){
            binding.myWebView.loadUrl(PreferencesHandler.lastWebViewLink!!)
        }
        val decorView: View = requireActivity().window.decorView
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        requireActivity().window.clearFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        (requireActivity() as SearchActivity).supportActionBar?.show()
        val navBar: BottomNavigationView =
            requireActivity().findViewById(R.id.bottom_nav_view)
        navBar.visibility = View.VISIBLE
        isFullscreen = false
    }

    private fun handleLoading() {
        binding.myWebView.setup(delegate = this)
        Log.d(
            "surprise",
            "startBrowsing: last loaded is ${PreferencesHandler.lastWebViewLink}"
        )
        if(PreferencesHandler.lastWebViewLink != null){
            binding.myWebView.loadUrl(PreferencesHandler.lastWebViewLink!!)
            (activity as AppCompatActivity?)?.supportActionBar?.subtitle = PreferencesHandler.lastWebViewLink
        }
    }

    private fun showAddBookmarkDialog() {
        val dialog = AddBookmarkDialog()
        AddBookmarkDialog.bookmarkReservedName = binding.myWebView.title ?:""
        AddBookmarkDialog.link = binding.myWebView.url!!.substring(
            binding.myWebView.url!!.indexOf(
                "/",
                8
            )
        )
        dialog.showNow(requireActivity().supportFragmentManager, "ADD BOOKMARK DIALOG")
    }

    private fun enableFullscreen() {
        binding.myWebView.client.isFullscreen = true
        if(PreferencesHandler.lastWebViewLink != null){
            binding.myWebView.loadUrl(PreferencesHandler.lastWebViewLink!!)
        }
        val decorView: View = requireActivity().window.decorView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val uiOptions =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            decorView.systemUiVisibility = uiOptions
        } else {
            decorView.systemUiVisibility = View.GONE
        }
        (requireActivity() as SearchActivity).supportActionBar?.hide()
        requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        // hide toolbar and bottom menu bar
        requireActivity().actionBar?.hide()
        val navBar: BottomNavigationView =
            requireActivity().findViewById(R.id.bottom_nav_view)
        navBar.visibility = View.GONE
        binding.hideFullscreenBtn.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.title_browser)
        (activity as AppCompatActivity?)?.supportActionBar?.subtitle = PreferencesHandler.lastWebViewLink
        setMenu()
        viewModel.appendDownloadAppendedDelegate(this)
    }

    override fun onPause() {
        super.onPause()
        removeMenu()
        viewModel.removeDownloadAppendedDelegate()
    }

    private fun showViewSwitcher() {
        isViewSetupOpened = true
        crossfadeShow()
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
        const val SEARCH_URL = "/booksearch?ask="
    }

    override fun linkClicked(link: String) {
        Log.d("surprise", "WebViewFragment.kt 405: link is $link")
        showDownloadSetupDialog(link)
    }

    private fun showDownloadSetupDialog(link: String) {
        Log.d("surprise", "WebViewFragment: 376 ${mBooks.size}")
        mBooks.forEach {
            Log.d("surprise", "WebViewFragment: 378 ${it.key}")
        }
        val id = GrammarHandler.getBookIdentifierFromLink(link)
        val bookInfo = mBooks[id]
        if (bookInfo != null) {
            val dialog = BookDownloadSetupDialog()
            dialog.setup(bookInfo, link)
            dialog.showNow(requireActivity().supportFragmentManager, "BOOK DOWNLOAD DETAILS DIALOG")
        } else {
            Log.d("surprise", "WebViewFragment.kt 424: book info is null")
            viewModel.addDownload(link)
        }
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

    override fun notifyRequestError() {
        requireActivity().runOnUiThread {
            showErrorSnackbar()
        }
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

    override fun booksParsed(result: HashMap<String, FoundEntity>) {
        activity?.runOnUiThread {
            mBooks = result
            if (result.isNotEmpty()) {
                binding.massLoadFab.show()
            } else {
                binding.massLoadFab.hide()
            }
        }
    }

    override fun taskAppendFailed() {
        requireActivity().runOnUiThread {
            mBookCheckSnackbar?.dismiss()
            Toast.makeText(
                requireContext(),
                getString(R.string.no_download_links_title),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun keyPressed(keyCode: Int): Boolean {
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
            if ((activity as SearchActivity?)?.openedFromOpds == true) {
                if (isFullscreen) {
                    disableFullscreen()
                }
                (activity as SearchActivity?)?.returnToOpds()
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

    private fun showErrorSnackbar() {
        if (!this::errorSnackbar.isInitialized) {
            errorSnackbar = Snackbar.make(
                binding.root,
                getString(R.string.connection_error_message),
                Snackbar.LENGTH_INDEFINITE
            )
            errorSnackbar.setAction(getString(R.string.reload_title)) {
                if(PreferencesHandler.lastWebViewLink != null){
                    binding.myWebView.loadUrl(PreferencesHandler.lastWebViewLink!!)
                }
            }
            errorSnackbar.setActionTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.genre_text_color,
                    requireActivity().theme
                )
            )
            errorSnackbar.anchorView = requireActivity().findViewById(R.id.bottom_nav_view)
        }
        errorSnackbar.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("Fullscreen", isFullscreen)
        outState.putBoolean("ViewSetup", isViewSetupOpened)
    }

    private fun crossfadeShow() {
        binding.viewSwitcherContainer.apply {
            // Set the content view to 0% opacity but visible, so that it is visible
            // (but fully transparent) during the animation.
            alpha = 0.5f
            translationY = 500F
            visibility = View.VISIBLE

            // Animate the content view to 100% opacity, and clear any animation
            // listener set on the view.
            animate()
                .alpha(1f)
                .translationY(0F)
                .setDuration(shortAnimationDuration.toLong())
                .setListener(null)
        }

        /*val anim = ValueAnimator.ofInt(binding.viewSwitcherContainer.measuredHeight, -100)
        anim.addUpdateListener { valueAnimator ->
            val `val` = valueAnimator.animatedValue as Int
            val layoutParams: ViewGroup.LayoutParams = binding.viewSwitcherContainer.layoutParams
            layoutParams.height = `val`
            binding.viewSwitcherContainer.layoutParams = layoutParams
        }
        anim.duration = shortAnimationDuration.toLong()
        anim.start()*/
    }

    private fun crossfadeHide() {
        binding.viewSwitcherContainer.apply {
            // Set the content view to 0% opacity but visible, so that it is visible
            // (but fully transparent) during the animation.
            alpha = 1f

            // Animate the content view to 100% opacity, and clear any animation
            // listener set on the view.
            animate()
                .alpha(0f)
                .translationY(500F)
                .setDuration(shortAnimationDuration.toLong())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.viewSwitcherContainer.visibility = View.GONE
                    }
                })
        }
    }

    override fun checkBookAvailability(item: DownloadLink, callback: (String) -> Unit) {
        viewModel.checkFormatAvailability(requireContext(), item, callback)
    }

    override fun addToDownloadQueue(item: DownloadLink) {
        viewModel.addToDownloadQueue(item)
    }

    override fun showBookDownloadOptions(book: FoundEntity) {
        val dialog = BookDownloadSetupDialog()
        dialog.setup(book)
        dialog.showNow(requireActivity().supportFragmentManager, "BOOK DOWNLOAD DETAILS DIALOG")
    }
}