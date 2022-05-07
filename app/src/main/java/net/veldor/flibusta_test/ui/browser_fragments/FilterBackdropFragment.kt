package net.veldor.flibusta_test.ui.browser_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentFilterBackdropBinding
import net.veldor.flibusta_test.model.adapter.FilteredItemsAdapter
import net.veldor.flibusta_test.model.selections.opds.FoundEntity
import net.veldor.flibusta_test.model.view_model.OpdsViewModel
import java.util.*
import kotlin.collections.ArrayList

class FilterBackdropFragment : Fragment() {
    private lateinit var viewModel: OpdsViewModel
    private lateinit var binding: FragmentFilterBackdropBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(OpdsViewModel::class.java)
        binding = FragmentFilterBackdropBinding.inflate(inflater, container, false)
        setupUI()
        return binding.root
    }

    private fun setupUI() {
        val adapter = FilteredItemsAdapter(arrayListOf(), requireActivity())
        binding.filterList.adapter = adapter
        binding.filterList.layoutManager = LinearLayoutManager(requireContext())
    }

    fun appendResults(list: ArrayList<FoundEntity>) {
        (binding.filterList.adapter as FilteredItemsAdapter?)?.appendList(list)
        binding.filterCount.text = String.format(Locale.ENGLISH, getString(R.string.filtered_pattern), (binding.filterList.adapter as FilteredItemsAdapter?)?.itemCount)
    }

    fun clearResults() {
        (binding.filterList.adapter as FilteredItemsAdapter?)?.clear()
    }
}