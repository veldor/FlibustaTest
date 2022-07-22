package net.veldor.flibusta_test.ui.download_schedule_fragments

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentDownloadScheduleListBinding
import net.veldor.flibusta_test.model.adapter.DownloadScheduleAdapter
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.db.entity.BooksDownloadSchedule
import net.veldor.flibusta_test.model.delegate.SomeButtonPressedDelegate
import net.veldor.flibusta_test.model.handler.DownloadHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.view_model.DownloadScheduleViewModel


class DownloadScheduleListFragment : Fragment(), SomeButtonPressedDelegate {
    private lateinit var viewModel: DownloadScheduleViewModel
    private lateinit var binding: FragmentDownloadScheduleListBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadScheduleListBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this).get(DownloadScheduleViewModel::class.java)
        setupObservers()
        setupUI()
        return binding.root
    }

    private fun setupUI() {
        if(PreferencesHandler.instance.isEInk){
            binding.fab.backgroundTintList = ColorStateList.valueOf(ResourcesCompat.getColor(requireActivity().resources, R.color.black, requireActivity().theme))
        }

        setHasOptionsMenu(true)
        activity?.invalidateOptionsMenu()
        val adapter =
            DownloadScheduleAdapter(DatabaseInstance.instance.mDatabase.booksDownloadScheduleDao().allBooks, this, requireActivity())
        binding.resultsList.layoutManager = LinearLayoutManager(requireContext())
        binding.resultsList.adapter = adapter
        binding.fab.setOnClickListener {
            if (DownloadHandler.instance.downloadInProgress.value == true) {
                DownloadHandler.instance.cancelDownload()
            } else {
                DownloadHandler.instance.startDownload()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.download_queue_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_drop_all -> {viewModel.dropDownloadQueue()}
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupObservers() {
        DownloadHandler.instance.liveBookDownloadProgress.observe(viewLifecycleOwner) {
            (binding.resultsList.adapter as DownloadScheduleAdapter).setProgress(it)
        }

        DatabaseInstance.instance.mDatabase.booksDownloadScheduleDao().allBooksLive?.observe(
            viewLifecycleOwner
        ) {
            binding.emptyListText.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            if(it.isEmpty()){
                binding.fab.hide()
            }
            else{
                binding.fab.show()
            }
            (binding.resultsList.adapter as DownloadScheduleAdapter).setList(it)
        }
        DownloadHandler.instance.downloadInProgress.observe(viewLifecycleOwner){
            if(it){
                binding.fab.setImageResource(R.drawable.ic_baseline_pause_24)
            }
            else{
                binding.fab.setImageResource(R.drawable.ic_baseline_arrow_downward_24)
            }
        }
    }


    override fun buttonPressed(boundElement: Any) {
        if(boundElement is BooksDownloadSchedule){
            viewModel.deleteFromQueue(boundElement)
        }
    }
}