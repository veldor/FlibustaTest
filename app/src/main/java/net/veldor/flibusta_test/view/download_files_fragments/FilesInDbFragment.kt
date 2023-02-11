package net.veldor.flibusta_test.view.download_files_fragments

import android.app.AlertDialog
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentFilesInDbBinding
import net.veldor.flibusta_test.model.adapter.DownloadedBooksAdapter
import net.veldor.flibusta_test.model.data_source.DownloadFilesDataSource
import net.veldor.flibusta_test.model.data_source.DownloadedBooksDiffCallback
import net.veldor.flibusta_test.model.db.entity.DownloadedBooks
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.helper.BookActionsHelper
import net.veldor.flibusta_test.model.listener.DownloadedBookClicked
import net.veldor.flibusta_test.model.view_model.FilesViewModel
import java.io.File
import java.net.URI
import java.util.concurrent.Executor
import java.util.concurrent.Executors


class FilesInDbFragment : Fragment(), DownloadedBookClicked {
    private var adapter: DownloadedBooksAdapter? = null
    private lateinit var mViewModel: FilesViewModel
    private lateinit var binding: FragmentFilesInDbBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFilesInDbBinding.inflate(layoutInflater)
        mViewModel = ViewModelProvider(this)[FilesViewModel::class.java]
        handleList()
        return binding.root
    }

    private fun handleList() {
        val dataSource = DownloadFilesDataSource()
        val config = PagedList.Config.Builder()
            .setEnablePlaceholders(true)
            .setPageSize(5)
            .build()

        val pageList: PagedList<DownloadedBooks> = PagedList.Builder(dataSource, config)
            .setFetchExecutor(Executors.newSingleThreadExecutor())
            .setNotifyExecutor(MainThreadExecutor())
            .build()

        binding.resultsList.layoutManager = LinearLayoutManager(requireContext())
        adapter = DownloadedBooksAdapter(DownloadedBooksDiffCallback())
        adapter?.listener = this
        adapter?.submitList(pageList)
        binding.resultsList.adapter = adapter
    }


    internal class MainThreadExecutor : Executor {
        private val handler: Handler = Handler(Looper.getMainLooper())
        override fun execute(command: Runnable) {
            handler.post(command)
        }
    }

    override fun clicked(item: DownloadedBooks?) {
        activity?.runOnUiThread {
            if (item?.destination != null) {
                if (item.destination!!.startsWith("content")) {
                    val file =
                        DocumentFile.fromSingleUri(requireContext(), Uri.parse(item.destination))
                    if (file?.isFile == true) {
                        AlertDialog.Builder(requireContext())
                            .setTitle(file.name)
                            .setItems(
                                if (PreferencesHandler.useCompanionApp) {
                                    arrayOf(
                                        getString(R.string.share_title),
                                        getString(R.string.open_title),
                                        getString(R.string.delete_item_title),
                                        getString(R.string.send_to_companion_app)
                                    )
                                } else {
                                    arrayOf(
                                        getString(R.string.share_title),
                                        getString(R.string.open_title),
                                        getString(R.string.delete_item_title),
                                    )
                                }
                            ) { _, which ->
                                if (which == 0) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                        BookActionsHelper.shareBook(file)
                                    }
                                } else if (which == 1) {
                                    BookActionsHelper.openBook(file)
                                } else if (which == 2) {
                                    mViewModel.deleteItem(item)
                                    file.delete()
                                    adapter?.itemDeleted(item)
                                } else {
                                    mViewModel.sendToCompanion(requireContext(), file, file.name!!)
                                }
                            }
                            .show()
                    } else {
                        AlertDialog.Builder(requireContext())
                            .setTitle(item.destination)
                            .setItems(
                                arrayOf(
                                    getString(R.string.delete_item_title)
                                )
                            ) { _, which ->
                                if (which == 0) {
                                    mViewModel.deleteItem(item)
                                    adapter?.itemDeleted(item)
                                }
                            }
                            .show()
                    }
                } else {
                    val file = File(
                        URI.create(Uri.parse(item.destination!!).toString())
                    )
                    if (file.exists()) {
                        AlertDialog.Builder(requireContext())
                            .setTitle(file.name)
                            .setItems(
                                if (PreferencesHandler.useCompanionApp) {
                                    arrayOf(
                                        getString(R.string.share_title),
                                        getString(R.string.open_title),
                                        getString(R.string.delete_item_title),
                                        getString(R.string.send_to_companion_app)
                                    )
                                } else {
                                    arrayOf(
                                        getString(R.string.share_title),
                                        getString(R.string.open_title),
                                        getString(R.string.delete_item_title),
                                    )
                                }
                            ) { _, which ->
                                if (which == 0) {
                                    BookActionsHelper.shareBook(file)
                                } else if (which == 1) {
                                    BookActionsHelper.openBook(file)
                                } else if (which == 2) {
                                    mViewModel.deleteItem(item)
                                    file.delete()
                                    adapter?.itemDeleted(item)
                                } else {
                                    if(item.destination.isNullOrEmpty()){
                                        mViewModel.sendToCompanion(file, file.name)
                                    }
                                    else{
                                        mViewModel.sendToCompanion(file, item.destination!!)
                                    }
                                }
                            }
                            .show()
                    } else {
                        AlertDialog.Builder(requireContext())
                            .setTitle(file.name)
                            .setItems(
                                arrayOf(
                                    getString(R.string.delete_item_title)
                                )
                            ) { _, which ->
                                if (which == 0) {
                                    mViewModel.deleteItem(item)
                                    adapter?.itemDeleted(item)
                                }
                            }
                            .show()
                    }
                }
            }
        }
    }
}