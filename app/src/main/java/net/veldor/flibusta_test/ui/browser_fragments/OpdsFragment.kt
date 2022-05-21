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
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.SearchAutoComplete
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.internal.ViewUtils.dpToPx
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentOpdsBinding
import net.veldor.flibusta_test.model.adapter.BookmarkDirAdapter
import net.veldor.flibusta_test.model.adapter.FoundItemAdapter
import net.veldor.flibusta_test.model.adapter.FoundItemCompactAdapter
import net.veldor.flibusta_test.model.adapter.OpdsSortAdapter
import net.veldor.flibusta_test.model.components.HamburgerButton
import net.veldor.flibusta_test.model.components.SortShowSpinner
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.delegate.FoundItemActionDelegate
import net.veldor.flibusta_test.model.delegate.SearchResultActionDelegate
import net.veldor.flibusta_test.model.handler.*
import net.veldor.flibusta_test.model.helper.UrlHelper
import net.veldor.flibusta_test.model.interfaces.MyAdapterInterface
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_AUTHOR
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_BOOK
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_GENRE
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_SEQUENCE
import net.veldor.flibusta_test.model.selections.BookmarkItem
import net.veldor.flibusta_test.model.selections.HistoryItem
import net.veldor.flibusta_test.model.selections.RequestItem
import net.veldor.flibusta_test.model.selections.SortOption
import net.veldor.flibusta_test.model.selections.opds.FoundEntity
import net.veldor.flibusta_test.model.selections.opds.SearchResult
import net.veldor.flibusta_test.model.view_model.OpdsViewModel
import net.veldor.flibusta_test.ui.BrowserActivity
import net.veldor.flibusta_test.ui.DownloadBookSetupActivity
import net.veldor.flibusta_test.ui.FilterActivity
import net.veldor.flibusta_test.ui.download_schedule_fragments.DownloadScheduleStatementFragment
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent.setEventListener
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import java.net.URLEncoder
import java.util.*


