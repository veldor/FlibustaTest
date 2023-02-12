package net.veldor.flibusta_test.view.search_fragment

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.SearchAutoComplete
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.internal.ViewUtils.dpToPx
import com.google.android.material.slider.Slider
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentOpdsBinding
import net.veldor.flibusta_test.model.adapter.OpdsSearchResultsAdapter
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.delegate.FoundItemActionDelegate
import net.veldor.flibusta_test.model.delegate.OpdsObserverDelegate
import net.veldor.flibusta_test.model.handler.*
import net.veldor.flibusta_test.model.helper.UrlHelper
import net.veldor.flibusta_test.model.interfaces.BookCheckInterface
import net.veldor.flibusta_test.model.parser.OpdsParser
import net.veldor.flibusta_test.model.selection.*
import net.veldor.flibusta_test.model.selection.filter.*
import net.veldor.flibusta_test.model.selection.subscribe.SubscribeItem
import net.veldor.flibusta_test.model.view_model.OpdsViewModel
import net.veldor.flibusta_test.view.SearchActivity
import net.veldor.flibusta_test.view.components.*
import net.veldor.flibusta_test.view.download_fragments.DownloadScheduleStatementFragment
import net.veldor.tor_client.model.connection.WebResponse
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent.setEventListener
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.*


class OpdsFragment : DirectorySelectFragment(),
    FoundItemActionDelegate,
    OpdsObserverDelegate,
    SearchView.OnQueryTextListener,
    BookCheckInterface {
    private lateinit var menuProvider: MenuProvider
    private var downloadBadgeDrawable: BadgeDrawable? = null
    private var blockedBadgeDrawable: BadgeDrawable? = null
    private var badgeDrawable: BadgeDrawable? = null
    private var linkForLoad: String? = null
    private var bookmarkReservedName: String? = null
    private var mDisableHistoryDialog: AlertDialog? = null
    private var autocompleteComponent: SearchAutoComplete? = null
    private var mLastRequest: RequestItem? = null
    private var bottomSheetCoverBehavior: BottomSheetBehavior<View>? = null
    private var bottomSheetFilterBehavior: BottomSheetBehavior<View>? = null
    private var bottomSheetOpdsBehavior: BottomSheetBehavior<View>? = null
    private var backdropDownloadStateFragment: DownloadScheduleStatementFragment? = null
    private var backdropCoverFragment: CoverBackdropFragment? = null
    private var backdropFilterFragment: FilterBackdropFragment? = null
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var backdropFragment: OpdsDownloadBackdropFragment? = null
    private var mLastQuery: String? = null
    private lateinit var binding: FragmentOpdsBinding
    internal lateinit var viewModel: OpdsViewModel
    private var showHints = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val link = requireActivity().intent.getStringExtra("link")
        if (link != null && link.startsWith("/opds/")) {
            loadLink(link)
            requireActivity().intent.removeExtra("link")
        }
        activity?.invalidateOptionsMenu()
        viewModel = ViewModelProvider(this)[OpdsViewModel::class.java]
        binding = FragmentOpdsBinding.inflate(inflater, container, false)
        setupUI()
        setupObservers()
        restoreValues(savedInstanceState)
        viewModel.drawBadges(this)
        (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.scrollToPressed()
        viewModel.restoreLastDialog()?.show()

        prepareToDownload {
            Log.d("surprise", "OpdsFragment: 108 prepared!")
        }
        return binding.root
    }

    private fun setMenu() {
        requireActivity().invalidateOptionsMenu()
        menuProvider = object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.odps_menu, menu)
                val loginCookie = menu.findItem(R.id.action_login)
                if (PreferencesHandler.authCookie != null) {
                    loginCookie.isVisible = false
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                when (menuItem.itemId) {
                    R.id.action_add_bookmark -> {
                        handleBookmark()
                    }
                    R.id.action_search -> {
                        binding.noSearchResultsFoundTitle.visibility = View.GONE
                        binding.filterListView.clearFocus()
                        binding.quickSettingsPanel.visibility = View.VISIBLE
                        binding.filterListView.visibility = View.GONE
                        binding.filterByType.visibility = View.GONE
                        binding.quickLinksPanel.visibility = View.GONE
                        binding.bookSearchView.visibility = View.VISIBLE
                        binding.bookSearchView.isIconified = false
                        binding.bookSearchView.requestFocus()
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

    private fun handleBookmark() {
        if (BookmarkHandler.bookmarkInList(viewModel.getBookmarkLink())) {
            viewModel.removeBookmark()
            Toast.makeText(
                requireActivity(),
                getString(R.string.bookmark_removed_title),
                Toast.LENGTH_SHORT
            ).show()
            activity?.invalidateOptionsMenu()
            binding.addBookmarkBtn.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_baseline_bookmark_add_24,
                    requireActivity().theme
                )
            )
        } else {
            showAddBookmarkDialog()
        }
    }

    private fun showAddBookmarkDialog() {
        if (viewModel.readyToCreateBookmark()) {
            val dialog = AddBookmarkDialog()
            AddBookmarkDialog.bookmarkReservedName = bookmarkReservedName ?: ""
            AddBookmarkDialog.link = viewModel.getBookmarkLink() ?: ""
            dialog.showNow(requireActivity().supportFragmentManager, "ADD BOOKMARK DIALOG")
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.bookmark_not_ready_title),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showSortDialog() {
        val sortType: Int
        val sortOptions: List<SortOption?>
        val selectedOption: Int
        when {
            (binding.resultsList.adapter as OpdsSearchResultsAdapter).containsGenres() -> {
                sortType = R.id.searchGenre
                sortOptions = SortHandler().getDefaultSortOptions(requireContext())
                selectedOption = SelectedSortTypeHandler.getGenreSortOptionIndex()
            }
            (binding.resultsList.adapter as OpdsSearchResultsAdapter).containsSequences() -> {
                sortType = R.id.searchSequence
                sortOptions = SortHandler().getDefaultSortOptions(requireContext())
                selectedOption = SelectedSortTypeHandler.getSequenceSortOptionIndex()
            }
            (binding.resultsList.adapter as OpdsSearchResultsAdapter).containsAuthors() -> {
                sortType = R.id.searchAuthor
                sortOptions = SortHandler().getAuthorSortOptions(requireContext())
                selectedOption = SelectedSortTypeHandler.getAuthorSortOptionIndex()
            }
            else -> {
                sortType = R.id.searchBook
                sortOptions = SortHandler().getBookSortOptions(requireContext())
                selectedOption = SelectedSortTypeHandler.getBookSortOptionIndex()
            }
        }
        val searchArray: Array<CharSequence> = Array(sortOptions.size) { index ->
            sortOptions[index]!!.name
        }

        val builder = AlertDialog.Builder(requireActivity(), R.style.dialogTheme)
            .setTitle(getString(R.string.sort_list_by_title))
            .setSingleChoiceItems(searchArray, selectedOption) { dialog, selected ->
                dialog.dismiss()
                SelectedSortTypeHandler.saveSortType(
                    sortType,
                    selected
                )
                (binding.resultsList.adapter as OpdsSearchResultsAdapter).sort()
            }
        builder.show()
    }

    private fun showDownloadState() {
        backdropDownloadStateFragment?.binding?.pullUpView?.visibility = View.VISIBLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            backdropDownloadStateFragment?.binding?.root?.background =
                ResourcesCompat.getDrawable(resources, R.drawable.rounded_corner, null)
        }
        bottomSheetOpdsBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.opds_title)
        setupToolbar()
        OpdsStatement.delegate = this
        activity?.invalidateOptionsMenu()
        configureBackdrop()
        setMenu()
    }

    private fun setupToolbar() {
        // toolbar options
        if (PreferencesHandler.connectionType == PreferencesHandler.CONNECTION_MODE_TOR) {
            binding.connectionOptionBtn.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.tor_drawable,
                    activity?.theme
                )
            )
        } else {
            binding.connectionOptionBtn.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.vpn_drawable,
                    activity?.theme
                )
            )
        }
        binding.useFilterBtn.isVisible = PreferencesHandler.toolbarSearchShown
        binding.sortBtn.isVisible = PreferencesHandler.toolbarSortShown
        binding.showBlockedStateBtn.isVisible = PreferencesHandler.toolbarBlockedShown
        binding.downloadStateBtn.isVisible = PreferencesHandler.toolbarDloadStateShown
        binding.addBookmarkBtn.isVisible = PreferencesHandler.toolbarBookmarkShown
        binding.switchResultsLayoutBtn.isVisible =
            PreferencesHandler.toolbarViewConfigShown
        binding.nightModeSwitcher.isVisible = PreferencesHandler.toolbarThemeShown
        binding.readerModeSwitcher.isVisible = PreferencesHandler.toolbarEinkShown
    }

    override fun onPause() {
        super.onPause()
        removeMenu()
        binding.bookSearchView.clearFocus()
    }

    private fun removeMenu() {
        activity?.removeMenuProvider(menuProvider)
    }


    private fun configureBackdrop() {
// Get the fragment reference
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
// Get the download state fragment reference
        backdropDownloadStateFragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.downloadStateFragment) as DownloadScheduleStatementFragment?
        backdropDownloadStateFragment?.let {
            // Get the BottomSheetBehavior from the fragment view
            BottomSheetBehavior.from(it.requireView()).let { bsb ->
                // Set the initial state of the BottomSheetBehavior to HIDDEN
                bsb.state = BottomSheetBehavior.STATE_HIDDEN
                // Set the reference into class attribute (will be used latter)
                bottomSheetOpdsBehavior = bsb
            }
        }
