package net.veldor.flibusta_test.view.download_files_fragments

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentFilesInDirBinding
import net.veldor.flibusta_test.model.adapter.FolderAdapter
import net.veldor.flibusta_test.model.handler.FilesHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.helper.BookActionsHelper
import net.veldor.flibusta_test.model.selection.FilePojo
import net.veldor.flibusta_test.model.selection.RootDownloadDir
import net.veldor.flibusta_test.model.view_model.FilesViewModel
import java.io.File
import java.util.*


class FilesInDirFragment : Fragment() {
    private var folderAdapter: FolderAdapter? = null
    private lateinit var mViewModel: FilesViewModel
    private lateinit var binding: FragmentFilesInDirBinding

    private var folderAndFileList: MutableList<FilePojo> = mutableListOf()
    private var foldersList: MutableList<FilePojo> = mutableListOf()
    private var filesList: MutableList<FilePojo> = mutableListOf()
    private var modernDirectory: DocumentFile? = null
    private var compatDirectory: File? = null
    private var rootDir: RootDownloadDir = PreferencesHandler.rootDownloadDir

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFilesInDirBinding.inflate(layoutInflater)
        mViewModel = ViewModelProvider(this)[FilesViewModel::class.java]
        if (rootDir.root != null) {
            modernDirectory = rootDir.root
            loadLists(modernDirectory)
        } else if (rootDir.compatRoot != null) {
            compatDirectory = rootDir.compatRoot
            loadLists(compatDirectory)
        } else if (PreferencesHandler.storageAccessDenied) {
            compatDirectory = App.instance.getExternalFilesDir("media")
            loadLists(compatDirectory)
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity?)?.supportActionBar?.title =
            getString(R.string.files_from_dir_title)
    }

    private fun loadLists(documentFile: DocumentFile?) {
        if (documentFile != null) {
            try {
                if (!documentFile.isDirectory) {
                    showNoDirectoryNotification()
                }
                val files = documentFile.listFiles()
                foldersList = mutableListOf()
                filesList = mutableListOf()
                files.forEach {
                    if (it.isDirectory) {
                        val filePojo = FilePojo(it.name, true)
                        foldersList.add(filePojo)
                    } else {
                        val filePojo = FilePojo(it.name, false)
                        filesList.add(filePojo)
                    }
                }

                // sort & add to final List - as we show folders first add folders first to the final list
                Collections.sort(foldersList, comparatorAscending)
                folderAndFileList = mutableListOf()
                folderAndFileList.addAll(foldersList)
                Collections.sort(filesList, comparatorAscending)
                folderAndFileList.addAll(filesList)
                showList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadLists(folder: File?) {
        try {
            if (folder?.isDirectory == false) exit()
            val files = folder?.listFiles()
            foldersList = ArrayList<FilePojo>()
            filesList = ArrayList<FilePojo>()
            files?.forEach {
                if (it.isDirectory) {
                    val filePojo = FilePojo(it.name, true)
                    foldersList.add(filePojo)
                } else {
                    val filePojo = FilePojo(it.name, false)
                    filesList.add(filePojo)
                }
            }

            // sort & add to final List - as we show folders first add folders first to the final list
            Collections.sort(foldersList, comparatorAscending)
            folderAndFileList = ArrayList<FilePojo>()
            folderAndFileList.addAll(foldersList)

            Collections.sort(filesList, comparatorAscending)
            folderAndFileList.addAll(filesList)
            showList()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } // load List


    private fun showList() {
        try {
            folderAdapter = FolderAdapter(requireActivity(), ArrayList(folderAndFileList))
            val listView = binding.fpListView
            listView.adapter = folderAdapter
            listView.onItemClickListener =
                AdapterView.OnItemClickListener { _, _, position, _ -> listClick(position) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun listClick(position: Int) {
        if (!folderAndFileList[position].isFolder) {
            if (modernDirectory != null) {
                val file = modernDirectory!!.findFile(folderAndFileList[position].name)
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
                                file.delete()
                                folderAdapter?.itemDeleted(position)
                            } else {
                                mViewModel.sendToCompanion(requireContext(), file, FilesHandler.getBookRelativePath(file))
                            }
                        }
                        .show()
                } else {
                    Toast.makeText(requireContext(), "File not found", Toast.LENGTH_SHORT).show()
                }
            } else if (compatDirectory != null) {
                val file = File(compatDirectory, folderAndFileList[position].name)
                if (file.isFile) {
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
                                file.delete()
                                folderAdapter?.itemDeleted(position)
                            } else {
                                mViewModel.sendToCompanion(file, FilesHandler.getBookRelativePath(file))
                            }
                        }
                        .show()
                } else {
                    Toast.makeText(requireContext(), "File not found", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            if (modernDirectory != null) {
                modernDirectory = modernDirectory!!.findFile(folderAndFileList[position].name)
                loadLists(modernDirectory)
            } else if (compatDirectory != null) {
                compatDirectory = File(compatDirectory, folderAndFileList[position].name)
                loadLists(compatDirectory)
            }
        }
    }

    private fun showNoDirectoryNotification() {
        Toast.makeText(requireContext(), "No dir", Toast.LENGTH_SHORT).show()
    }

    private var comparatorAscending: Comparator<FilePojo> =
        Comparator<FilePojo> { f1, f2 -> f1.name.compareTo(f2.name) }


    fun goBack(): Boolean {
        if (modernDirectory != null) {
            if (modernDirectory!!.parentFile == null) {
                exit()
            } else {
                modernDirectory = modernDirectory!!.parentFile
                loadLists(modernDirectory)
            }
        }
        return true
    }

    private fun exit() {
        activity?.finish()
    }
}