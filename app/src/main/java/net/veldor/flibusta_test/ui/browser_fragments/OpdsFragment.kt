package net.veldor.flibusta_test.ui.browser_fragments

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.SearchAutoComplete
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.internal.ViewUtils.dpToPx
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentOpdsBinding
import net.veldor.flibusta_test.model.adapter.BookmarkDirAdapter
import net.veldor.flibusta_test.model.adapter.NewFoundItemAdapter
import net.veldor.flibusta_test.model.components.HamburgerButton
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.delegate.FoundItemActionDelegate
import net.veldor.flibusta_test.model.delegate.OpdsObserverDelegate
import net.veldor.flibusta_test.model.handler.*
import net.veldor.flibusta_test.model.handler.PreferencesHandler.Companion.NIGHT_THEME_DAY
import net.veldor.flibusta_test.model.handler.PreferencesHandler.Companion.NIGHT_THEME_NIGHT
import net.veldor.flibusta_test.model.helper.UrlHelper
import net.veldor.flibusta_test.model.interfaces.MyAdapterInterface
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_AUTHOR
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_BOOK
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_GENRE
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_SEQUENCE
import net.veldor.flibusta_test.model.selections.*
import net.veldor.flibusta_test.model.selections.opds.FoundEntity
import net.veldor.flibusta_test.model.view_model.OpdsViewModel
import net.veldor.flibusta_test.ui.BrowserActivity
import net.veldor.flibusta_test.ui.DownloadBookSetupActivity
import net.veldor.flibusta_test.ui.download_schedule_fragments.DownloadScheduleStatementFragment
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent.setEventListener
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import java.net.URLEncoder
import java.util.*