class OpdsFragment : Fragment(),
    FoundItemActionDelegate,
    SearchResultActionDelegate, SearchView.OnQueryTextListener {

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
    private var lastScrolled: Int = -1
    private var mLastQuery: String? = null
    private lateinit var sortShowSpinner: SortShowSpinner
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
        activity?.invalidateOptionsMenu()
        viewModel = ViewModelProvider(this).get(OpdsViewModel::class.java)
        viewModel.searchResultsDelegate = this
        binding = FragmentOpdsBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        setupUI()
        setupObservers()
        restoreValues(savedInstanceState)
        return binding.root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_show_download_state -> {
                // show download state fragment
                showDownloadState()
            }
            R.id.action_show_sort -> {
                showSortDialog()
            }
            R.id.action_add_bookmark -> {
                if (BookmarkHandler.instance.bookmarkInList(viewModel.getBookmarkLink())) {
                    viewModel.removeBookmark()
                    Toast.makeText(requireActivity(), "Bookmark removed", Toast.LENGTH_SHORT).show()
                    activity?.invalidateOptionsMenu()
                } else {
                    showAddBookmarkDialog()
                }
            }
            R.id.action_search -> {
                binding.bookSearchView.visibility = View.VISIBLE
                binding.bookSearchView.isIconified = false
                binding.bookSearchView.requestFocus()
            }
            R.id.action_show_filter -> {
                bottomSheetFilterBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
        return super.onOptionsItemSelected(item)
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
            AlertDialog.Builder(requireActivity())
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

        val builder = AlertDialog.Builder(requireActivity())
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
        activity?.invalidateOptionsMenu()
        // check adapter change
        if (PreferencesHandler.instance.isLightOpdsAdapter && binding.resultsList.adapter is FoundItemAdapter) {
            val newAdapter = FoundItemCompactAdapter(
                (binding.resultsList.adapter as FoundItemAdapter).getList(),
                this,
                requireActivity()
            )
            binding.resultsList.adapter = newAdapter
        } else if (!PreferencesHandler.instance.isLightOpdsAdapter && binding.resultsList.adapter is FoundItemCompactAdapter) {
            val newAdapter = FoundItemAdapter(
                (binding.resultsList.adapter as FoundItemCompactAdapter).getList(),
                this,
                requireActivity()
            )
            binding.resultsList.adapter = newAdapter
        }
        configureBackdrop()
    }

    override fun onPause() {
        super.onPause()
        binding.bookSearchView.clearFocus()
    }


    fun configureBackdrop() {
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
        (binding.resultsList.adapter as MyAdapterInterface?)?.liveSize?.observe(viewLifecycleOwner) {
            binding.foundResultsQuantity.text = String.format(
                Locale.ENGLISH,
                getString(R.string.found_results_quantity_title),
                it
            )
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

        viewModel.liveRequestState.observe(viewLifecycleOwner) {
            when (it) {
                OpdsViewModel.STATUS_WAIT -> {
                    binding.requestProgressBar.visibility = View.GONE
                }
                OpdsViewModel.STATUS_REQUESTING -> {
                    binding.requestProgressBar.visibility = View.VISIBLE
                    binding.requestProgressBar.progress = 10
                }
                OpdsViewModel.STATUS_REQUESTED -> {
                    binding.requestProgressBar.visibility = View.VISIBLE
                    binding.requestProgressBar.progress = 50
                }
                OpdsViewModel.STATUS_PARSED -> {
                    binding.requestProgressBar.visibility = View.VISIBLE
                    binding.requestProgressBar.progress = 90
                }
                OpdsViewModel.STATUS_READY -> {
                    binding.requestProgressBar.visibility = View.GONE
                    binding.requestProgressBar.progress = 0
                }
                OpdsViewModel.STATUS_CANCELLED -> {
                    binding.requestProgressBar.visibility = View.GONE
                    binding.requestProgressBar.progress = 0
                }
                OpdsViewModel.STATUS_REQUEST_ERROR -> {
                    binding.requestProgressBar.visibility = View.GONE
                    binding.fab.hide()
                    showErrorSnackbar()
                }
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
                    newRequestLaunched()
                    viewModel.request(mLastRequest!!)
                } else {
                    Log.d("surprise", "showErrorSnackbar: no request")
                }
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
                    ResourcesCompat.getColor(resources, R.color.white, requireActivity().theme),
                    ResourcesCompat.getColor(resources, R.color.white, requireActivity().theme),
                    ResourcesCompat.getColor(resources, R.color.light_gray, requireActivity().theme),
                )
            )

            binding.searchBook.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    requireActivity().theme
                )
            )
            binding.searchBook.supportButtonTintList = myColorStateList
            binding.searchAuthor.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    requireActivity().theme
                )
            )
            binding.searchAuthor.supportButtonTintList = myColorStateList
            binding.searchGenre.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    requireActivity().theme
                )
            )
            binding.searchGenre.supportButtonTintList = myColorStateList
            binding.searchSequence.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    requireActivity().theme
                )
            )
            binding.searchSequence.supportButtonTintList = myColorStateList

            binding.showArrivalsBtn.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    requireActivity().theme
                )
            )

            binding.showEntitiesByAlphabetBtn.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    requireActivity().theme
                )
            )

            binding.resultsPagingSwitcher.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    requireActivity().theme
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.resultsPagingSwitcher.buttonTintList = myColorStateList
                binding.resultsPagingSwitcher.trackTintList = myColorStateList
            }

            binding.sortByTitle.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    requireActivity().theme
                )
            )

            binding.useFiltersSwitch.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    requireActivity().theme
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.useFiltersSwitch.trackTintList = myColorStateList
            }

            binding.showFilterPreferencesBtn.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    requireActivity().theme
                )
            )

            binding.doOpdsSearchBtn.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    requireActivity().theme
                )
            )
        }

        binding.bookSearchView.isSubmitButtonEnabled = false
        val searchLayout = binding.bookSearchView.getChildAt(0) as LinearLayout
        val buttonStyle: Int = R.attr.buttonBarButtonStyle
        val showAutofillBtn = ImageButton(requireActivity(), null, buttonStyle)

        val hamburgerDrawable = HamburgerButton(
            size = dpToPx(requireContext(), 24).toInt(),
            barThickness = dpToPx(requireContext(), 2),
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
        // try hide status bar
        //search bar
        if (mLastQuery != null) {
            binding.bookSearchView.setQuery(mLastQuery, false)
        }
        binding.bookSearchView.queryHint = getString(R.string.enter_request_title)
        binding.bookSearchView.setOnQueryTextFocusChangeListener { view, b ->
            Log.d("surprise", "setupUI: focus changed")
            if (b) {
                (activity as BrowserActivity).binding.includedToolbar.appBarLayout.visibility =
                    View.GONE
                (activity as BrowserActivity).binding.includedBnv.bottomNavView.visibility =
                    View.GONE
                binding.searchOptionsContainer.visibility = View.VISIBLE
                showInputMethod(view.findFocus())
            } else {
                (activity as BrowserActivity).binding.includedToolbar.appBarLayout.visibility =
                    View.VISIBLE
                (activity as BrowserActivity).binding.includedBnv.bottomNavView.visibility =
                    View.VISIBLE
                binding.bookSearchView.visibility = View.GONE
                binding.searchOptionsContainer.visibility = View.GONE
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
                newRequestLaunched()
                bookmarkReservedName = request
                mLastRequest = RequestItem(
                    UrlHelper.getSearchRequest(
                        binding.searchType.checkedRadioButtonId,
                        URLEncoder.encode(request, "utf-8").replace("+", "%20")
                    ),
                    append = false,
                    addToHistory = true,
                    clickedElementIndex = -1
                )
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
            if (isChecked) {
                binding.showFilterPreferencesBtn.visibility = View.VISIBLE
            } else {
                binding.showFilterPreferencesBtn.visibility = View.GONE
            }
        }
        binding.useFiltersSwitch.isChecked = PreferencesHandler.instance.isOpdsUseFilter

        binding.showFilterPreferencesBtn.setOnClickListener {
            startActivity(Intent(requireContext(), FilterActivity::class.java))
        }

        binding.fab.setOnClickListener {
            binding.foundResultsQuantity.visibility = View.VISIBLE
            binding.requestProgressBar.visibility = View.GONE
            viewModel.cancelSearch()
            binding.fab.hide()
            (binding.resultsList.adapter as MyAdapterInterface).setLoadInProgress(false)
        }

        binding.massLoadFab.setOnClickListener {
            // transfer book list to backdrop fragment
            backdropFragment?.loadBooksList((binding.resultsList.adapter as MyAdapterInterface).getList())
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        // handle entity list display actions
        binding.showEntitiesByAlphabetBtn.setOnClickListener {
            binding.bookSearchView.clearFocus()
            newRequestLaunched()

            when (binding.searchType.checkedRadioButtonId) {
                R.id.searchAuthor -> {
                    bookmarkReservedName = "Авторы"
                    mLastRequest = RequestItem(
                        "/opds/authorsindex",
                        append = false,
                        addToHistory = true,
                        clickedElementIndex = -1
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
                        addToHistory = true,
                        clickedElementIndex = -1
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
                        addToHistory = true,
                        clickedElementIndex = -1
                    )
                    viewModel.request(
                        mLastRequest
                    )
                }
            }
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
                    sortShowSpinner.setSortList(
                        SortHandler().getBookSortOptions(requireContext()),
                        SelectedSortTypeHandler.instance.getBookSortOptionIndex()
                    )
                }
                R.id.searchAuthor -> {
                    // change title of new arrivals
                    binding.showArrivalsBtn.text = getString(R.string.new_authors_title)
                    binding.showEntitiesByAlphabetBtn.visibility = View.VISIBLE
                    binding.showEntitiesByAlphabetBtn.text =
                        getString(R.string.show_authors_list_title)
                    sortShowSpinner.setSortList(
                        SortHandler().getAuthorSortOptions(requireContext()),
                        SelectedSortTypeHandler.instance.getAuthorSortOptionIndex()
                    )
                }
                R.id.searchGenre -> {
                    // change title of new arrivals
                    binding.showArrivalsBtn.text = getString(R.string.new_genres_title)
                    binding.showEntitiesByAlphabetBtn.visibility = View.VISIBLE
                    binding.showEntitiesByAlphabetBtn.text =
                        getString(R.string.show_genres_list_title)
                    sortShowSpinner.setSortList(
                        SortHandler().getDefaultSortOptions(requireContext()),
                        SelectedSortTypeHandler.instance.getGenreSortOptionIndex()
                    )
                }
                R.id.searchSequence -> {
                    // change title of new arrivals
                    binding.showArrivalsBtn.text = getString(R.string.new_sequences_title)
                    binding.showEntitiesByAlphabetBtn.visibility = View.VISIBLE
                    binding.showEntitiesByAlphabetBtn.text =
                        getString(R.string.show_sequences_list_title)
                    sortShowSpinner.setSortList(
                        SortHandler().getDefaultSortOptions(requireContext()),
                        SelectedSortTypeHandler.instance.getSequenceSortOptionIndex()
                    )
                }
            }
        }
        binding.showArrivalsBtn.setOnClickListener {
            binding.bookSearchView.clearFocus()
            newRequestLaunched()
            when (binding.searchType.checkedRadioButtonId) {
                R.id.searchBook -> {
                    bookmarkReservedName = "Новинки"
                    mLastRequest = RequestItem(
                        "/opds/new/0/new",
                        append = false,
                        addToHistory = true,
                        clickedElementIndex = -1
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
                        addToHistory = true,
                        clickedElementIndex = -1
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
                        addToHistory = true,
                        clickedElementIndex = -1
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
                        addToHistory = true,
                        clickedElementIndex = -1
                    )
                    viewModel.request(
                        mLastRequest
                    )
                }
            }
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
        handleSortOptions()

        binding.doOpdsSearchBtn.setOnClickListener {
            binding.bookSearchView.setQuery(binding.bookSearchView.query, true)
        }
        val a = if (PreferencesHandler.instance.isLightOpdsAdapter) {
            FoundItemCompactAdapter(arrayListOf(), this, requireActivity())
        } else {
            FoundItemAdapter(arrayListOf(), this, requireActivity())
        }
        // recycler setup
        a.setHasStableIds(true)
        // load results if exists
        loadPreviousResults(a, viewModel.getPreviousResults())
        binding.resultsList.adapter = a
        binding.resultsList.layoutManager = LinearLayoutManager(requireActivity())

        if (linkForLoad != null) {
            newRequestLaunched()
            mLastRequest = RequestItem(
                linkForLoad!!,
                append = false,
                addToHistory = true,
                clickedElementIndex = -1
            )
            viewModel.request(
                mLastRequest
            )
            linkForLoad = null
        }

        // add scroll listener for next results load to recycler
        binding.resultsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                // проверю последний видимый элемент
                val manager = binding.resultsList.layoutManager as LinearLayoutManager?
                if (manager != null) {
                    val adapter = binding.resultsList.adapter
                    if (adapter != null) {
                        val position = manager.findLastCompletelyVisibleItemPosition()
                        viewModel.saveScrolledPosition(position)
                        if (
                            !(binding.resultsList.adapter as MyAdapterInterface).filterEnabled() &&
                            !viewModel.loadInProgress() &&
                            position == adapter.itemCount - 1 &&
                            position > lastScrolled &&
                            PreferencesHandler.instance.opdsPagingType &&
                            !PreferencesHandler.instance.isDisplayPagerButton &&
                            viewModel.getNextPageLink() != null
                        ) {
                            newRequestLaunched()
                            mLastRequest = RequestItem(
                                viewModel.getNextPageLink()!!,
                                append = true,
                                addToHistory = false,
                                clickedElementIndex = -1
                            )
                            viewModel.request(
                                mLastRequest
                            )
                        }
                        lastScrolled = position
                    }
                }

            }
        })

        binding.filterListView.isSubmitButtonEnabled = true

        //setup filter list option
        binding.useFilterBtn.setOnClickListener {
            binding.filterListView.visibility = View.VISIBLE
            binding.filterByType.visibility = View.VISIBLE
            it.visibility = View.GONE
            binding.filterListView.isIconified = false
            binding.filterListView.requestFocus()
            (binding.resultsList.adapter as MyAdapterInterface).setFilterEnabled(true)
        }

        binding.filterListView.setOnCloseListener {
            lastScrolled = lastScrolled - 1
            (binding.resultsList.adapter as MyAdapterInterface).setFilterEnabled(false)
            binding.filterListView.visibility = View.GONE
            binding.filterByType.visibility = View.GONE
            binding.useFilterBtn.visibility = View.VISIBLE
            return@setOnCloseListener true
        }

        binding.filterByType.setOnCheckedChangeListener { _, selected ->
            binding.filterListView.setQuery("", false)
            (binding.resultsList.adapter as MyAdapterInterface).setFilterSelection(selected)
        }

        binding.filterListView.setOnQueryTextListener(this)
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

    private fun loadPreviousResults(
        a: MyAdapterInterface?,
        previousResults: ArrayList<SearchResult>?
    ) {
        if (previousResults != null && previousResults.isNotEmpty()) {
            binding.hintContainer.visibility = View.GONE
            viewModel.replacePreviousResults(previousResults)
            previousResults.forEach {
                a?.appendContent(it.results)
            }
            binding.foundResultsQuantity.visibility = View.VISIBLE
            if (PreferencesHandler.instance.opdsPagingType) {
                a?.setNextPageLink(previousResults.lastOrNull()?.nextPageLink)
            }
            val clickedElementIndex = previousResults.lastOrNull()?.clickedElementIndex
            Log.d("surprise", "loadPreviousResults: go to clicked $clickedElementIndex")
            if (clickedElementIndex != null) {
                if (clickedElementIndex >= 0) {
                    Log.d("surprise", "loadFromHistory: clicked item id is $clickedElementIndex")
                    val position =
                        (binding.resultsList.adapter as MyAdapterInterface?)?.getItemPositionById(
                            clickedElementIndex
                        )
                    if (position != null && position >= 0) {
                        binding.resultsList.scrollToPosition(position)
                        (binding.resultsList.adapter as MyAdapterInterface).markClickedElement(
                            clickedElementIndex
                        )
                    } else {
                        binding.resultsList.layoutManager?.scrollToPosition(viewModel.getScrolledPosition())
                    }
                } else {
                    binding.resultsList.layoutManager?.scrollToPosition(viewModel.getScrolledPosition())
                }
            }
            if ((binding.resultsList.adapter as MyAdapterInterface?)?.containsBooks() == true) {
                binding.massLoadFab.show()
            } else {
                binding.massLoadFab.hide()
            }
        }
    }

    private fun handleSortOptions() {
        sortShowSpinner = binding.sortShowSpinner
        sortShowSpinner.setSortList(
            SortHandler().getBookSortOptions(requireContext()),
            SelectedSortTypeHandler.instance.getBookSortOptionIndex()
        )
        sortShowSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, pos: Int,
                id: Long
            ) {
                if ((parent.adapter as OpdsSortAdapter).notFirstSelection) {
                    SelectedSortTypeHandler.instance.saveSortType(
                        binding.searchType.checkedRadioButtonId,
                        pos
                    )
                    (parent.adapter as OpdsSortAdapter).setSelection(pos)
                }
                sortShowSpinner.notifySelection()
                (binding.resultsList.adapter as MyAdapterInterface).sort()
            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {
                Log.d("surprise", "TimetableFragment onNothingSelected 54: nothing selected")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.odps_menu, menu)
        if (!PreferencesHandler.instance.showFilterStatistics) {
            menu.findItem(R.id.action_show_filter)?.isVisible = false
        }
        // check when request link in bookmarks list
        if (BookmarkHandler.instance.bookmarkInList(viewModel.getBookmarkLink())) {
            val item = menu.findItem(R.id.action_add_bookmark)
            item.icon = ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_baseline_bookmark_border_24,
                requireActivity().theme
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                item.icon.setTint(
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (this::binding.isInitialized) {
            outState.putString(STATE_SEARCH_VALUE, binding.bookSearchView.query.toString())
        }
    }

    override fun buttonPressed(item: FoundEntity) {
        when (item.type) {
            TYPE_AUTHOR -> {
                if (item.link?.startsWith("/opds/new/0/newauthors") == true) {
                    // go to link
                    bookmarkReservedName = "Книги автора ${item.name}"
                    newRequestLaunched()
                    mLastRequest = RequestItem(
                        item.link!!,
                        append = false,
                        addToHistory = true,
                        clickedElementIndex = (binding.resultsList.adapter as MyAdapterInterface).getClickedItemId()
                    )
                    viewModel.request(
                        mLastRequest
                    )
                } else {
                    // выдам список действий по автору
                    showAuthorViewSelect(item)
                }
            }
            TYPE_BOOK -> {
                // show window for book download prepare
                val goDownloadIntent =
                    Intent(requireContext(), DownloadBookSetupActivity::class.java)
                goDownloadIntent.putExtra("EXTRA_BOOK", item)
                startActivity(goDownloadIntent)
            }
            else -> {
                bookmarkReservedName = item.name
                // перейду по ссылке
                newRequestLaunched()
                mLastRequest = RequestItem(
                    item.link!!,
                    append = false,
                    addToHistory = true,
                    clickedElementIndex = (binding.resultsList.adapter as MyAdapterInterface).getClickedItemId()
                )
                viewModel.request(
                    mLastRequest
                )
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
        bookmarkReservedName = item.name
        when (item.type) {
            TYPE_AUTHOR -> {
                if (item.link?.startsWith("/opds/new/0/newauthors") == true) {
                    // go to link
                    newRequestLaunched()
                    mLastRequest = RequestItem(
                        item.link!!,
                        append = false,
                        addToHistory = true,
                        clickedElementIndex = (binding.resultsList.adapter as MyAdapterInterface).getClickedItemId()
                    )
                    viewModel.request(
                        mLastRequest
                    )
                } else {
                    // выдам список действий по автору
                    showAuthorViewSelect(item)
                }
            }
            else -> {
                newRequestLaunched()
                mLastRequest = RequestItem(
                    item.link!!,
                    append = false,
                    addToHistory = true,
                    clickedElementIndex = (binding.resultsList.adapter as MyAdapterInterface).getClickedItemId()
                )
                // перейду по ссылке
                viewModel.request(
                    mLastRequest
                )
            }
        }
    }

    override fun buttonLongPressed(item: FoundEntity, target: String) {
        // add value to filter
        viewModel.applyFilters(
            item,
            target,
            (binding.resultsList.adapter as MyAdapterInterface?)?.getList(),
        )

    }

    override fun itemLongPressed(item: FoundEntity) {
        TODO("Not yet implemented")
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
        newRequestLaunched()
        if (viewModel.getNextPageLink() != null) {
            mLastRequest = RequestItem(
                viewModel.getNextPageLink()!!,
                append = true,
                addToHistory = false,
                clickedElementIndex = -1
            )
            viewModel.request(
                mLastRequest
            )
        }
    }

    private fun newRequestLaunched() {
        // disable filter
        (binding.resultsList.adapter as MyAdapterInterface).setFilterEnabled(false)
        binding.filterListView.visibility = View.GONE
        binding.filterListView.setQuery("", false)
        binding.filterByType.visibility = View.GONE
        binding.useFilterBtn.visibility = View.VISIBLE

        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        binding.fab.show()
        binding.massLoadFab.hide()
        binding.foundResultsQuantity.visibility = View.GONE
        (binding.resultsList.adapter as MyAdapterInterface).setLoadInProgress(true)
        activity?.invalidateOptionsMenu()
    }

    override fun authorClicked(item: FoundEntity) {
        if (item.authors.size == 1) {
            showAuthorViewSelect(item.authors[0])
        } else {
            showSelectAuthorFromList(item.authors)
        }
    }

    private fun showSelectAuthorFromList(authors: ArrayList<FoundEntity>) {
        // создам диалоговое окно
        val dialogBuilder = AlertDialog.Builder(requireContext())
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
            showAuthorViewSelect(authors[i])
        }
        dialogBuilder.show()
    }

    override fun sequenceClicked(item: FoundEntity) {
        if (item.sequences.size == 1) {
            bookmarkReservedName = item.sequences[0].name
            newRequestLaunched()
            mLastRequest = RequestItem(
                item.sequences[0].link!!,
                append = false,
                addToHistory = true,
                clickedElementIndex = (binding.resultsList.adapter as MyAdapterInterface).getClickedItemId()
            )
            viewModel.request(
                mLastRequest
            )
        } else {
            showSelectSequenceFromList(item.sequences)
        }
    }

    private fun showSelectSequenceFromList(sequences: ArrayList<FoundEntity>) {
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
            bookmarkReservedName = sequences[i].name
            newRequestLaunched()
            mLastRequest = RequestItem(
                sequences[i].link!!,
                append = false,
                addToHistory = true,
                clickedElementIndex = (binding.resultsList.adapter as MyAdapterInterface).getClickedItemId()
            )
            viewModel.request(
                mLastRequest
            )
        }
        dialogBuilder.show()
    }

    override fun nameClicked(item: FoundEntity) {
        // load info about book in backdrop
        if (item.link != null) {
            PreferencesHandler.instance.lastWebViewLink = item.link!!
            (requireActivity() as BrowserActivity).launchWebViewFromOpds()
        }
    }

    private fun scrollToTop() {
        binding.resultsList.scrollToPosition(0)
    }

    override fun receiveSearchResult(searchResult: SearchResult) {
        requireActivity().runOnUiThread {
            binding.hintContainer.visibility = View.GONE
            if (!searchResult.appended) {
                backdropFilterFragment?.clearResults()
                scrollToTop()
                (binding.resultsList.adapter as MyAdapterInterface).clearList()
            }
            (binding.resultsList.adapter as MyAdapterInterface).appendContent(searchResult.results)
            binding.foundResultsQuantity.visibility = View.VISIBLE
            if (PreferencesHandler.instance.opdsPagingType) {
                (binding.resultsList.adapter as MyAdapterInterface).setHasNext(searchResult.nextPageLink != null)
                (binding.resultsList.adapter as MyAdapterInterface).setLoadInProgress(false)
                binding.fab.hide()
                if ((binding.resultsList.adapter as MyAdapterInterface).containsBooks()) {
                    binding.massLoadFab.show()
                }
            } else {
                if (searchResult.nextPageLink == null) {
                    (binding.resultsList.adapter as MyAdapterInterface).setLoadInProgress(false)
                    binding.fab.hide()
                    if ((binding.resultsList.adapter as MyAdapterInterface).containsBooks()) {
                        binding.massLoadFab.show()
                    }
                }
            }
            setupSortView()
            if (PreferencesHandler.instance.showFilterStatistics) {
                backdropFilterFragment?.appendResults(searchResult.filteredList)
            }
        }
    }

    override fun valueFiltered(item: ArrayList<FoundEntity>) {
        activity?.runOnUiThread {
            item.forEach {
                (binding.resultsList.adapter as MyAdapterInterface?)?.itemFiltered(it)
            }
        }
    }

    private fun setupSortView() {
        when {
            (binding.resultsList.adapter as MyAdapterInterface).containsGenres() -> {
                sortShowSpinner.setSortList(
                    SortHandler().getDefaultSortOptions(requireContext()),
                    SelectedSortTypeHandler.instance.getGenreSortOptionIndex()
                )
            }
            (binding.resultsList.adapter as MyAdapterInterface).containsSequences() -> {
                sortShowSpinner.setSortList(
                    SortHandler().getDefaultSortOptions(requireContext()),
                    SelectedSortTypeHandler.instance.getSequenceSortOptionIndex()
                )
            }
            (binding.resultsList.adapter as MyAdapterInterface).containsAuthors() -> {
                sortShowSpinner.setSortList(
                    SortHandler().getAuthorSortOptions(requireContext()),
                    SelectedSortTypeHandler.instance.getAuthorSortOptionIndex()
                )
            }
            (binding.resultsList.adapter as MyAdapterInterface).containsBooks() -> {
                sortShowSpinner.setSortList(
                    SortHandler().getBookSortOptions(requireContext()),
                    SelectedSortTypeHandler.instance.getBookSortOptionIndex()
                )
            }
        }
    }

    private fun showAuthorViewSelect(author: FoundEntity) {
        // создам диалоговое окно
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.dialogTheme)
        dialogBuilder
            .setTitle(author.author)
            .setItems(mAuthorViewTypes) { _: DialogInterface?, which: Int ->
                loadAuthor(
                    which,
                    author
                )
            }
        dialogBuilder.create().show()
    }

    private fun loadAuthor(which: Int, author: FoundEntity) {
        Log.d("surprise", "OpdsFragment.kt 1350: ${author.name}")
        Log.d("surprise", "OpdsFragment.kt 1350: ${author.link}")
        Log.d("surprise", "OpdsFragment.kt 1350: ${author.id}")
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
            newRequestLaunched()
            mLastRequest = RequestItem(
                url,
                append = false,
                addToHistory = true,
                clickedElementIndex = (binding.resultsList.adapter as MyAdapterInterface).getClickedItemId()
            )
            viewModel.request(
                mLastRequest
            )
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
                if (PreferencesHandler.instance.saveOpdsHistory) {
                    if (lastResults != null) {
                        loadFromHistory(lastResults)
                        return true
                    }
                } else {
                    if (lastResults != null) {
                        val link = lastResults.searchResults.first().requestLink
                        if (link != null) {
                            newRequestLaunched()
                            // load last request
                            mLastRequest = RequestItem(
                                link,
                                append = false,
                                addToHistory = false,
                                clickedElementIndex = -1
                            )
                            viewModel.request(
                                mLastRequest
                            )
                        }
                    }
                }
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
        return true
    }

    private fun loadFromHistory(lastResults: HistoryItem) {
        (binding.resultsList.adapter as MyAdapterInterface).clearList()
        if (PreferencesHandler.instance.saveOpdsHistory) {
            loadPreviousResults(
                binding.resultsList.adapter as MyAdapterInterface,
                lastResults.searchResults
            )
        }
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
            var position = manager.findFirstCompletelyVisibleItemPosition()
            val adapter = binding.resultsList.adapter
            if (adapter != null) {
                if (position < adapter.itemCount - 1) {
                    manager.scrollToPositionWithOffset(position + 1, 10)
                    position = manager.findLastCompletelyVisibleItemPosition()
                }
                viewModel.saveScrolledPosition(position)
                if (
                    !viewModel.loadInProgress() &&
                    position == adapter.itemCount - 1 &&
                    position > lastScrolled &&
                    PreferencesHandler.instance.opdsPagingType &&
                    !PreferencesHandler.instance.isDisplayPagerButton &&
                    viewModel.getNextPageLink() != null
                ) {
                    mLastRequest = RequestItem(
                        viewModel.getNextPageLink()!!,
                        append = true,
                        addToHistory = false,
                        clickedElementIndex = -1
                    )
                    viewModel.request(
                        mLastRequest
                    )
                }
                lastScrolled = position
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

    fun loadLink(link: String) {
        linkForLoad = link
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        (binding.resultsList.adapter as MyAdapterInterface).filter.filter(query)
        binding.foundResultsQuantity.visibility = View.VISIBLE
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        (binding.resultsList.adapter as MyAdapterInterface).filter.filter(newText)
        binding.foundResultsQuantity.visibility = View.VISIBLE
        return false
    }
}