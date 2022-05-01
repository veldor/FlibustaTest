package net.veldor.flibusta_test.ui.browser_fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentOpdsDownloadBackdropBinding
import net.veldor.flibusta_test.model.adapter.DownloadFormatAdapter
import net.veldor.flibusta_test.model.adapter.MassDownloadAdapter
import net.veldor.flibusta_test.model.delegate.CheckboxDelegate
import net.veldor.flibusta_test.model.handler.FormatHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_BOOK
import net.veldor.flibusta_test.model.selections.opds.FoundEntity
import net.veldor.flibusta_test.model.view_model.OpdsViewModel
import java.util.*


class OpdsDownloadBackdropFragment : Fragment(), CheckboxDelegate {

    private lateinit var viewModel: OpdsViewModel
    private lateinit var binding: FragmentOpdsDownloadBackdropBinding
    private var booksList: List<FoundEntity>? = null
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
        return binding.root
    }

    private fun setupUI() {
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

    fun loadBooksList(list: List<FoundEntity>) {
        // clear list from no-books

        booksList = list.filter {
            return@filter it.type == TYPE_BOOK
        }
        (binding.resultsList.adapter as MassDownloadAdapter?)?.setList(
            list,
            binding.formatSpinner.selectedItem as String
        )
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
}