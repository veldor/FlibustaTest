package net.veldor.flibusta_test.ui.browser_fragments

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentOpdsDownloadBackdropBinding
import net.veldor.flibusta_test.model.adapter.DownloadFormatAdapter
import net.veldor.flibusta_test.model.adapter.MassDownloadAdapter
import net.veldor.flibusta_test.model.delegate.BookInfoAddedDelegate
import net.veldor.flibusta_test.model.delegate.CheckboxDelegate
import net.veldor.flibusta_test.model.handler.FormatHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.selections.opds.FoundEntity
import net.veldor.flibusta_test.model.view_model.OpdsViewModel
import java.util.*


class OpdsDownloadBackdropFragment : Fragment(), CheckboxDelegate, BookInfoAddedDelegate {

    private lateinit var viewModel: OpdsViewModel
    private lateinit var binding: FragmentOpdsDownloadBackdropBinding
    private var booksList: ArrayList<FoundEntity>? = null
    private var selectedFormat: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedFormat =
            if (PreferencesHandler.instance.rememberFavoriteFormat) PreferencesHandler.instance.favoriteFormat else FormatHandler.getAllFormats()[0].longName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentOpdsDownloadBackdropBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(OpdsViewModel::class.java)
        setupUI()
        viewModel.setBookInfoAddedDelegate(this)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.removeBookInfoAddedDelegate()
    }

    private fun setupUI() {
        if (PreferencesHandler.instance.isEInk) {
            binding.allBtn.setTextColor(ResourcesCompat.getColor(resources, R.color.einkTextColor, requireActivity().theme))
            binding.noneBtn.setTextColor(ResourcesCompat.getColor(resources, R.color.einkTextColor, requireActivity().theme))
            binding.invertBtn.setTextColor(ResourcesCompat.getColor(resources, R.color.einkTextColor, requireActivity().theme))
            binding.unloadedBtn.setTextColor(ResourcesCompat.getColor(resources, R.color.einkTextColor, requireActivity().theme))
        }
        binding.saveAsFolderCheckbox.setOnCheckedChangeListener { _, state ->
            binding.customDirName.isEnabled = state
        }
        val adapter = MassDownloadAdapter(booksList, this, requireActivity())
        val layout = LinearLayoutManager(requireContext())
        binding.resultsList.adapter = adapter
        binding.resultsList.layoutManager = layout

        binding.strictFormatCheckbox.setOnCheckedChangeListener { _, value ->
            PreferencesHandler.instance.strictDownloadFormat = value
            (binding.resultsList.adapter as MassDownloadAdapter?)?.setStrictFormat()
        }
        binding.strictFormatCheckbox.isChecked = PreferencesHandler.instance.strictDownloadFormat
        //setup format spinner

        binding.formatSpinner.setSortList(
            FormatHandler.getAllFormats(),
            if (PreferencesHandler.instance.rememberFavoriteFormat) PreferencesHandler.instance.favoriteFormat else null
        )

        binding.formatSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                pos: Int,
                id: Long
            ) {
                val selectedFormat = binding.formatSpinner.getItemAtPosition(pos)
                (binding.resultsList.adapter as MassDownloadAdapter?)?.changeDownloadFormat(
                    selectedFormat as String
                )
                if ((parent.adapter as DownloadFormatAdapter).notFirstSelection) {
                    Log.d("surprise", "onItemSelected: $selectedFormat")
                    Log.d("surprise", "onItemSelected: save here")
                    if (PreferencesHandler.instance.rememberFavoriteFormat) {
                        PreferencesHandler.instance.favoriteFormat = selectedFormat as String
                    }
                    (parent.adapter as DownloadFormatAdapter).setSelection(pos)
                }
                binding.formatSpinner.notifySelection()
            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {
                Log.d("surprise", "TimetableFragment onNothingSelected 54: nothing selected")
            }
        }

        binding.allBtn.setOnClickListener {
            (binding.resultsList.adapter as MassDownloadAdapter?)?.checkAll()
            binding.startDownloadButton.text = String.format(
                Locale.ENGLISH,
                getString(R.string.download_x_title),
                (binding.resultsList.adapter as MassDownloadAdapter?)?.countSelected()
            )
        }
        binding.noneBtn.setOnClickListener {
            (binding.resultsList.adapter as MassDownloadAdapter?)?.uncheckAll()
            binding.startDownloadButton.text = String.format(
                Locale.ENGLISH,
                getString(R.string.download_x_title),
                (binding.resultsList.adapter as MassDownloadAdapter?)?.countSelected()
            )
        }
        binding.invertBtn.setOnClickListener {
            (binding.resultsList.adapter as MassDownloadAdapter?)?.reverseCheckAll()
            binding.startDownloadButton.text = String.format(
                Locale.ENGLISH,
                getString(R.string.download_x_title),
                (binding.resultsList.adapter as MassDownloadAdapter?)?.countSelected()
            )
        }
        binding.unloadedBtn.setOnClickListener {
            (binding.resultsList.adapter as MassDownloadAdapter?)?.checkUnloaded()
            binding.startDownloadButton.text = String.format(
                Locale.ENGLISH,
                getString(R.string.download_x_title),
                (binding.resultsList.adapter as MassDownloadAdapter?)?.countSelected()
            )
        }

        binding.startDownloadButton.setOnClickListener {
            BottomSheetBehavior.from(requireView()).state = BottomSheetBehavior.STATE_HIDDEN
            (binding.resultsList.adapter as MassDownloadAdapter?)?.getList()?.forEach {
                if (binding.saveAsFolderCheckbox.isChecked && binding.customDirName.text.isNotEmpty()) {
                    it.selectedLink?.reservedSequenceName = binding.customDirName.text.toString()
                }
                viewModel.addToDownloadQueue(it.selectedLink)
            }
        }
    }

    fun loadBooksList(books: ArrayList<FoundEntity>) {
        booksList = books
        (binding.resultsList.adapter as MassDownloadAdapter?)?.setList(
            booksList,
            binding.formatSpinner.selectedItem as String
        )

        // check books for filling
        viewModel.checkItemsFilled(booksList)

        binding.startDownloadButton.text = String.format(
            Locale.ENGLISH,
            getString(R.string.download_x_title),
            (binding.resultsList.adapter as MassDownloadAdapter?)?.countSelected()
        )
    }

    override fun checked(state: Boolean) {
        binding.startDownloadButton.text = String.format(
            Locale.ENGLISH,
            getString(R.string.download_x_title),
            (binding.resultsList.adapter as MassDownloadAdapter?)?.countSelected()
        )
    }

    override fun infoAdded(book: FoundEntity) {
        requireActivity().runOnUiThread {
            (binding.resultsList.adapter as MassDownloadAdapter?)?.updateBookInfo(book)
        }
    }

    override fun checkProgress(linksChecked: Int, currentProgress: Int, size: Int?) {
        requireActivity().runOnUiThread {
            if (size != null && size > 0) {
                if (currentProgress <= size) {
                    binding.linksCheckProgress.visibility = View.VISIBLE
                    binding.linksCheckProgress.max = size
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        binding.linksCheckProgress.setProgress(currentProgress, true)
                    } else {
                        binding.linksCheckProgress.progress = currentProgress
                    }
                    binding.linkCheckProgressText.visibility = View.VISIBLE
                    binding.linkCheckProgressText.text = String.format(
                        Locale.ENGLISH,
                        getString(R.string.books_availability_pattern),
                        currentProgress,
                        size,
                        linksChecked
                    )
                } else {
                    binding.linksCheckProgress.visibility = View.GONE
                    binding.linkCheckProgressText.visibility = View.GONE
                }
            } else {
                binding.linksCheckProgress.visibility = View.GONE
                binding.linkCheckProgressText.visibility = View.GONE
            }
        }
    }

    fun cancelBookInfoLoad() {
        viewModel.cancelBookInfoLoad()
    }
}