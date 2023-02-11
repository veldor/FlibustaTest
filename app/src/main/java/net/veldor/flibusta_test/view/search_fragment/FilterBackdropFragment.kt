package net.veldor.flibusta_test.view.search_fragment

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
import net.veldor.flibusta_test.model.selection.OpdsStatement
import net.veldor.flibusta_test.model.view_model.OpdsViewModel
import java.util.*

class FilterBackdropFragment : Fragment() {
    private lateinit var viewModel: OpdsViewModel
    private lateinit var binding: FragmentFilterBackdropBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[OpdsViewModel::class.java]
        binding = FragmentFilterBackdropBinding.inflate(inflater, container, false)
        setupUI()
        return binding.root
    }

    private fun setupUI() {
        val adapter =
            FilteredItemsAdapter(OpdsStatement.blockedEntities, requireActivity())
        binding.filterList.adapter = adapter
        binding.filterList.layoutManager = LinearLayoutManager(requireContext())
    }

    fun updateBlockedCount() {
        binding.filterCount.text = String.format(
            Locale.ENGLISH,
            getString(R.string.blocked_entities_pattern),
            OpdsStatement.getBlockedResultsSize()
        )
    }

    fun updateList() {
        (binding.filterList.adapter as FilteredItemsAdapter?)?.requireUpdate()
    }
}