class OpdsFragment : Fragment(),
    FoundItemActionDelegate,
    OpdsObserverDelegate,
    SearchView.OnQueryTextListener {

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
    private lateinit var viewModel: OpdsViewModel
    private lateinit var errorSnackbar: Snackbar
    private var mConfirmExit: Long = 0
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
        viewModel = ViewModelProvider(this).get(OpdsViewModel::class.java)
        binding = FragmentOpdsBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        setupUI()
        setupObservers()
        restoreValues(savedInstanceState)
        viewModel.drawBadges(this)
        (binding.resultsList.adapter as NewFoundItemAdapter?)?.scrollToPressed()

        return binding.root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_bookmark -> {
                handleBookmark()
            }
            R.id.action_search -> {
                binding.bookSearchView.visibility = View.VISIBLE
                binding.bookSearchView.isIconified = false
                binding.bookSearchView.requestFocus()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleBookmark() {
        if (BookmarkHandler.instance.bookmarkInList(viewModel.getBookmarkLink())) {
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
            val layout = layoutInflater.inflate(R.layout.dialog_catalog_bookmark, null, false)
            val linkValueView = layout.findViewById<TextInputEditText>(R.id.linkValue)
            val bookmarkNameTextView =
                layout.findViewById<TextInputEditText>(R.id.bookmarkName)
            bookmarkNameTextView.setText(bookmarkReservedName)
            linkValueView.setText(viewModel.getBookmarkLink())
            val spinner = layout.findViewById<Spinner>(R.id.bookmarkFoldersSpinner)
            spinner.adapter = BookmarkDirAdapter(
                requireActivity(),
                BookmarkHandler.instance.getBookmarkCategories(requireContext())
            )
            AlertDialog.Builder(requireActivity(), R.style.dialogTheme)
                .setTitle(getString(R.string.add_bookmark_title))
                .setView(layout)
                .setPositiveButton(getString(R.string.add_title)) { _, _ ->
                    val categoryTextView =
                        layout.findViewById<TextInputEditText>(R.id.addNewFolderText)
                    val category: BookmarkItem = if (categoryTextView.text?.isNotEmpty() == true) {
                        BookmarkHandler.instance.addCategory(categoryTextView.text.toString())
                    } else {
                        spinner.selectedItem as BookmarkItem
                    }
                    viewModel.addBookmark(
                        category,
                        bookmarkNameTextView.text.toString(),
                        linkValueView.text.toString()
                    )
                    binding.addBookmarkBtn.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            resources,
                            R.drawable.ic_baseline_bookmark_border_24,
                            requireActivity().theme
                        )
                    )
                    activity?.invalidateOptionsMenu()
                    if (category.id.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            String.format(
                                Locale.ENGLISH,
                                getString(R.string.add_bookmark_template),
                                bookmarkNameTextView.text.toString()
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            String.format(
                                Locale.ENGLISH,
                                getString(R.string.add_bookmark_with_category_template),
                                bookmarkNameTextView.text.toString(),
                                category.name
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .show()
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
            (binding.resultsList.adapter as MyAdapterInterface).containsGenres() -> {
                sortType = R.id.searchGenre
                sortOptions = SortHandler().getDefaultSortOptions(requireContext())
                selectedOption = SelectedSortTypeHandler.instance.getGenreSortOptionIndex()
            }
            (binding.resultsList.adapter as MyAdapterInterface).containsSequences() -> {
                sortType = R.id.searchSequence
                sortOptions = SortHandler().getDefaultSortOptions(requireContext())
                selectedOption = SelectedSortTypeHandler.instance.getSequenceSortOptionIndex()
            }
            (binding.resultsList.adapter as MyAdapterInterface).containsAuthors() -> {
                sortType = R.id.searchAuthor
                sortOptions = SortHandler().getAuthorSortOptions(requireContext())
                selectedOption = SelectedSortTypeHandler.instance.getAuthorSortOptionIndex()
            }
            else -> {
                sortType = R.id.searchBook
                sortOptions = SortHandler().getBookSortOptions(requireContext())
                selectedOption = SelectedSortTypeHandler.instance.getBookSortOptionIndex()
            }
        }
        val searchArray: Array<CharSequence> = Array(sortOptions.size) { index ->
            sortOptions[index]!!.name
        }

        val builder = AlertDialog.Builder(requireActivity(), R.style.dialogTheme)
            .setTitle(getString(R.string.sort_list_by_title))
            .setSingleChoiceItems(searchArray, selectedOption) { dialog, selected ->
                dialog.dismiss()
                SelectedSortTypeHandler.instance.saveSortType(
                    sortType,
                    selected
                )
                (binding.resultsList.adapter as MyAdapterInterface).sort()
            }
        builder.show()
    }

    private fun showDownloadState() {
        backdropDownloadStateFragment?.binding?.pullUpView?.visibility = View.VISIBLE
        backdropDownloadStateFragment?.binding?.root?.background =
            ResourcesCompat.getDrawable(resources, R.drawable.rounded_corner, null)
        bottomSheetOpdsBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
    }


    override fun onResume() {
        super.onResume()
        OpdsStatement.instance.delegate = this
        activity?.invalidateOptionsMenu()
        configureBackdrop()
    }

    override fun onPause() {
        super.onPause()
        binding.bookSearchView.clearFocus()
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
                if (dy > 0) binding.massLoadFab.hide() else if (dy < 0 && OpdsStatement.instance.requestState.value != OpdsStatement.STATE_LOADING) binding.massLoadFab.show()
            }
        })

        // буду отслеживать состояние режима
        OpdsStatement.instance.requestState.observe(viewLifecycleOwner) {
            if (it == OpdsStatement.STATE_LOADING) {
                binding.fab.visibility = View.VISIBLE
                binding.swipeLayout.isRefreshing = true
            } else if (it == OpdsStatement.STATE_CANCELLED) {
                binding.fab.visibility = View.GONE
                binding.swipeLayout.isRefreshing = false
                binding.swipeLayout.isEnabled = OpdsStatement.instance.isNextPageLink()
            } else if (it == OpdsStatement.STATE_READY) {
                // если выбрана загрузка всех результатов- загружу следующую страницу
                if (PreferencesHandler.instance.opdsPagingType) {
                    binding.fab.visibility = View.GONE
                    binding.swipeLayout.isRefreshing = false
                    binding.swipeLayout.isEnabled = OpdsStatement.instance.isNextPageLink()
                } else {
                    if (OpdsStatement.instance.isNextPageLink()) {
                        mLastRequest = RequestItem(
                            OpdsStatement.instance.getNextPageLink()!!,
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
                        binding.swipeLayout.isEnabled = OpdsStatement.instance.isNextPageLink()
                    }
                }
            } else if (it == OpdsStatement.STATE_ERROR) {
                Log.d("surprise", "setupObservers: have error!")
                showErrorSnackbar()
            }
        }

        DownloadHandler.instance.liveBookDownloadProgress.observe(viewLifecycleOwner) {
            handleBookDownloadProgress(it)
        }

        OpdsResultsHandler.instance.livePossibleMemoryOverflow.observe(viewLifecycleOwner) {
            if (it) {
                showDisableHistoryDialog()
            }
        }

        DatabaseInstance.instance.mDatabase.downloadedBooksDao().lastDownloadedBookLive?.observe(
            viewLifecycleOwner
        ) {
            (binding.resultsList.adapter as MyAdapterInterface?)?.markAsDownloaded(it)
        }

        setEventListener(
            requireActivity(),
            object : KeyboardVisibilityEventListener {
                override fun onVisibilityChanged(isOpen: Boolean) {
                    if (!isOpen) {
                        binding.bookSearchView.clearFocus()
                    }
                }
            })
        binding.swipeLayout.setOnRefreshListener {
            if (OpdsStatement.instance.isNextPageLink()) {
                mLastRequest =
                    RequestItem(
                        OpdsStatement.instance.getNextPageLink()!!,
                        append = true,
                        addToHistory = false
                    )
                newRequestLaunched(mLastRequest!!)
                viewModel.request(
                    mLastRequest
                )
                binding.swipeLayout.performHapticFeedback(
                    HapticFeedbackConstants.KEYBOARD_TAP,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            } else {
                binding.swipeLayout.isRefreshing = false
            }
        }
    }

    private fun showDisableHistoryDialog() {
        if (mDisableHistoryDialog == null) {
            mDisableHistoryDialog = AlertDialog.Builder(requireContext(), R.style.dialogTheme)
                .setTitle(getString(R.string.disable_catalog_history_title))
                .setMessage(getString(R.string.disable_catalog_history_message))
                .setPositiveButton(getString(R.string.disable_message)) { _, _ ->
                    PreferencesHandler.instance.disableHistoryMessageViewed = true
                    PreferencesHandler.instance.saveOpdsHistory = false
                }.setNegativeButton(getString(R.string.keep_message)) { _, _ ->
                    PreferencesHandler.instance.disableHistoryMessageViewed = true
                }
                .create()
            mDisableHistoryDialog?.show()
        }
    }

    private fun showErrorSnackbar() {
        if (!this::errorSnackbar.isInitialized) {
            errorSnackbar = Snackbar.make(
                binding.root,
                getString(R.string.connection_error_message),
                Snackbar.LENGTH_INDEFINITE
            )
            errorSnackbar.setAction(getString(R.string.retry_request_title)) {
                if (mLastRequest != null) {
                    newRequestLaunched(mLastRequest!!)
                    viewModel.request(mLastRequest!!)
                } else {
                    Log.d("surprise", "showErrorSnackbar: no request")
                }
            }
            if (PreferencesHandler.instance.isEInk) {
                errorSnackbar.setBackgroundTint(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.always_white,
                        requireActivity().theme
                    )
                )
                errorSnackbar.setActionTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.black,
                        requireActivity().theme
                    )
                )
            } else {
                errorSnackbar.setActionTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.white,
                        requireActivity().theme
                    )
                )
            }
            errorSnackbar.setActionTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    requireActivity().theme
                )
            )
        }
        errorSnackbar.show()
    }

    @SuppressLint("RestrictedApi")
    private fun setupUI() {
        if (PreferencesHandler.instance.isEInk) {
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
                    R.color.always_white,
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
                    R.color.always_white,
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
                        R.color.light_gray,
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

        val closeBtn = searchLayout.findViewById(R.id.search_close_btn) as ImageView?
        closeBtn?.isEnabled = false
        closeBtn?.setImageDrawable(null)
        val buttonStyle: Int = R.attr.buttonBarButtonStyle
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
                                    TYPE_BOOK
                                }
                                R.id.searchAuthor -> {
                                    TYPE_AUTHOR
                                }
                                R.id.searchGenre -> {
                                    TYPE_GENRE
                                }
                                else -> TYPE_SEQUENCE
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
                (activity as BrowserActivity).binding.includedToolbar.appBarLayout.visibility =
                    View.GONE
                (activity as BrowserActivity).binding.includedBnv.bottomNavView.visibility =
                    View.GONE
                binding.searchOptionsContainer.visibility = View.VISIBLE
                binding.swipeLayout.isEnabled = false
                showInputMethod(view.findFocus())
            } else {
                (activity as BrowserActivity).binding.includedToolbar.appBarLayout.visibility =
                    View.VISIBLE
                (activity as BrowserActivity).binding.includedBnv.bottomNavView.visibility =
                    View.VISIBLE
                binding.bookSearchView.visibility = View.GONE
                binding.searchOptionsContainer.visibility = View.GONE
                binding.swipeLayout.isEnabled = true
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
            PreferencesHandler.instance.isOpdsUseFilter = isChecked
        }
        binding.useFiltersSwitch.isChecked = PreferencesHandler.instance.isOpdsUseFilter
        binding.showBlockedStateBtn.isVisible = PreferencesHandler.instance.isOpdsUseFilter

        binding.fab.setOnClickListener {
            viewModel.cancelSearch()
            binding.fab.hide()
            binding.swipeLayout.isRefreshing = false
            binding.swipeLayout.isEnabled = OpdsStatement.instance.isNextPageLink()
        }

        binding.massLoadFab.setOnClickListener {
            // transfer book list to backdrop fragment
            backdropFragment?.loadBooksList((binding.resultsList.adapter as MyAdapterInterface).getList())
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
                PreferencesHandler.instance.opdsPagingType = true
            } else {
                if (!PreferencesHandler.instance.coversMessageViewed) {
                    showCoversNotificationDialog()
                }
                binding.resultsPagingSwitcher.text = getString(R.string.load_all_results_title)
                PreferencesHandler.instance.opdsPagingType = false
            }
        }
        binding.resultsPagingSwitcher.isChecked =
            PreferencesHandler.instance.opdsPagingType

        binding.doOpdsSearchBtn.setOnClickListener {
            binding.bookSearchView.setQuery(binding.bookSearchView.query, true)
        }
        val a =
            NewFoundItemAdapter(OpdsStatement.instance.results, this, requireActivity())
        // recycler setup
        a.setHasStableIds(true)
        // load results if exists
        binding.resultsList.adapter = a
        Log.d("surprise", "setupUI: pressed item is ${OpdsStatement.instance.getPressedItemId()}")
        a.setPressedId(OpdsStatement.instance.getPressedItemId())
        val rowsCount = PreferencesHandler.instance.opdsLayoutRowsCount
        if (rowsCount == 0) {
            binding.resultsList.layoutManager = LinearLayoutManager(requireActivity())
        } else {
            binding.resultsList.layoutManager =
                GridLayoutManager(requireActivity(), rowsCount + 1)
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
            if (BookmarkHandler.instance.bookmarkInList(viewModel.getBookmarkLink())) {
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
            if (PreferencesHandler.instance.showFilterStatistics) {
                backdropFilterFragment?.updateList()
                bottomSheetFilterBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            } else {
                Toast.makeText(
                    requireContext(),
                    String.format(
                        Locale.ENGLISH,
                        getString(R.string.filter_statistics_disabled_pattern),
                        OpdsStatement.instance.getBlockedResultsSize()
                    ),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }

        binding.readerModeSwitcher.setOnClickListener {
            PreferencesHandler.instance.isEInk = !PreferencesHandler.instance.isEInk
            requireActivity().recreate()
        }

        binding.sortBtn.setOnClickListener {
            showSortDialog()
        }

        binding.downloadStateBtn.setOnClickListener {
            showDownloadState()
        }

        binding.nightModeSwitcher.setOnClickListener {
            when (PreferencesHandler.instance.nightMode) {
                NIGHT_THEME_DAY -> {
                    PreferencesHandler.instance.nightMode = NIGHT_THEME_NIGHT
                    AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_YES
                    )
                }
                NIGHT_THEME_NIGHT -> {
                    PreferencesHandler.instance.nightMode = NIGHT_THEME_DAY
                    AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_NO
                    )
                }
                else -> {
                    PreferencesHandler.instance.nightMode = NIGHT_THEME_NIGHT
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
            binding.filterListView.visibility = View.VISIBLE
            binding.filterByType.visibility = View.VISIBLE
            binding.quickSettingsPanel.visibility = View.GONE
            binding.filterListView.isIconified = false
            binding.filterListView.requestFocus()
            (binding.resultsList.adapter as MyAdapterInterface).setFilterEnabled(true)
        }

        binding.filterListView.setOnCloseListener {
            (binding.resultsList.adapter as MyAdapterInterface).setFilterEnabled(false)
            binding.filterListView.visibility = View.GONE
            binding.filterByType.visibility = View.GONE
            binding.quickSettingsPanel.visibility = View.VISIBLE
            return@setOnCloseListener true
        }

        binding.filterByType.setOnCheckedChangeListener { _, selected ->
            binding.filterListView.setQuery("", false)
            (binding.resultsList.adapter as MyAdapterInterface).setFilterSelection(selected)
        }

        binding.filterListView.setOnQueryTextListener(this)

        // запрещу обновление через swipeLayout по умолчанию
        binding.swipeLayout.isEnabled = false

        // toolbar options
        binding.useFilterBtn.isVisible = PreferencesHandler.instance.toolbarSearchShown
        binding.sortBtn.isVisible = PreferencesHandler.instance.toolbarSortShown
        binding.showBlockedStateBtn.isVisible = PreferencesHandler.instance.toolbarBlockedShown
        binding.downloadStateBtn.isVisible = PreferencesHandler.instance.toolbarDloadStateShown
        binding.addBookmarkBtn.isVisible = PreferencesHandler.instance.toolbarBookmarkShown
        binding.switchResultsLayoutBtn.isVisible =
            PreferencesHandler.instance.toolbarViewConfigShown
        binding.nightModeSwitcher.isVisible = PreferencesHandler.instance.toolbarThemeShown
        binding.readerModeSwitcher.isVisible = PreferencesHandler.instance.toolbarEinkShown
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showResultsViewCustomizeDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_switch_layout_grid, null)

        // switch covers
        val showCoversSwitch = view.findViewById<CheckBox>(R.id.showCoversCheckbox)
        showCoversSwitch?.isChecked = PreferencesHandler.instance.showCovers
        showCoversSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.instance.showCovers = state
            (binding.resultsList.adapter as NewFoundItemAdapter?)?.notifyDataSetChanged()
        }
        // switch show authors
        val showAuthorsSwitch = view.findViewById<CheckBox>(R.id.showAuthorsCheckbox)
        showAuthorsSwitch?.isChecked = PreferencesHandler.instance.showAuthors
        showAuthorsSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.instance.showAuthors = state
            (binding.resultsList.adapter as NewFoundItemAdapter?)?.notifyDataSetChanged()
        }
        // switch show translators
        val showTranslatorsSwitch = view.findViewById<CheckBox>(R.id.showTranslatorsCheckbox)
        showTranslatorsSwitch?.isChecked = PreferencesHandler.instance.showFoundBookTranslators
        showTranslatorsSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.instance.showFoundBookTranslators = state
            (binding.resultsList.adapter as NewFoundItemAdapter?)?.notifyDataSetChanged()
        }
        // switch show sequences
        val showSequencesSwitch = view.findViewById<CheckBox>(R.id.showSequencesCheckbox)
        showSequencesSwitch?.isChecked = PreferencesHandler.instance.showFoundBookSequences
        showSequencesSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.instance.showFoundBookSequences = state
            (binding.resultsList.adapter as NewFoundItemAdapter?)?.notifyDataSetChanged()
        }
        // switch show genres
        val showGenresSwitch = view.findViewById<CheckBox>(R.id.showGenresCheckbox)
        showGenresSwitch?.isChecked = PreferencesHandler.instance.showFoundBookGenres
        showGenresSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.instance.showFoundBookGenres = state
            (binding.resultsList.adapter as NewFoundItemAdapter?)?.notifyDataSetChanged()
        }
        // switch show format
        val showFormatSwitch = view.findViewById<CheckBox>(R.id.showFormatCheckbox)
        showFormatSwitch?.isChecked = PreferencesHandler.instance.showFoundBookFormat
        showFormatSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.instance.showFoundBookFormat = state
            (binding.resultsList.adapter as NewFoundItemAdapter?)?.notifyDataSetChanged()
        }
        // switch show downloads
        val showDownloadsSwitch = view.findViewById<CheckBox>(R.id.showDownloadsCheckbox)
        showDownloadsSwitch?.isChecked = PreferencesHandler.instance.showFoundBookDownloads
        showDownloadsSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.instance.showFoundBookDownloads = state
            (binding.resultsList.adapter as NewFoundItemAdapter?)?.notifyDataSetChanged()
        }
        // switch show size
        val showSizeSwitch = view.findViewById<CheckBox>(R.id.showSizeCheckbox)
        showSizeSwitch?.isChecked = PreferencesHandler.instance.showFoundBookSize
        showSizeSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.instance.showFoundBookSize = state
            (binding.resultsList.adapter as NewFoundItemAdapter?)?.notifyDataSetChanged()
        }
        // switch show size
        val showFormatsSwitch = view.findViewById<CheckBox>(R.id.showAvailableFormatsCheckbox)
        showFormatsSwitch?.isChecked = PreferencesHandler.instance.showFoundBookAvailableFormats
        showFormatsSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.instance.showFoundBookAvailableFormats = state
            (binding.resultsList.adapter as NewFoundItemAdapter?)?.notifyDataSetChanged()
        }
        // switch show read
        val showReadSwitch = view.findViewById<CheckBox>(R.id.showReadCheckbox)
        showReadSwitch?.isChecked = PreferencesHandler.instance.showFoundBookReadBtn
        showReadSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.instance.showFoundBookReadBtn = state
            (binding.resultsList.adapter as NewFoundItemAdapter?)?.notifyDataSetChanged()
        }
        // switch show downloaded
        val showDownloadSwitch = view.findViewById<CheckBox>(R.id.showDownloadCheckbox)
        showDownloadSwitch?.isChecked = PreferencesHandler.instance.showFoundBookDownloadBtn
        showDownloadSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.instance.showFoundBookDownloadBtn = state
            (binding.resultsList.adapter as NewFoundItemAdapter?)?.notifyDataSetChanged()
        }
        // switch show downloaded
        val showElementDescriptionSwitch = view.findViewById<CheckBox>(R.id.showElementDescription)
        showElementDescriptionSwitch?.isChecked = PreferencesHandler.instance.showElementDescription
        showElementDescriptionSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.instance.showElementDescription = state
            (binding.resultsList.adapter as NewFoundItemAdapter?)?.notifyDataSetChanged()
        }

        // switch show element btn
        val showElementBtnSwitch = view.findViewById<CheckBox>(R.id.showElementBtn)
        showElementBtnSwitch?.isChecked = !PreferencesHandler.instance.hideOpdsResultsButtons
        showElementBtnSwitch?.setOnCheckedChangeListener { _, state ->
            PreferencesHandler.instance.hideOpdsResultsButtons = !state
            (binding.resultsList.adapter as NewFoundItemAdapter?)?.notifyDataSetChanged()
        }

        val slider = view.findViewById<Slider>(R.id.seekBar)

        slider.value =
            (PreferencesHandler.instance.opdsLayoutRowsCount + 1).toFloat()

        slider?.setLabelFormatter { value: Float ->
            val longValue = value.toInt() - 1
            PreferencesHandler.instance.opdsLayoutRowsCount = longValue
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
                PreferencesHandler.instance.coversMessageViewed = true
                PreferencesHandler.instance.showCovers = true
            }.setNegativeButton(getString(R.string.hide_covers_message)) { _, _ ->
                PreferencesHandler.instance.coversMessageViewed = true
                PreferencesHandler.instance.showCovers = false
            }.setNeutralButton(getString(R.string.load_covers_by_request_message)) { _, _ ->
                PreferencesHandler.instance.coversMessageViewed = true
                PreferencesHandler.instance.showCoversByRequest = true
            }
            .show()
    }

    private fun setAutocompleteAdapter(): ArrayAdapter<String> {
        val autocomplete = viewModel.getAutocomplete(
            when (binding.searchType.checkedRadioButtonId) {
                R.id.searchBook -> TYPE_BOOK
                R.id.searchAuthor -> TYPE_AUTHOR
                R.id.searchGenre -> TYPE_GENRE
                else -> TYPE_SEQUENCE
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.odps_menu, menu)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (this::binding.isInitialized) {
            outState.putString(STATE_SEARCH_VALUE, binding.bookSearchView.query.toString())
        }
    }

    override fun buttonPressed(item: FoundEntity) {
        if (item.type != TYPE_BOOK) {
            OpdsStatement.instance.setPressedItem(item)
        }
        when (item.type) {
            TYPE_AUTHOR -> {
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
                    showAuthorViewSelect(item, null)
                }
            }
            TYPE_BOOK -> {
                if (PreferencesHandler.instance.skipDownloadSetup && PreferencesHandler.instance.rememberFavoriteFormat && PreferencesHandler.instance.favoriteFormat != null) {
                    viewModel.addToDownloadQueue(item.getFavoriteLink())
                } else {
                    // show window for book download prepare
                    val goDownloadIntent =
                        Intent(requireContext(), DownloadBookSetupActivity::class.java)
                    goDownloadIntent.putExtra("EXTRA_BOOK", item)
                    startActivity(goDownloadIntent)
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
        OpdsStatement.instance.setPressedItem(item)
        bookmarkReservedName = item.name
        when (item.type) {
            TYPE_AUTHOR -> {
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
                    showAuthorViewSelect(item, null)
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
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )

        view.setOnCreateContextMenuListener { menu, _, _ ->
            var menuItem: MenuItem =
                menu.add(getString(R.string.pull_to_blacklist_msg))
            menuItem.setOnMenuItemClickListener {
                viewModel.addBlacklistItem(item, target)
                if (PreferencesHandler.instance.isOpdsUseFilter) {
                    (binding.resultsList.adapter as NewFoundItemAdapter).hide(item)
                    OpdsStatement.instance.addFilteredResult(item)
                }
                Toast.makeText(
                    requireContext(),
                    getString(R.string.added_to_filter_list_title),
                    Toast.LENGTH_SHORT
                ).show()
                true
            }
            menuItem =
                menu.add(getString(R.string.add_to_subscribes_msg))
            menuItem.setOnMenuItemClickListener {
                viewModel.addSubscribeItem(item, target)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.added_to_subscribes_list_title),
                    Toast.LENGTH_SHORT
                ).show()
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
        button.setOnCreateContextMenuListener { menu, _, _ ->
            var menuItem: MenuItem =
                menu.add(getString(R.string.download_message))
            menuItem.setOnMenuItemClickListener {
                val goDownloadIntent =
                    Intent(requireContext(), DownloadBookSetupActivity::class.java)
                goDownloadIntent.putExtra("EXTRA_BOOK", item)
                startActivity(goDownloadIntent)
                true
            }
            if (!item.read) {
                menuItem =
                    menu.add(getString(R.string.mark_as_read_title))
                menuItem.setOnMenuItemClickListener {
                    viewModel.markRead(item)
                    (binding.resultsList.adapter as MyAdapterInterface).markBookRead(item)
                    true
                }
            } else {
                menuItem =
                    menu.add(getString(R.string.mark_as_unread_title))
                menuItem.setOnMenuItemClickListener {
                    viewModel.markUnread(item)
                    (binding.resultsList.adapter as MyAdapterInterface).markBookUnread(item)
                    true
                }
            }
        }
    }

    override fun loadMoreBtnClicked() {
        if (OpdsStatement.instance.isNextPageLink()) {
            mLastRequest = RequestItem(
                OpdsStatement.instance.getNextPageLink()!!,
                append = true,
                addToHistory = false
            )
            newRequestLaunched(mLastRequest!!)
            viewModel.request(
                mLastRequest
            )
            binding.root.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }

    private fun newRequestLaunched(request: RequestItem) {
        // disable filter
        if (!request.append) {
            // промотаю страницу до верха
            // проверю закладки
            if (BookmarkHandler.instance.bookmarkInList(request.request)) {
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

            (binding.resultsList.adapter as NewFoundItemAdapter?)?.clearList()
            showBadge(0)
        }
        binding.filterListView.visibility = View.GONE
        binding.filterListView.setQuery("", false)
        binding.filterByType.visibility = View.GONE
        binding.quickSettingsPanel.visibility = View.VISIBLE

        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        binding.fab.show()
        binding.massLoadFab.hide()
        (binding.resultsList.adapter as MyAdapterInterface).setLoadInProgress(true)
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
            OpdsStatement.instance.setPressedItem(item)
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
            OpdsStatement.instance.setPressedItem(item)
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
            OpdsStatement.instance.setPressedItem(item)
            PreferencesHandler.instance.lastWebViewLink = item.link!!
            (requireActivity() as BrowserActivity).launchWebViewFromOpds()
        }
    }

    override fun rightButtonPressed(item: FoundEntity) {
        if (!item.downloaded) {
            item.downloaded = !item.downloaded
            Toast.makeText(requireContext(), R.string.mark_as_downloaded_title, Toast.LENGTH_SHORT)
                .show()
            viewModel.markDownloaded(item)
            (binding.resultsList.adapter as MyAdapterInterface).markAsDownloaded(item)
            if (PreferencesHandler.instance.isOpdsUseFilter && PreferencesHandler.instance.isHideDownloaded) {
                OpdsStatement.instance.addFilteredResult(item)
            }
        } else {
            item.downloaded = !item.downloaded
            Toast.makeText(
                requireContext(),
                R.string.mark_as_no_downloaded_title,
                Toast.LENGTH_SHORT
            ).show()
            viewModel.markNoDownloaded(item)
            (binding.resultsList.adapter as MyAdapterInterface).markAsNoDownloaded(item)
        }
    }

    override fun leftButtonPressed(item: FoundEntity) {
        if (item.read) {
            item.read = !item.read
            Toast.makeText(requireContext(), R.string.mark_as_unread_title, Toast.LENGTH_SHORT)
                .show()
            viewModel.markUnread(item)
            (binding.resultsList.adapter as NewFoundItemAdapter).markBookUnread(item)
        } else {
            item.read = !item.read
            Toast.makeText(requireContext(), R.string.mark_as_read_title, Toast.LENGTH_SHORT).show()
            viewModel.markRead(item)
            (binding.resultsList.adapter as NewFoundItemAdapter).markBookRead(item)
            if (PreferencesHandler.instance.isOpdsUseFilter && PreferencesHandler.instance.isHideRead) {
                OpdsStatement.instance.addFilteredResult(item)
            }
        }
    }

    override fun scrollTo(indexOf: Int) {
        Log.d("surprise", "scrollTo: i scroll to $indexOf")
        binding.resultsList.scrollToPosition(indexOf)
    }

    private fun showAuthorViewSelect(author: FoundEntity, item: FoundEntity?) {
        // создам диалоговое окно
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.dialogTheme)
        dialogBuilder
            .setTitle(author.author)
            .setItems(mAuthorViewTypes) { _: DialogInterface?, which: Int ->
                OpdsStatement.instance.setPressedItem(item)
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
        val link = Regex("[^0-9]").replace(author.link!!, "")
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

    fun keyPressed(keyCode: Int): Boolean {
        if (PreferencesHandler.instance.isEInk) {
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
                (binding.resultsList.adapter as MyAdapterInterface).setLoadInProgress(false)
                binding.fab.hide()
                if ((binding.resultsList.adapter as MyAdapterInterface).containsBooks()) {
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
            if (!HistoryHandler.instance.isEmpty) {
                val lastResults = HistoryHandler.instance.lastPage
                (binding.resultsList.adapter as NewFoundItemAdapter?)?.clearList()
                OpdsStatement.instance.load(lastResults)
                (binding.resultsList.adapter as NewFoundItemAdapter?)?.setPressedId(lastResults?.pressedItemId)
                if (PreferencesHandler.instance.saveOpdsHistory) {
                    (binding.resultsList.adapter as NewFoundItemAdapter?)?.scrollToPressed()
                    showBlockedBadge(OpdsStatement.instance.getBlockedResultsSize())
                    updateDownloadBadge()
                    if (lastResults?.nextPageLink != null) {
                        binding.swipeLayout.isEnabled = true
                    }
                } else {
                    OpdsStatement.instance.prepareRequestFromHistory()
                    // load last request
                    mLastRequest = RequestItem(
                        OpdsStatement.instance.getCurrentRequest()!!,
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
            if (mConfirmExit != 0L) {
                if (mConfirmExit > System.currentTimeMillis() - 3000) {
                    // выйду из приложения
                    val startMain = Intent(Intent.ACTION_MAIN)
                    startMain.addCategory(Intent.CATEGORY_HOME)
                    startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(startMain)
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.press_back_again_for_exit_title),
                        Toast.LENGTH_SHORT
                    ).show()
                    mConfirmExit = System.currentTimeMillis()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.press_back_again_for_exit_title),
                    Toast.LENGTH_SHORT
                ).show()
                mConfirmExit = System.currentTimeMillis()
            }
        }
        return false
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
                R.id.searchBook -> TYPE_BOOK
                R.id.searchAuthor -> TYPE_AUTHOR
                R.id.searchGenre -> TYPE_GENRE
                else -> TYPE_SEQUENCE
            }
        )
    }

    private fun loadLink(link: String) {
        linkForLoad = link
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        (binding.resultsList.adapter as MyAdapterInterface).filter.filter(query)
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        (binding.resultsList.adapter as MyAdapterInterface).filter.filter(newText)
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
        requireActivity().runOnUiThread {
            val resultsCount = (binding.resultsList.adapter as NewFoundItemAdapter).addItem(item)
            showBadge(resultsCount)
        }
    }

    override fun itemFiltered(item: FoundEntity) {
        requireActivity().runOnUiThread {

            if (PreferencesHandler.instance.showFilterStatistics) {
                backdropFilterFragment?.updateBlockedCount()
            }
            showBlockedBadge(OpdsStatement.instance.getBlockedResultsSize())
        }
    }

    @com.google.android.material.badge.ExperimentalBadgeUtils
    override fun drawBadges() {
        activity?.runOnUiThread {
            // добавлю бейджи сразу тут
            blockedBadgeDrawable = BadgeDrawable.create(requireActivity())
            BadgeUtils.attachBadgeDrawable(blockedBadgeDrawable!!, binding.showBlockedStateBtn)
            blockedBadgeDrawable?.maxCharacterCount = 5
            blockedBadgeDrawable?.badgeGravity = BadgeDrawable.BOTTOM_END
            blockedBadgeDrawable?.verticalOffset = 40
            blockedBadgeDrawable?.horizontalOffset = 60

            if (OpdsStatement.instance.getBlockedResultsSize() > 0) {
                blockedBadgeDrawable?.number = OpdsStatement.instance.getBlockedResultsSize()
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

            if (OpdsStatement.instance.results.size > 0) {
                badgeDrawable?.number = OpdsStatement.instance.results.size
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


    private fun updateDownloadBadge() {
        val currentProgress = DownloadHandler.instance.liveBookDownloadProgress.value
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
}