// cover backdrop
        backdropCoverFragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.coverBackdropFragment) as CoverBackdropFragment?
        backdropCoverFragment?.let {
            // Get the BottomSheetBehavior from the fragment view
            BottomSheetBehavior.from(it.requireView()).let { bsb ->
                // Set the initial state of the BottomSheetBehavior to HIDDEN
                bsb.state = BottomSheetBehavior.STATE_HIDDEN
                // Set the reference into class attribute (will be used latter)
                bottomSheetCoverBehavior = bsb
            }
        }
// filter backdrop
        backdropFilterFragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.filterBackdropFragment) as FilterBackdropFragment?
        backdropFilterFragment?.let {
            // Get the BottomSheetBehavior from the fragment view
            BottomSheetBehavior.from(it.requireView()).let { bsb ->
                // Set the initial state of the BottomSheetBehavior to HIDDEN
                bsb.state = BottomSheetBehavior.STATE_HIDDEN
                // Set the reference into class attribute (will be used latter)
                bottomSheetFilterBehavior = bsb
            }
        }
    }

    private fun restoreValues(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(STATE_SEARCH_VALUE)) {
                mLastQuery = savedInstanceState.getString(STATE_SEARCH_VALUE)
            }
        }
    }

    private fun setupObservers() {
        binding.resultsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) binding.massLoadFab.hide() else if (dy < 0 && OpdsStatement.requestState.value != OpdsStatement.STATE_LOADING) binding.massLoadFab.show()
            }
        })

        // буду отслеживать состояние режима
        OpdsStatement.requestState.observe(viewLifecycleOwner) {
            if (it == OpdsStatement.STATE_LOADING) {
                binding.fab.visibility = View.VISIBLE
                binding.swipeLayout.isRefreshing = true
                (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.loadInProgress = true
            } else if (it == OpdsStatement.STATE_CANCELLED) {
                binding.fab.visibility = View.GONE
                binding.swipeLayout.isRefreshing = false
                binding.swipeLayout.isEnabled = OpdsStatement.isNextPageLink()
                (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.loadInProgress = false
            } else if (it == OpdsStatement.STATE_READY) {
                // если выбрана загрузка всех результатов- загружу следующую страницу
                if (PreferencesHandler.opdsPagingType) {
                    binding.fab.visibility = View.GONE
                    binding.swipeLayout.isRefreshing = false
                    binding.swipeLayout.isEnabled = OpdsStatement.isNextPageLink()
                    (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.loadInProgress =
                        false
                } else {
                    if (OpdsStatement.isNextPageLink()) {
                        mLastRequest = RequestItem(
                            OpdsStatement.getNextPageLink()!!,
                            append = true,
                            addToHistory = false
                        )
                        newRequestLaunched(mLastRequest!!)
                        viewModel.request(
                            mLastRequest
                        )
                    } else {
                        binding.fab.visibility = View.GONE
                        binding.swipeLayout.isRefreshing = false
                        binding.swipeLayout.isEnabled = OpdsStatement.isNextPageLink()
                        (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.loadInProgress =
                            false
                    }
                }
            } else if (it == OpdsStatement.STATE_ERROR) {
                Log.d("surprise", "setupObservers: have error!")
                showErrorSnackbar()
            }
        }

        DownloadHandler.liveBookDownloadProgress.observe(viewLifecycleOwner) {
            handleBookDownloadProgress(it)
        }

        OpdsResultsHandler.livePossibleMemoryOverflow.observe(viewLifecycleOwner) {
            if (it) {
                showDisableHistoryDialog()
            }
        }

        DatabaseInstance.mDatabase.downloadedBooksDao().lastDownloadedBookLive?.observe(
            viewLifecycleOwner
        ) {
            (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.markAsDownloaded(it)
        }

        setEventListener(
            requireActivity(),
            KeyboardVisibilityEventListener { isOpen ->
                if (!isOpen) {
                    binding.bookSearchView.clearFocus()
                }
            })
        binding.swipeLayout.setOnRefreshListener {
            if (OpdsStatement.isNextPageLink()) {
                mLastRequest =
                    RequestItem(
                        OpdsStatement.getNextPageLink()!!,
                        append = true,
                        addToHistory = false
                    )
                newRequestLaunched(mLastRequest!!)
                viewModel.request(
                    mLastRequest
                )
                binding.swipeLayout.performHapticFeedback(
                    HapticFeedbackConstants.KEYBOARD_TAP,
                )
                (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.loadInProgress = true
            } else {
                binding.swipeLayout.isRefreshing = false
                (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.loadInProgress = false
            }
        }
    }

    private fun showDisableHistoryDialog() {
        if (mDisableHistoryDialog == null) {
            mDisableHistoryDialog = AlertDialog.Builder(requireContext(), R.style.dialogTheme)
                .setTitle(getString(R.string.disable_catalog_history_title))
                .setMessage(getString(R.string.disable_catalog_history_message))
                .setPositiveButton(getString(R.string.disable_message)) { _, _ ->
                    PreferencesHandler.disableHistoryMessageViewed = true
                    PreferencesHandler.saveOpdsHistory = false
                }.setNegativeButton(getString(R.string.keep_message)) { _, _ ->
                    PreferencesHandler.disableHistoryMessageViewed = true
                }
                .create()
            mDisableHistoryDialog?.show()
        }
    }

    private fun showErrorSnackbar() {

    }

    @SuppressLint("RestrictedApi")
    private fun setupUI() {
        if (PreferencesHandler.isEInk) {
            binding.root.setBackgroundColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    requireActivity().theme
                )
            )
            binding.showArrivalsBtn.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.invertable_black,
                    requireActivity().theme
                )
            )
            binding.showEntitiesByAlphabetBtn.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.invertable_black,
                    requireActivity().theme
                )
            )
            binding.doOpdsSearchBtn.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.invertable_black,
                    requireActivity().theme
                )
            )

            binding.fab.backgroundTintList = ColorStateList.valueOf(
                ResourcesCompat.getColor(
                    requireActivity().resources,
                    R.color.alwaysWhite,
                    requireActivity().theme
                )
            )
            binding.fab.supportImageTintList = ColorStateList.valueOf(
                ResourcesCompat.getColor(
                    requireActivity().resources,
                    R.color.black,
                    requireActivity().theme
                )
            )
            binding.massLoadFab.backgroundTintList = ColorStateList.valueOf(
                ResourcesCompat.getColor(
                    requireActivity().resources,
                    R.color.alwaysWhite,
                    requireActivity().theme
                )
            )
            binding.massLoadFab.setTextColor(
                ColorStateList.valueOf(
                    ResourcesCompat.getColor(
                        requireActivity().resources,
                        R.color.black,
                        requireActivity().theme
                    )
                )
            )

            binding.searchOptionsContainer.setBackgroundColor(
                ResourcesCompat.getColor(resources, R.color.white, requireActivity().theme)
            )
            binding.bookSearchView.queryHint = ""
        } else {
            val myColorStateList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(android.R.attr.state_selected),
                    intArrayOf(),
                ), intArrayOf(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.text_light_color,
                        requireActivity().theme
                    ),
                    ResourcesCompat.getColor(
                        resources,
                        R.color.text_light_color,
                        requireActivity().theme
                    ),
                    ResourcesCompat.getColor(
                        resources,
                        R.color.inactive,
                        requireActivity().theme
                    ),
                )
            )
            binding.searchBook.supportButtonTintList = myColorStateList
            binding.searchAuthor.supportButtonTintList = myColorStateList
            binding.searchGenre.supportButtonTintList = myColorStateList
            binding.searchSequence.supportButtonTintList = myColorStateList

            binding.resultsPagingSwitcher.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.text_light_color,
                    requireActivity().theme
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.resultsPagingSwitcher.buttonTintList = myColorStateList
                binding.resultsPagingSwitcher.trackTintList = myColorStateList
            }

            binding.useFiltersSwitch.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.text_light_color,
                    requireActivity().theme
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.useFiltersSwitch.trackTintList = myColorStateList
            }
        }

        binding.bookSearchView.isSubmitButtonEnabled = false

        val searchLayout = binding.bookSearchView.getChildAt(0) as LinearLayout

        /*val closeBtn = searchLayout.findViewById(R.id.search_close_btn) as ImageView?
        closeBtn?.isEnabled = false
        closeBtn?.setImageDrawable(null)*/
        val buttonStyle: Int = androidx.appcompat.R.attr.buttonBarButtonStyle
        val showAutofillBtn = ImageButton(requireActivity(), null, buttonStyle)

        val hamburgerDrawable = HamburgerButton(
            size = dpToPx(requireContext(), 24).toInt(),
            barThickness = dpToPx(requireContext(), 3),
            barGap = dpToPx(requireContext(), 5)
        )
        hamburgerDrawable.color =
            ResourcesCompat.getColor(resources, R.color.icon_text_color, requireActivity().theme)
        showAutofillBtn.setImageDrawable(
            hamburgerDrawable
        )
        showAutofillBtn.setOnClickListener {
            if (showHints) {
                // animate it
                val animator = ValueAnimator.ofFloat(1.0f, 0.0f)
                animator.interpolator = AccelerateDecelerateInterpolator()
                animator.duration = 300
                animator.addUpdateListener {
                    val progress = it.animatedValue as Float
                    /*val color = interpolateColor(hamburgerColor, crossColor, progress)
                    hamburgerDrawable.color = color*/
                    hamburgerDrawable.progress = progress
                }
                animator.start()
                autocompleteComponent?.setAdapter(null)
            } else {
                // animate it
                val animator = ValueAnimator.ofFloat(0.0f, 1.0f)
                animator.interpolator = AccelerateDecelerateInterpolator()
                animator.duration = 300
                animator.addUpdateListener {
                    val progress = it.animatedValue as Float
                    /*val color = interpolateColor(hamburgerColor, crossColor, progress)
                    hamburgerDrawable.color = color*/
                    hamburgerDrawable.progress = progress
                }
                animator.start()
                // show autofill window
                val searchAdapter =
                    setAutocompleteAdapter()

                autocompleteComponent =
                    binding.bookSearchView.findViewById(androidx.appcompat.R.id.search_src_text)

                binding.bookSearchView.setOnSuggestionListener(object :
                    SearchView.OnSuggestionListener {
                    override fun onSuggestionSelect(i: Int): Boolean {
                        return true
                    }

                    override fun onSuggestionClick(i: Int): Boolean {
                        val value = viewModel.getAutocomplete(
                            when (binding.searchType.checkedRadioButtonId) {
                                R.id.searchBook -> {
                                    OpdsParser.TYPE_BOOK
                                }
                                R.id.searchAuthor -> {
                                    OpdsParser.TYPE_AUTHOR
                                }
                                R.id.searchGenre -> {
                                    OpdsParser.TYPE_GENRE
                                }
                                else -> OpdsParser.TYPE_SEQUENCE
                            }
                        )[i]
                        binding.bookSearchView.setQuery(value, false)
                        return true
                    }
                })
                autocompleteComponent?.threshold = 0
                autocompleteComponent?.dropDownHeight = ViewGroup.LayoutParams.WRAP_CONTENT
                autocompleteComponent?.setDropDownBackgroundResource(R.color.background_color)
                autocompleteComponent?.setAdapter(searchAdapter)
                autocompleteComponent?.showDropDown()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.hints_showed_title),
                    Toast.LENGTH_SHORT
                ).show()
            }
            showHints = !showHints
        }
        searchLayout.addView(showAutofillBtn)

        showAutofillBtn.layoutParams.width = dpToPx(requireContext(), 40).toInt()
        showAutofillBtn.layoutParams.height = dpToPx(requireContext(), 40).toInt()
        // try hide status bar
        //search bar
        if (mLastQuery != null) {
            binding.bookSearchView.setQuery(mLastQuery, false)
        }
        binding.bookSearchView.isSubmitButtonEnabled = false
        binding.bookSearchView.queryHint = getString(R.string.enter_request_title)
        binding.bookSearchView.setOnQueryTextFocusChangeListener { view, b ->
            if (b) {
                (activity as SearchActivity).mBinding.includedToolbar.appBarLayout.visibility =
                    View.GONE
                (activity as SearchActivity).mBinding.includedBnv.bottomNavView.visibility =
                    View.GONE
                binding.searchOptionsContainer.visibility = View.VISIBLE
                binding.swipeLayout.isEnabled = false
                showInputMethod(view.findFocus())
            } else {
                (activity as SearchActivity).mBinding.includedToolbar.appBarLayout.visibility =
                    View.VISIBLE
                (activity as SearchActivity).mBinding.includedBnv.bottomNavView.visibility =
                    View.VISIBLE
                binding.bookSearchView.visibility = View.GONE
                binding.searchOptionsContainer.visibility = View.GONE
                if (OpdsStatement.notInitialized) {
                    binding.quickLinksPanel.visibility = View.VISIBLE
                }
                binding.swipeLayout.isEnabled = true
            }
        }

        binding.bookSearchView.setOnFocusChangeListener { v, hasFocus ->
            Log.d("surprise", "OpdsFragment: 699 focus changed on $hasFocus")
            if (hasFocus) {
                binding.noSearchResultsFoundTitle.visibility = View.GONE
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
                if (binding.searchType.checkedRadioButtonId == R.id.searchAuthor) {
                    // check some words in request. If yes, notify about search author option, what search only by name
                    if (request.split(" ").size > 1) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.author_search_some_words_message),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                addValueToAutocompleteList(request, binding.searchType.checkedRadioButtonId)
                // refresh autofill
                val searchAdapter =
                    setAutocompleteAdapter()
                autocompleteComponent?.setAdapter(searchAdapter)
                bookmarkReservedName = request
                mLastRequest = RequestItem(
                    UrlHelper.getSearchRequest(
                        binding.searchType.checkedRadioButtonId,
                        URLEncoder.encode(request, "utf-8").replace("+", "%20")
                    ),
                    append = false,
                    addToHistory = true
                )
                newRequestLaunched(mLastRequest!!)
                viewModel.request(
                    mLastRequest
                )
                binding.bookSearchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(request: String?): Boolean {
                return true
            }
        })

        binding.massLoadFab.visibility = View.GONE

        binding.useFiltersSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesHandler.isOpdsUseFilter = isChecked
        }
        binding.useFiltersSwitch.isChecked = PreferencesHandler.isOpdsUseFilter
        binding.showBlockedStateBtn.isVisible = PreferencesHandler.isOpdsUseFilter

        binding.fab.setOnClickListener {
            viewModel.cancelSearch()
            binding.fab.hide()
            binding.swipeLayout.isRefreshing = false
            (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.loadInProgress = false
            binding.swipeLayout.isEnabled = OpdsStatement.isNextPageLink()
        }

        binding.massLoadFab.setOnClickListener {
            // transfer book list to backdrop fragment
            backdropFragment?.loadBooksList((binding.resultsList.adapter as OpdsSearchResultsAdapter).getList())
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        // handle entity list display actions
        binding.showEntitiesByAlphabetBtn.setOnClickListener {
            binding.bookSearchView.clearFocus()

            when (binding.searchType.checkedRadioButtonId) {
                R.id.searchAuthor -> {
                    bookmarkReservedName = "Авторы"
                    mLastRequest = RequestItem(
                        "/opds/authorsindex",
                        append = false,
                        addToHistory = true
                    )
                    viewModel.request(
                        mLastRequest
                    )
                }
                R.id.searchGenre -> {
                    bookmarkReservedName = "Жанры"
                    mLastRequest = RequestItem(
                        "/opds/genres",
                        append = false,
                        addToHistory = true
                    )
                    viewModel.request(
                        mLastRequest
                    )
                }
                R.id.searchSequence -> {
                    bookmarkReservedName = "Серии"
                    mLastRequest = RequestItem(
                        "/opds/sequencesindex",
                        append = false,
                        addToHistory = true
                    )
                    viewModel.request(
                        mLastRequest
                    )
                }
            }
            newRequestLaunched(mLastRequest!!)
        }

        // handle search type value change
        binding.searchType.setOnCheckedChangeListener { _, i ->
            if (showHints) {
                val searchAdapter =
                    setAutocompleteAdapter()
                autocompleteComponent?.setAdapter(searchAdapter)
            }
            when (i) {
                R.id.searchBook -> {
                    // change title of new arrivals
                    binding.showArrivalsBtn.text = getString(R.string.new_books_title)
                    binding.showEntitiesByAlphabetBtn.visibility = View.GONE
                }
                R.id.searchAuthor -> {
                    // change title of new arrivals
                    binding.showArrivalsBtn.text = getString(R.string.new_authors_title)
                    binding.showEntitiesByAlphabetBtn.visibility = View.VISIBLE
                    binding.showEntitiesByAlphabetBtn.text =
                        getString(R.string.show_authors_list_title)
                }
                R.id.searchGenre -> {
                    // change title of new arrivals
                    binding.showArrivalsBtn.text = getString(R.string.new_genres_title)
                    binding.showEntitiesByAlphabetBtn.visibility = View.VISIBLE
                    binding.showEntitiesByAlphabetBtn.text =
                        getString(R.string.show_genres_list_title)
                }
                R.id.searchSequence -> {
                    // change title of new arrivals
                    binding.showArrivalsBtn.text = getString(R.string.new_sequences_title)
                    binding.showEntitiesByAlphabetBtn.visibility = View.VISIBLE
                    binding.showEntitiesByAlphabetBtn.text =
                        getString(R.string.show_sequences_list_title)
                }
            }
        }
        binding.showArrivalsBtn.setOnClickListener {
            binding.bookSearchView.clearFocus()
            when (binding.searchType.checkedRadioButtonId) {
                R.id.searchBook -> {
                    bookmarkReservedName = "Новинки"
                    mLastRequest = RequestItem(
                        "/opds/new/0/new",
                        append = false,
                        addToHistory = true
                    )
                    viewModel.request(
                        mLastRequest
                    )
                }
                R.id.searchAuthor -> {
                    bookmarkReservedName = "Новинки по авторам"
                    mLastRequest = RequestItem(
                        "/opds/newauthors",
                        append = false,
                        addToHistory = true
                    )
                    viewModel.request(
                        mLastRequest
                    )
                }
                R.id.searchGenre -> {
                    bookmarkReservedName = "Новинки по жанрам"
                    mLastRequest = RequestItem(
                        "/opds/newgenres",
                        append = false,
                        addToHistory = true
                    )
                    viewModel.request(
                        mLastRequest
                    )
                }
                R.id.searchSequence -> {
                    bookmarkReservedName = "Новинки по сериям"
                    mLastRequest = RequestItem(
                        "/opds/newsequences",
                        append = false,
                        addToHistory = true
                    )
                    viewModel.request(
                        mLastRequest
                    )
                }
            }

            newRequestLaunched(mLastRequest!!)
        }
        binding.resultsPagingSwitcher.setOnCheckedChangeListener { _, b ->

            if (b) {
                binding.resultsPagingSwitcher.text = getString(R.string.load_by_pages_title)
                PreferencesHandler.opdsPagingType = true
            } else {
                if (!PreferencesHandler.coversMessageViewed) {
                    showCoversNotificationDialog()
                }
                binding.resultsPagingSwitcher.text = getString(R.string.load_all_results_title)
                PreferencesHandler.opdsPagingType = false
            }
        }
        binding.resultsPagingSwitcher.isChecked =
            PreferencesHandler.opdsPagingType

        binding.doOpdsSearchBtn.setOnClickListener {
            binding.bookSearchView.setQuery(binding.bookSearchView.query, true)
        }

        Log.d("surprise", "OpdsFragment: 913 results count is ${OpdsStatement.results.size}")
        val a =
            OpdsSearchResultsAdapter(this, requireActivity())
        // recycler setup
        a.setHasStableIds(true)
        // load results if exists
        binding.resultsList.adapter = a
        a.setPressedId(OpdsStatement.getPressedItemId())
        val rowsCount = PreferencesHandler.opdsLayoutRowsCount
        if (rowsCount == 0) {
            binding.resultsList.layoutManager = LinearLayoutManager(requireActivity())
        } else {
            binding.resultsList.layoutManager =
                GridLayoutManager(requireActivity(), rowsCount + 1)
        }

        if (OpdsStatement.notInitialized) {
            binding.quickLinksPanel.visibility = View.VISIBLE
        }

        if (linkForLoad != null) {
            mLastRequest = RequestItem(
                linkForLoad!!,
                append = false,
                addToHistory = true
            )
            viewModel.request(
                mLastRequest
            )
            linkForLoad = null
            newRequestLaunched(mLastRequest!!)
        }

        binding.filterListView.isSubmitButtonEnabled = false

        binding.addBookmarkBtn.setImageDrawable(
            if (BookmarkHandler.bookmarkInList(viewModel.getBookmarkLink())) {
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_baseline_bookmark_border_24,
                    requireActivity().theme
                )
            } else {
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_baseline_bookmark_add_24,
                    requireActivity().theme
                )
            }
        )
        binding.addBookmarkBtn.setOnClickListener {
            handleBookmark()
        }

        binding.showBlockedStateBtn.setOnClickListener {
            if (PreferencesHandler.showFilterStatistics) {
                backdropFilterFragment?.updateList()
                bottomSheetFilterBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            } else {
                Toast.makeText(
                    requireContext(),
                    String.format(
                        Locale.ENGLISH,
                        getString(R.string.filter_statistics_disabled_pattern),
                        OpdsStatement.getBlockedResultsSize()
                    ),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }

        binding.readerModeSwitcher.setOnClickListener {
            PreferencesHandler.isEInk = !PreferencesHandler.isEInk
            requireActivity().recreate()
        }

        binding.sortBtn.setOnClickListener {
            showSortDialog()
        }

        binding.connectionOptionBtn.setOnClickListener {
            if (PreferencesHandler.connectionType == PreferencesHandler.CONNECTION_MODE_TOR) {
                PreferencesHandler.connectionType = PreferencesHandler.CONNECTION_MODE_VPN
                binding.connectionOptionBtn.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.vpn_drawable,
                        activity?.theme
                    )
                )
                Toast.makeText(
                    requireContext(),
                    getString(R.string.switched_to_vpn_message),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                PreferencesHandler.connectionType = PreferencesHandler.CONNECTION_MODE_TOR
                binding.connectionOptionBtn.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.tor_drawable,
                        activity?.theme
                    )
                )
                Toast.makeText(
                    requireContext(),
                    getString(R.string.switched_to_tor_message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.downloadStateBtn.setOnClickListener {
            showDownloadState()
        }

        binding.nightModeSwitcher.setOnClickListener {
            when (PreferencesHandler.nightMode) {
                PreferencesHandler.NIGHT_THEME_DAY -> {
                    PreferencesHandler.nightMode = PreferencesHandler.NIGHT_THEME_NIGHT
                    AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_YES
                    )
                }
                PreferencesHandler.NIGHT_THEME_NIGHT -> {
                    PreferencesHandler.nightMode = PreferencesHandler.NIGHT_THEME_DAY
                    AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_NO
                    )
                }
                else -> {
                    PreferencesHandler.nightMode = PreferencesHandler.NIGHT_THEME_NIGHT
                    AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_YES
                    )
                }
            }
            requireActivity().recreate()
        }

        binding.switchResultsLayoutBtn.setOnClickListener {
            showResultsViewCustomizeDialog()
        }

        //setup filter list option
        binding.useFilterBtn.setOnClickListener {
            binding.quickSettingsPanel.visibility = View.GONE
            binding.filterListView.visibility = View.VISIBLE
            binding.filterByType.visibility = View.VISIBLE
            binding.quickSettingsPanel.visibility = View.GONE
            binding.quickLinksPanel.visibility = View.GONE
            binding.filterListView.isIconified = false
            binding.filterListView.requestFocus()
            (binding.resultsList.adapter as OpdsSearchResultsAdapter).setFilterEnabled(true)
        }

        binding.filterListView.setOnCloseListener {
            (binding.resultsList.adapter as OpdsSearchResultsAdapter).setFilterEnabled(false)
            binding.filterListView.visibility = View.GONE
            binding.filterByType.visibility = View.GONE
            binding.quickSettingsPanel.visibility = View.VISIBLE
            return@setOnCloseListener true
        }

        binding.filterByType.setOnCheckedChangeListener { _, selected ->
            binding.filterListView.setQuery("", false)
            (binding.resultsList.adapter as OpdsSearchResultsAdapter).setFilterSelection(selected)
        }

        binding.filterListView.setOnQueryTextListener(this)

        // запрещу обновление через swipeLayout по умолчанию
        binding.swipeLayout.isEnabled = false

        binding.quickFoundBooksBtn.setOnClickListener {
            binding.searchOptionsContainer.visibility = View.VISIBLE
            binding.searchBook.performClick()
            binding.bookSearchView.visibility = View.VISIBLE
            binding.bookSearchView.isIconified = false
            binding.bookSearchView.requestFocus()
            binding.quickLinksPanel.visibility = View.GONE
        }
        binding.quickFoundAuthorsBtn.setOnClickListener {
            binding.searchOptionsContainer.visibility = View.VISIBLE
            binding.searchAuthor.performClick()
            binding.bookSearchView.visibility = View.VISIBLE
            binding.bookSearchView.isIconified = false
            binding.bookSearchView.requestFocus()
            binding.quickLinksPanel.visibility = View.GONE
        }
        binding.quickFoundGenresBtn.setOnClickListener {
            binding.searchOptionsContainer.visibility = View.VISIBLE
            binding.searchGenre.performClick()
            binding.bookSearchView.visibility = View.VISIBLE
            binding.bookSearchView.isIconified = false
            binding.bookSearchView.requestFocus()
            binding.quickLinksPanel.visibility = View.GONE
        }
        binding.quickFoundSequencesBtn.setOnClickListener {
            binding.searchOptionsContainer.visibility = View.VISIBLE
            binding.searchSequence.performClick()
            binding.bookSearchView.visibility = View.VISIBLE
            binding.bookSearchView.isIconified = false
            binding.bookSearchView.requestFocus()
            binding.quickLinksPanel.visibility = View.GONE
        }
        binding.quickShowNewBooks.setOnClickListener {
            bookmarkReservedName = "Новинки"
            mLastRequest = RequestItem(
                "/opds/new/0/new",
                append = false,
                addToHistory = true
            )
            viewModel.request(
                mLastRequest
            )
            newRequestLaunched(mLastRequest!!)
        }
        binding.quickShowNewAuthors.setOnClickListener {
            bookmarkReservedName = "Новинки по авторам"
            mLastRequest = RequestItem(
                "/opds/newauthors",
                append = false,
                addToHistory = true
            )
            viewModel.request(
                mLastRequest
            )
            newRequestLaunched(mLastRequest!!)
        }
        binding.quickShowNewGenres.setOnClickListener {
            bookmarkReservedName = "Новинки по жанрам"
            mLastRequest = RequestItem(
                "/opds/newgenres",
                append = false,
                addToHistory = true
            )
            viewModel.request(
                mLastRequest
            )
            newRequestLaunched(mLastRequest!!)
        }
        binding.quickShowNewSequences.setOnClickListener {
            bookmarkReservedName = "Новинки по сериям"
            mLastRequest = RequestItem(
                "/opds/newsequences",
                append = false,
                addToHistory = true
            )
            viewModel.request(
                mLastRequest
            )
            newRequestLaunched(mLastRequest!!)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showResultsViewCustomizeDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_switch_layout_grid, null)

        // switch covers
        val showCoversSwitch = view.findViewById<CheckBox>(R.id.showCoversCheckbox)
        showCoversSwitch?.isChecked = PreferencesHandler.showCovers
        showCoversSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.showCovers = state
            (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.notifyDataSetChanged()
        }
        // switch show authors
        val showAuthorsSwitch = view.findViewById<CheckBox>(R.id.showAuthorsCheckbox)
        showAuthorsSwitch?.isChecked = PreferencesHandler.showAuthors
        showAuthorsSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.showAuthors = state
            (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.notifyDataSetChanged()
        }
        // switch show translators
        val showTranslatorsSwitch = view.findViewById<CheckBox>(R.id.showTranslatorsCheckbox)
        showTranslatorsSwitch?.isChecked = PreferencesHandler.showFoundBookTranslators
        showTranslatorsSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.showFoundBookTranslators = state
            (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.notifyDataSetChanged()
        }
        // switch show sequences
        val showSequencesSwitch = view.findViewById<CheckBox>(R.id.showSequencesCheckbox)
        showSequencesSwitch?.isChecked = PreferencesHandler.showFoundBookSequences
        showSequencesSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.showFoundBookSequences = state
            (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.notifyDataSetChanged()
        }
        // switch show genres
        val showGenresSwitch = view.findViewById<CheckBox>(R.id.showGenresCheckbox)
        showGenresSwitch?.isChecked = PreferencesHandler.showFoundBookGenres
        showGenresSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.showFoundBookGenres = state
            (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.notifyDataSetChanged()
        }
        // switch show format
        val showFormatSwitch = view.findViewById<CheckBox>(R.id.showFormatCheckbox)
        showFormatSwitch?.isChecked = PreferencesHandler.showFoundBookFormat
        showFormatSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.showFoundBookFormat = state
            (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.notifyDataSetChanged()
        }
        // switch show downloads
        val showDownloadsSwitch = view.findViewById<CheckBox>(R.id.showDownloadsCheckbox)
        showDownloadsSwitch?.isChecked = PreferencesHandler.showFoundBookDownloads
        showDownloadsSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.showFoundBookDownloads = state
            (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.notifyDataSetChanged()
        }
        // switch show size
        val showSizeSwitch = view.findViewById<CheckBox>(R.id.showSizeCheckbox)
        showSizeSwitch?.isChecked = PreferencesHandler.showFoundBookSize
        showSizeSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.showFoundBookSize = state
            (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.notifyDataSetChanged()
        }
        // switch show size
        val showFormatsSwitch = view.findViewById<CheckBox>(R.id.showAvailableFormatsCheckbox)
        showFormatsSwitch?.isChecked = PreferencesHandler.showFoundBookAvailableFormats
        showFormatsSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.showFoundBookAvailableFormats = state
            (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.notifyDataSetChanged()
        }
        // switch show read
        val showReadSwitch = view.findViewById<CheckBox>(R.id.showReadCheckbox)
        showReadSwitch?.isChecked = PreferencesHandler.showFoundBookReadBtn
        showReadSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.showFoundBookReadBtn = state
            (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.notifyDataSetChanged()
        }
        // switch show downloaded
        val showDownloadSwitch = view.findViewById<CheckBox>(R.id.showDownloadCheckbox)
        showDownloadSwitch?.isChecked = PreferencesHandler.showFoundBookDownloadBtn
        showDownloadSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.showFoundBookDownloadBtn = state
            (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.notifyDataSetChanged()
        }
        // switch show downloaded
        val showElementDescriptionSwitch = view.findViewById<CheckBox>(R.id.showElementDescription)
        showElementDescriptionSwitch?.isChecked = PreferencesHandler.showElementDescription
        showElementDescriptionSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.showElementDescription = state
            (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.notifyDataSetChanged()
        }

        // switch show element btn
        val showElementBtnSwitch = view.findViewById<CheckBox>(R.id.showElementBtn)
        showElementBtnSwitch?.isChecked = !PreferencesHandler.hideOpdsResultsButtons
        showElementBtnSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.hideOpdsResultsButtons = !state
            (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.notifyDataSetChanged()
        }

        val slider = view.findViewById<Slider>(R.id.seekBar)

        slider.value =
            (PreferencesHandler.opdsLayoutRowsCount + 1).toFloat()

        slider?.setLabelFormatter { value: Float ->
            val longValue = value.toInt() - 1
            PreferencesHandler.opdsLayoutRowsCount = longValue
            if (longValue == 0) {
                binding.resultsList.layoutManager = LinearLayoutManager(requireActivity())
            } else {
                binding.resultsList.layoutManager =
                    GridLayoutManager(requireActivity(), longValue + 1)
            }
            return@setLabelFormatter (longValue + 1).toString()
        }

        AlertDialog.Builder(requireActivity(), R.style.dialogTheme)
            .setTitle(getString(R.string.select_result_layout_rows_title))
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showCoversNotificationDialog() {
        AlertDialog.Builder(requireContext(), R.style.dialogTheme)
            .setTitle(getString(R.string.covers_notification_title))
            .setMessage(getString(R.string.covers_notification_message))
            .setPositiveButton(getString(R.string.show_covers_title)) { _, _ ->
                PreferencesHandler.coversMessageViewed = true
                PreferencesHandler.showCovers = true
            }.setNegativeButton(getString(R.string.hide_covers_message)) { _, _ ->
                PreferencesHandler.coversMessageViewed = true
                PreferencesHandler.showCovers = false
            }.setNeutralButton(getString(R.string.load_covers_by_request_message)) { _, _ ->
                PreferencesHandler.coversMessageViewed = true
                PreferencesHandler.showCoversByRequest = true
            }
            .show()
    }

    private fun setAutocompleteAdapter(): ArrayAdapter<String> {
        val autocomplete = viewModel.getAutocomplete(
            when (binding.searchType.checkedRadioButtonId) {
                R.id.searchBook -> OpdsParser.TYPE_BOOK
                R.id.searchAuthor -> OpdsParser.TYPE_AUTHOR
                R.id.searchGenre -> OpdsParser.TYPE_GENRE
                else -> OpdsParser.TYPE_SEQUENCE
            }
        )
        return ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            autocomplete
        )
    }

    private fun showInputMethod(v: View?) {
        val imm = App.instance.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.showSoftInput(v, 0)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (this::binding.isInitialized) {
            outState.putString(STATE_SEARCH_VALUE, binding.bookSearchView.query.toString())
        }
    }

    override fun buttonPressed(item: FoundEntity) {
        if (item.type != OpdsParser.TYPE_BOOK) {
            OpdsStatement.setPressedItem(item)
        }
        when (item.type) {
            OpdsParser.TYPE_AUTHOR -> {
                if (item.link?.startsWith("/opds/new/0/newauthors") == true) {
                    // go to link
                    bookmarkReservedName = "Книги автора ${item.name}"
                    mLastRequest = RequestItem(
                        item.link!!,
                        append = false,
                        addToHistory = true
                    )
                    viewModel.request(
                        mLastRequest
                    )
                    newRequestLaunched(mLastRequest!!)
                } else {
                    // выдам список действий по автору
                    showAuthorViewSelect(item, item)
                }
            }
            OpdsParser.TYPE_BOOK -> {
                if (PreferencesHandler.skipDownloadSetup && PreferencesHandler.rememberFavoriteFormat && PreferencesHandler.favoriteFormat != null) {
                    viewModel.addToDownloadQueue(item.getFavoriteLink())
                } else {
                    downloadBookButtonHandle(item)
                }
            }
            else -> {
                bookmarkReservedName = item.name
                // перейду по ссылке
                mLastRequest = RequestItem(
                    item.link!!,
                    append = false,
                    addToHistory = true
                )
                viewModel.request(
                    mLastRequest
                )
                newRequestLaunched(mLastRequest!!)
            }
        }
    }

    private fun downloadBookButtonHandle(book: FoundEntity) {
        prepareToDownload {
            activity?.runOnUiThread {
                showBookDownloadOptions(book)
            }
        }
    }


    override fun showBookDownloadOptions(book: FoundEntity) {
        val dialog = BookDownloadSetupDialog()
        dialog.setup(book)
        dialog.showNow(requireActivity().supportFragmentManager, "BOOK DOWNLOAD DETAILS DIALOG")
    }

    override fun imageClicked(item: FoundEntity) {
        if (item.coverUrl != null) {
            backdropCoverFragment?.setTarget(item)
            bottomSheetCoverBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            Toast.makeText(requireContext(), getString(R.string.no_cover_title), Toast.LENGTH_LONG)
                .show()
        }
    }

    override fun itemPressed(item: FoundEntity) {
        OpdsStatement.setPressedItem(item)
        bookmarkReservedName = item.name
        when (item.type) {
            OpdsParser.TYPE_AUTHOR -> {
                if (item.link?.startsWith("/opds/new/0/newauthors") == true) {
                    // go to link
                    mLastRequest = RequestItem(
                        item.link!!,
                        append = false,
                        addToHistory = true
                    )
                    viewModel.request(
                        mLastRequest
                    )
                } else {
                    // выдам список действий по автору
                    showAuthorViewSelect(item, item)
                }
                newRequestLaunched(mLastRequest!!)
            }
            else -> {
                mLastRequest = RequestItem(
                    item.link!!,
                    append = false,
                    addToHistory = true
                )
                newRequestLaunched(mLastRequest!!)
                // перейду по ссылке
                viewModel.request(
                    mLastRequest
                )
            }
        }
    }

    override fun buttonLongPressed(item: FoundEntity, target: String, view: View) {
        binding.root.performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP
        )
        view.setOnCreateContextMenuListener { menu, _, _ ->
            val addFilterMenuName: String
            val blacklistItemDialog = AddBlacklistItemDialog()
            AddBlacklistItemDialog.callback = {
                activity?.runOnUiThread {
                    if (PreferencesHandler.isOpdsUseFilter) {
                        (binding.resultsList.adapter as OpdsSearchResultsAdapter).hide(item)
                        OpdsStatement.addFilteredResult(item)
                    }
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.added_to_filter_list_title),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            when (target) {
                "name" -> {
                    AddBlacklistItemDialog.type = BlacklistItem.TYPE_BOOK
                    addFilterMenuName = getString(R.string.add_name_to_blacklist_title)
                    AddBlacklistItemDialog.value = item.name
                }
                "author" -> {
                    AddBlacklistItemDialog.type = BlacklistItem.TYPE_AUTHOR
                    addFilterMenuName = getString(R.string.add_author_to_blacklist_title)
                    AddBlacklistItemDialog.value = item.author
                }
                "genre" -> {
                    AddBlacklistItemDialog.type = BlacklistItem.TYPE_GENRE
                    addFilterMenuName = getString(R.string.add_genre_to_blacklist_title)
                    AddBlacklistItemDialog.value = item.genreComplex
                }
                else -> {
                    AddBlacklistItemDialog.type = BlacklistItem.TYPE_SEQUENCE
                    addFilterMenuName = getString(R.string.add_sequence_to_blacklist_title)
                    AddBlacklistItemDialog.value = item.sequencesComplex
                }
            }
            var menuItem: MenuItem =
                menu.add(addFilterMenuName)
            menuItem.setOnMenuItemClickListener {
                blacklistItemDialog.showNow(
                    requireActivity().supportFragmentManager,
                    AddBlacklistItemDialog.TAG
                )
                true
            }
            val addSubscribeMenuName: String
            val subscribeDialog = AddSubscribeItemDialog()
            AddSubscribeItemDialog.callback = {
                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.added_to_subscribes_list_title),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            when (target) {
                "name" -> {
                    AddSubscribeItemDialog.type = SubscribeItem.TYPE_BOOK
                    addSubscribeMenuName = getString(R.string.option_subscribe_book_name)
                    AddSubscribeItemDialog.value = item.name
                }
                "author" -> {
                    AddSubscribeItemDialog.type = SubscribeItem.TYPE_AUTHOR
                    addSubscribeMenuName = getString(R.string.option_subscribe_author)
                    AddSubscribeItemDialog.value = item.author
                }
                "genre" -> {
                    AddSubscribeItemDialog.type = SubscribeItem.TYPE_GENRE
                    addSubscribeMenuName = getString(R.string.option_subscribe_genre)
                    AddSubscribeItemDialog.value = item.genreComplex
                }
                else -> {
                    AddSubscribeItemDialog.type = SubscribeItem.TYPE_SEQUENCE
                    addSubscribeMenuName = getString(R.string.option_subscribe_sequence)
                    AddSubscribeItemDialog.value = item.sequencesComplex
                }
            }
            menuItem =
                menu.add(addSubscribeMenuName)
            menuItem.setOnMenuItemClickListener {
                subscribeDialog.showNow(
                    requireActivity().supportFragmentManager,
                    AddSubscribeItemDialog.TAG
                )
                true
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            view.showContextMenu(view.pivotX, view.pivotY)
        } else {
            view.showContextMenu()
        }
    }

    override fun menuItemPressed(item: FoundEntity, button: View) {
        // show options for this item
        val dialog = OpdsEntityOptionsDialog()
        OpdsEntityOptionsDialog.foundEntity = item
        dialog.showNow(requireActivity().supportFragmentManager, "ENTITY OPTIONS DIALOG")
    }

    override fun loadMoreBtnClicked() {
        if (OpdsStatement.isNextPageLink()) {
            mLastRequest = RequestItem(
                OpdsStatement.getNextPageLink()!!,
                append = true,
                addToHistory = false
            )
            newRequestLaunched(mLastRequest!!)
            viewModel.request(
                mLastRequest
            )
            binding.root.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP
            )
        }
    }

    private fun newRequestLaunched(request: RequestItem) {
        binding.noSearchResultsFoundTitle.visibility = View.GONE
        OpdsStatement.notInitialized = false
        binding.quickLinksPanel.visibility = View.GONE
        // disable filter
        if (!request.append) {
            // промотаю страницу до верха
            // проверю закладки
            if (BookmarkHandler.bookmarkInList(request.request)) {
                binding.addBookmarkBtn.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_baseline_bookmark_border_24,
                        requireActivity().theme
                    )
                )
            } else {
                binding.addBookmarkBtn.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_baseline_bookmark_add_24,
                        requireActivity().theme
                    )
                )
            }

            (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.clearList()
            showBadge(0)
        }
        binding.filterListView.visibility = View.GONE
        binding.filterListView.setQuery("", false)
        binding.filterByType.visibility = View.GONE
        binding.quickSettingsPanel.visibility = View.VISIBLE

        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        binding.fab.show()
        binding.massLoadFab.hide()
        (binding.resultsList.adapter as OpdsSearchResultsAdapter).loadInProgress = true
        activity?.invalidateOptionsMenu()
    }

    override fun authorClicked(item: FoundEntity) {
        if (item.authors.size == 1) {
            showAuthorViewSelect(item.authors[0], item)
        } else {
            showSelectAuthorFromList(item.authors, item)
        }
    }

    private fun showSelectAuthorFromList(authors: ArrayList<FoundEntity>, item: FoundEntity?) {
        // создам диалоговое окно
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.dialogTheme)
        dialogBuilder.setTitle(R.string.select_authors_choose_message)
        // получу сисок имён авторов
        val iterator: Iterator<FoundEntity> = authors.iterator()
        val authorsList = ArrayList<String?>()
        while (iterator.hasNext()) {
            val a = iterator.next()
            authorsList.add(a.name)
        }
        // покажу список выбора автора
        dialogBuilder.setItems(authorsList.toTypedArray()) { _: DialogInterface?, i: Int ->
            showAuthorViewSelect(authors[i], item)
        }
        dialogBuilder.show()
    }

    override fun sequenceClicked(item: FoundEntity) {
        if (item.sequences.size == 1) {
            bookmarkReservedName = item.sequences[0].name
            OpdsStatement.setPressedItem(item)
            mLastRequest = RequestItem(
                item.sequences[0].link!!,
                append = false,
                addToHistory = true
            )
            viewModel.request(
                mLastRequest
            )
            newRequestLaunched(mLastRequest!!)

        } else {
            showSelectSequenceFromList(item.sequences, item)
        }
    }

    private fun showSelectSequenceFromList(sequences: ArrayList<FoundEntity>, item: FoundEntity?) {
        // создам диалоговое окно
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.dialogTheme)
        dialogBuilder.setTitle(R.string.select_sequences_choose_message)
        // получу сисок имён авторов
        val iterator: Iterator<FoundEntity> = sequences.iterator()
        val list = ArrayList<String?>()
        while (iterator.hasNext()) {
            val a = iterator.next()
            list.add(a.name)
        }
        // покажу список выбора автора
        dialogBuilder.setItems(list.toTypedArray()) { _: DialogInterface?, i: Int ->
            OpdsStatement.setPressedItem(item)
            bookmarkReservedName = sequences[i].name
            mLastRequest = RequestItem(
                sequences[i].link!!,
                append = false,
                addToHistory = true
            )
            viewModel.request(
                mLastRequest
            )
            newRequestLaunched(mLastRequest!!)
        }
        dialogBuilder.show()
    }

    override fun nameClicked(item: FoundEntity) {
        // load info about book in backdrop
        if (item.link != null) {
            showBookDetailsDialog(item)
        }
    }

    private fun showBookDetailsDialog(item: FoundEntity) {
        val dialog = BookDetailsDialog()
        BookDetailsDialog.mTitle = getString(R.string.book_details_title)
        BookDetailsDialog.book = item
        dialog.showNow(requireActivity().supportFragmentManager, "BOOK DETAILS DIALOG")
    }

    @Suppress("KotlinConstantConditions")
    override fun rightButtonPressed(item: FoundEntity) {
        if (!item.downloaded) {
            item.downloaded = !item.downloaded
            Toast.makeText(requireContext(), R.string.mark_as_downloaded_title, Toast.LENGTH_SHORT)
                .show()
            viewModel.markDownloaded(item)
            (binding.resultsList.adapter as OpdsSearchResultsAdapter).markAsDownloaded(item)
            if (PreferencesHandler.isOpdsUseFilter && PreferencesHandler.isHideDownloaded) {
                OpdsStatement.addFilteredResult(item)
            }
        } else {
            item.downloaded = !item.downloaded
            Toast.makeText(
                requireContext(),
                R.string.mark_as_no_downloaded_title,
                Toast.LENGTH_SHORT
            ).show()
            viewModel.markNoDownloaded(item)
            (binding.resultsList.adapter as OpdsSearchResultsAdapter).markAsNoDownloaded(item)
        }
    }

    @Suppress("KotlinConstantConditions")
    override fun leftButtonPressed(item: FoundEntity) {
        if (item.read) {
            item.read = !item.read
            Toast.makeText(requireContext(), R.string.mark_as_unread_title, Toast.LENGTH_SHORT)
                .show()
            viewModel.markUnread(item)
            (binding.resultsList.adapter as OpdsSearchResultsAdapter).markBookUnread(item)
        } else {
            item.read = !item.read
            Toast.makeText(requireContext(), R.string.mark_as_read_title, Toast.LENGTH_SHORT).show()
            viewModel.markRead(item)
            (binding.resultsList.adapter as OpdsSearchResultsAdapter).markBookRead(item)
            if (PreferencesHandler.isOpdsUseFilter && PreferencesHandler.isHideRead) {
                OpdsStatement.addFilteredResult(item)
            }
        }
    }

    override fun scrollTo(indexOf: Int) {
        Log.d("surprise", "scrollTo: i scroll to $indexOf")
        binding.resultsList.scrollToPosition(indexOf)
    }

    override fun fastDownload(link: DownloadLink) {
        prepareToDownload {
            viewModel.addToDownloadQueue(link)
        }
    }

    private fun showAuthorViewSelect(author: FoundEntity, item: FoundEntity?) {
        // создам диалоговое окно
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.dialogTheme)
        dialogBuilder
            .setTitle(author.author)
            .setItems(mAuthorViewTypes) { _: DialogInterface?, which: Int ->
                OpdsStatement.setPressedItem(item)
                loadAuthor(
                    which,
                    author
                )
            }
        dialogBuilder.create().show()
    }

    private fun loadAuthor(which: Int, author: FoundEntity) {
        bookmarkReservedName = author.name
        var url: String? = null
        val link = Regex("\\D").replace(author.link!!, "")
        Log.d("surprise", "loadAuthor: link is $link")
        when (which) {
            0 -> {
                url = "/opds/authorsequences/$link"
            }
            1 -> {
                url = "/opds/author/$link/authorsequenceless"
            }
            2 -> {
                url = "/opds/author/$link/alphabet"
            }
            3 -> {
                url = "/opds/author/$link/time"
            }
        }
        if (url != null) {
            mLastRequest = RequestItem(
                url,
                append = false,
                addToHistory = true
            )
            viewModel.request(
                mLastRequest
            )
            newRequestLaunched(mLastRequest!!)
        }
    }

    fun open(url: String) {
        mLastRequest = RequestItem(
            url,
            append = false,
            addToHistory = true
        )
        viewModel.request(
            mLastRequest
        )
        newRequestLaunched(mLastRequest!!)
    }

    fun keyPressed(keyCode: Int): Boolean {
        if (PreferencesHandler.isEInk) {
            // scroll list when volume button pressed
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_PAGE_UP) {
                scrollUp()
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_PAGE_DOWN) {
                scrollDown()
            }
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (viewModel.loadInProgress()) {
                viewModel.cancelSearch()
                (binding.resultsList.adapter as OpdsSearchResultsAdapter).loadInProgress = false
                binding.fab.hide()
                if ((binding.resultsList.adapter as OpdsSearchResultsAdapter).containsBooks()) {
                    binding.massLoadFab.show()
                }
                return true
            }
            if (bottomSheetFilterBehavior != null && bottomSheetFilterBehavior?.state != BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetFilterBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                return true
            }
            if (bottomSheetOpdsBehavior != null && bottomSheetOpdsBehavior?.state != BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetOpdsBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                return true
            }
            if (bottomSheetBehavior != null && bottomSheetBehavior?.state != BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                return true
            }
            if (bottomSheetCoverBehavior != null && bottomSheetCoverBehavior?.state != BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetCoverBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                return true
            }
            // если открыта нижняя вкладка с загрузка
            // если доступен возврат назад- возвращаюсь, если нет- закрываю приложение
            if (!HistoryHandler.isEmpty) {
                val lastResults = HistoryHandler.lastPage
                (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.clearList()
                OpdsStatement.load(lastResults)
                (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.setPressedId(lastResults?.pressedItemId)
                if (PreferencesHandler.saveOpdsHistory) {
                    (binding.resultsList.adapter as OpdsSearchResultsAdapter?)?.scrollToPressed()
                    showBlockedBadge(OpdsStatement.getBlockedResultsSize())
                    updateDownloadBadge()
                    if (lastResults?.nextPageLink != null) {
                        binding.swipeLayout.isEnabled = true
                    }
                } else {
                    OpdsStatement.prepareRequestFromHistory()
                    // load last request
                    mLastRequest = RequestItem(
                        OpdsStatement.getCurrentRequest()!!,
                        append = false,
                        addToHistory = false
                    )
                    newRequestLaunched(mLastRequest!!)
                    viewModel.request(
                        mLastRequest
                    )
                }
                return true
            }
            showCloseAppDialog()
//            if (mConfirmExit != 0L) {
//                if (mConfirmExit > System.currentTimeMillis() - 3000) {
//                    // выйду из приложения
//                    val startMain = Intent(Intent.ACTION_MAIN)
//                    startMain.addCategory(Intent.CATEGORY_HOME)
//                    startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                    startActivity(startMain)
//                } else {
//                    Toast.makeText(
//                        requireContext(),
//                        getString(R.string.press_back_again_for_exit_title),
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    mConfirmExit = System.currentTimeMillis()
//                }
//            } else {
//                Toast.makeText(
//                    requireContext(),
//                    getString(R.string.press_back_again_for_exit_title),
//                    Toast.LENGTH_SHORT
//                ).show()
//                mConfirmExit = System.currentTimeMillis()
//            }
        }
        return false
    }

    private fun showCloseAppDialog() {
        AlertDialog.Builder(requireContext(), R.style.dialogTheme)
            .setTitle(R.string.exit_app_title)
            .setMessage(R.string.exit_app_message)
            .setPositiveButton(R.string.cancel, null)
            .setNegativeButton(R.string.close_app_message) { _, _ ->
                if (Build.VERSION.SDK_INT >= 21) {
                    activity?.finishAndRemoveTask()
                } else {
                    CloseAppHandler.closeApp(requireContext())
                }
            }
            .show()
    }

    private fun scrollUp() {
        val manager = binding.resultsList.layoutManager as LinearLayoutManager?
        if (manager != null) {
            val position = manager.findFirstCompletelyVisibleItemPosition()
            if (position > 0) {
                manager.scrollToPositionWithOffset(position - 1, 10)
            }
        }
    }

    private fun scrollDown() {
        val manager = binding.resultsList.layoutManager as LinearLayoutManager?
        if (manager != null) {
            val position = manager.findFirstCompletelyVisibleItemPosition()
            val adapter = binding.resultsList.adapter
            if (adapter != null) {
                if (position < adapter.itemCount - 1) {
                    manager.scrollToPositionWithOffset(position + 1, 10)
                }
            }
        }
    }


    @Suppress("KotlinConstantConditions")
    companion object {

        const val STATE_SEARCH_VALUE = "search value"

        private val mAuthorViewTypes = arrayOf(
            "Книги по сериям",
            "Книги вне серий",
            "Книги по алфавиту",
            "Книги по дате поступления"
        )
    }

    private fun addValueToAutocompleteList(value: String, type: Int) {
        // занесу значение в список автозаполнения
        XMLHandler.putSearchValue(
            value, when (type) {
                R.id.searchBook -> OpdsParser.TYPE_BOOK
                R.id.searchAuthor -> OpdsParser.TYPE_AUTHOR
                R.id.searchGenre -> OpdsParser.TYPE_GENRE
                else -> OpdsParser.TYPE_SEQUENCE
            }
        )
    }

    private fun loadLink(link: String) {
        linkForLoad = link
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        (binding.resultsList.adapter as OpdsSearchResultsAdapter).filter.filter(query)
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        (binding.resultsList.adapter as OpdsSearchResultsAdapter).filter.filter(newText)
        return false
    }


    private fun handleBookDownloadProgress(it: BooksDownloadProgress) {
        val booksLeft = it.booksInQueue - it.successLoads - it.loadErrors
        if (booksLeft > 0) {
            downloadBadgeDrawable?.isVisible = true
            downloadBadgeDrawable?.number = booksLeft
        } else {
            downloadBadgeDrawable?.isVisible = false
        }
    }

    override fun itemInserted(item: FoundEntity) {
        while (binding.resultsList.isComputingLayout) {
            Thread.sleep(200)
        }
        requireActivity().runOnUiThread {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                (activity as AppCompatActivity?)?.supportActionBar?.subtitle =
                    URLDecoder.decode(OpdsStatement.getCurrentRequest(), Charset.defaultCharset())
            } else {
                URLDecoder.decode(OpdsStatement.getCurrentRequest(), "UTF-8")
            }
            val resultsCount =
                (binding.resultsList.adapter as OpdsSearchResultsAdapter).addItem(item)
            showBadge(resultsCount)
        }
    }

    override fun itemFiltered(item: FoundEntity) {
        requireActivity().runOnUiThread {

            if (PreferencesHandler.showFilterStatistics) {
                backdropFilterFragment?.updateBlockedCount()
            }
            showBlockedBadge(OpdsStatement.getBlockedResultsSize())
        }
    }

    @com.google.android.material.badge.ExperimentalBadgeUtils
    override fun drawBadges() {
        while (activity == null) {
            Thread.sleep(200)
        }
        activity?.runOnUiThread {
            if (!isAdded) {
                return@runOnUiThread
            }
            blockedBadgeDrawable = BadgeDrawable.create(requireActivity())
            BadgeUtils.attachBadgeDrawable(blockedBadgeDrawable!!, binding.showBlockedStateBtn)
            blockedBadgeDrawable?.maxCharacterCount = 5
            blockedBadgeDrawable?.badgeGravity = BadgeDrawable.BOTTOM_END
            blockedBadgeDrawable?.verticalOffset = 40
            blockedBadgeDrawable?.horizontalOffset = 60

            if (OpdsStatement.getBlockedResultsSize() > 0) {
                blockedBadgeDrawable?.number = OpdsStatement.getBlockedResultsSize()
                blockedBadgeDrawable?.isVisible = true
            } else {
                blockedBadgeDrawable?.isVisible = false
            }

            badgeDrawable = BadgeDrawable.create(requireContext())
            BadgeUtils.attachBadgeDrawable(badgeDrawable!!, binding.sortBtn)
            badgeDrawable?.maxCharacterCount = 5
            badgeDrawable?.badgeGravity = BadgeDrawable.BOTTOM_END
            badgeDrawable?.verticalOffset = 40
            badgeDrawable?.horizontalOffset = 60

            if (OpdsStatement.results.size > 0) {
                badgeDrawable?.number = OpdsStatement.results.size
                badgeDrawable?.isVisible = true
            } else {
                badgeDrawable?.isVisible = false
            }

            downloadBadgeDrawable = BadgeDrawable.create(requireActivity())
            BadgeUtils.attachBadgeDrawable(downloadBadgeDrawable!!, binding.downloadStateBtn)
            downloadBadgeDrawable?.maxCharacterCount = 5
            downloadBadgeDrawable?.badgeGravity = BadgeDrawable.BOTTOM_END
            downloadBadgeDrawable?.verticalOffset = 40
            downloadBadgeDrawable?.horizontalOffset = 60
            downloadBadgeDrawable?.isVisible = false
        }
    }

    override fun hasConnectionError(request: RequestItem, response: WebResponse) {
        activity?.runOnUiThread {
            val dialog = ConnectionErrorDialog()
            ConnectionErrorDialog.response = response
            ConnectionErrorDialog.request = request
            ConnectionErrorDialog.callback = {
                activity?.runOnUiThread {
                    Log.d("surprise", "OpdsFragment: 2069 retry request")
                    newRequestLaunched(ConnectionErrorDialog.request!!)
                    viewModel.request(ConnectionErrorDialog.request)
                    Log.d("surprise", "OpdsFragment: 2073 request relaunched")
                }
            }
            dialog.showNow(requireActivity().supportFragmentManager, ConnectionErrorDialog.TAG)
        }
    }

    override fun hasNoResults() {
        activity?.runOnUiThread {
            binding.noSearchResultsFoundTitle.visibility = View.VISIBLE
        }
    }

    override fun resultsFound() {
        activity?.runOnUiThread {
            binding.massLoadFab.visibility = View.VISIBLE
        }
    }


    private fun updateDownloadBadge() {
        val currentProgress = DownloadHandler.liveBookDownloadProgress.value
        if (currentProgress != null) {
            handleBookDownloadProgress(currentProgress)
        } else {
            downloadBadgeDrawable?.isVisible = false
        }
    }

    private fun showBadge(resultsCount: Int) {
        if (resultsCount > 0) {
            badgeDrawable?.isVisible = true
            badgeDrawable?.number = resultsCount
        } else {
            badgeDrawable?.isVisible = false
        }
    }

    private fun showBlockedBadge(count: Int) {
        if (count > 0) {
            blockedBadgeDrawable?.isVisible = true
            blockedBadgeDrawable?.number = count
        } else {
            blockedBadgeDrawable?.isVisible = false
        }
    }

    override fun checkBookAvailability(item: DownloadLink, callback: (String) -> Unit) {
        viewModel.checkFormatAvailability(requireContext(), item, callback)
    }

    override fun addToDownloadQueue(item: DownloadLink) {
        viewModel.addToDownloadQueue(item)
    }
}