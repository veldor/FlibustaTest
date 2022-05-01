package net.veldor.flibusta_test.ui.download_schedule_fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentDownloadScheduleErrorsListBinding
import net.veldor.flibusta_test.model.adapter.DownloadScheduleErrorAdapter
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.db.entity.BooksDownloadSchedule
import net.veldor.flibusta_test.model.db.entity.DownloadError
import net.veldor.flibusta_test.model.delegate.SomeButtonPressedDelegate
import net.veldor.flibusta_test.model.view_model.DownloadScheduleViewModel


class DownloadScheduleListErrorsFragment : Fragment(), SomeButtonPressedDelegate {
    private lateinit var viewModel: DownloadScheduleViewModel
    private lateinit var binding: FragmentDownloadScheduleErrorsListBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadScheduleErrorsListBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this).get(DownloadScheduleViewModel::class.java)
        setupObservers()
        setupUI()
        return binding.root
    }

    private fun setupUI() {
        val adapter =
            DownloadScheduleErrorAdapter(DatabaseInstance.instance.mDatabase.booksDownloadScheduleDao().allBooks, this, requireActivity())
        binding.resultsList.layoutManager = LinearLayoutManager(requireContext())
        binding.resultsList.adapter = adapter
        setHasOptionsMenu(true)
        activity?.invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.download_errors_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_reload_all -> viewModel.reloadAllErrors()
            R.id.action_drop_all -> viewModel.dropErrorQueue()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupObservers() {
        DatabaseInstance.instance.mDatabase.downloadErrorDao().allBooksLive?.observe(
            viewLifecycleOwner
        ) {
            binding.emptyListText.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            (binding.resultsList.adapter as DownloadScheduleErrorAdapter).setList(it)
        }
    }

    override fun buttonPressed(boundElement: Any) {
        if(boundElement is DownloadError){
            viewModel.reloadError(boundElement)
        }
    }
}