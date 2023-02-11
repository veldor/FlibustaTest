package net.veldor.flibusta_test.view.download_fragments

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentDownloadScheduleErrorsListBinding
import net.veldor.flibusta_test.model.adapter.DownloadScheduleErrorAdapter
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.db.entity.DownloadError
import net.veldor.flibusta_test.model.delegate.SomeButtonPressedDelegate
import net.veldor.flibusta_test.model.view_model.DownloadScheduleViewModel


class DownloadScheduleListErrorsFragment : Fragment(), SomeButtonPressedDelegate {
    private lateinit var menuProvider: MenuProvider
    private lateinit var viewModel: DownloadScheduleViewModel
    private lateinit var binding: FragmentDownloadScheduleErrorsListBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadScheduleErrorsListBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[DownloadScheduleViewModel::class.java]
        setupObservers()
        setupUI()
        return binding.root
    }

    private fun setupUI() {
        val adapter =
            DownloadScheduleErrorAdapter(DatabaseInstance.mDatabase.downloadErrorDao().allBooks, this, requireActivity())
        binding.resultsList.layoutManager = LinearLayoutManager(requireContext())
        binding.resultsList.adapter = adapter
    }

    private fun setupObservers() {
        DatabaseInstance.mDatabase.downloadErrorDao().allBooksLive?.observe(
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

    override fun onResume() {
        super.onResume()
        setMenu()
    }

    override fun onPause() {
        super.onPause()
        removeMenu()
    }


    private fun setMenu() {
        requireActivity().invalidateOptionsMenu()
        menuProvider = object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.download_errors_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                when (menuItem.itemId) {
                    R.id.action_reload_all -> viewModel.reloadAllErrors()
                    R.id.action_drop_all -> viewModel.dropErrorQueue()
                }
                return true
            }
        }
        activity?.addMenuProvider(menuProvider)
    }

    private fun removeMenu() {
        activity?.removeMenuProvider(menuProvider)
    }
}