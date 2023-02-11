package net.veldor.flibusta_test.view.download_fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentDownloadScheduleStatementBinding
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.handler.DownloadHandler
import net.veldor.flibusta_test.model.handler.GrammarHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.view_model.DownloadScheduleViewModel
import net.veldor.flibusta_test.view.search_fragment.DirectorySelectFragment
import java.util.*

class DownloadScheduleStatementFragment : DirectorySelectFragment() {
    private lateinit var viewModel: DownloadScheduleViewModel
    lateinit var binding: FragmentDownloadScheduleStatementBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadScheduleStatementBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this).get(DownloadScheduleViewModel::class.java)
        setupUI()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        setupObservers()
    }

    private fun setupUI() {
        binding.actionDropAll.setOnClickListener {
            viewModel.dropDownloadQueue()
        }
        binding.dropAllBtn.setOnClickListener {
            viewModel.dropDownloadQueue()
        }
        binding.stopDownloadBtn.setOnClickListener {
            DownloadHandler.cancelDownload()
        }
        binding.startDownloadButton.setOnClickListener {
            prepareToDownload {
                DownloadHandler.startDownload()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        unbindObservers()
    }

    private fun setupObservers() {
        DownloadHandler.liveBookDownloadProgress.observe(viewLifecycleOwner) {
            binding.totalBooksInQueue.text =
                String.format(
                    Locale.ENGLISH,
                    getString(R.string.total_books_in_queue_pattern),
                    it.booksInQueue
                )
            binding.successBooksLoads.text =
                String.format(
                    Locale.ENGLISH,
                    getString(R.string.success_loads_pattern),
                    it.successLoads
                )
            binding.errorBooksLoads.text =
                String.format(
                    Locale.ENGLISH,
                    getString(R.string.error_loads_pattern),
                    it.loadErrors
                )
            binding.stopDownloadBtn.text =
                String.format(
                    Locale.ENGLISH,
                    getString(R.string.stop_download_title),
                    it.loadErrors
                )
            binding.loadingNowBookName.text = it.currentlyLoadedBookName
            binding.totalLoadProgress.max = it.booksInQueue
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                binding.totalLoadProgress.setProgress(it.loadErrors + it.successLoads, true)
            } else {
                binding.totalLoadProgress.progress = it.loadErrors + it.successLoads
            }
            binding.currentBookLoadProgress.max = (it.bookFullSize / 1000).toInt()
            binding.currentBookLoadProgress.isIndeterminate = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                binding.currentBookLoadProgress.setProgress(
                    (it.bookLoadedSize / 1000).toInt(),
                    true
                )
            } else {
                binding.currentBookLoadProgress.progress = (it.bookLoadedSize / 1000).toInt()
            }
            binding.totalLoadProgressText.text = String.format(
                Locale.ENGLISH,
                getString(R.string.of_pattern),
                it.loadErrors + it.successLoads + 1,
                it.booksInQueue
            )
            if (it.bookLoadedSize > 0) {
                binding.currentLoadProgressText.text = String.format(
                    Locale.ENGLISH,
                    getString(R.string.of_string_pattern),
                    GrammarHandler.getTextSize(it.bookLoadedSize),
                    GrammarHandler.getTextSize(it.bookFullSize)
                )
            } else {
                binding.currentBookLoadProgress.isIndeterminate = true
                binding.currentLoadProgressText.text = getString(R.string.waiting)
            }
        }

        DownloadHandler.downloadInProgress.observe(viewLifecycleOwner) {
            binding.currentState.text =
                if (it) getString(R.string.download_running_state_title) else getString(R.string.download_stopped_state_title)
            if (!PreferencesHandler.isEInk) {
                binding.currentState.setTextColor(
                    if (it) ResourcesCompat.getColor(
                        resources,
                        R.color.genre_text_color,
                        null
                    ) else ResourcesCompat.getColor(
                        resources,
                        R.color.book_name_color,
                        requireActivity().theme
                    )
                )
            }
            binding.runningStateOptions.visibility = if (it) View.VISIBLE else View.GONE
            binding.stoppedStateOptions.visibility = if (it) View.GONE else View.VISIBLE
        }
        DatabaseInstance.mDatabase.booksDownloadScheduleDao().allBooksLive?.observe(
            viewLifecycleOwner
        ) {
            if (it.isEmpty()) {
                binding.currentState.text = getString(R.string.empty_download_queue_status_title)
                if (!PreferencesHandler.isEInk) {
                    binding.currentState.setTextColor(
                        ResourcesCompat.getColor(
                            resources,
                            R.color.author_text_color,
                            requireActivity().theme
                        )
                    )
                }
                binding.startDownloadButton.isEnabled = false
                binding.startDownloadButton.text = getString(R.string.download_queue_empty_title)
                binding.dropAllBtn.isEnabled = false
                binding.actionDropAll.isEnabled = false
                binding.dropAllBtn.text = getString(R.string.download_queue_empty_title)
                binding.actionDropAll.text = getString(R.string.download_queue_empty_title)
            } else {
                binding.startDownloadButton.isEnabled = true
                binding.startDownloadButton.text =
                    String.format(
                        Locale.ENGLISH,
                        getString(R.string.start_download_pattern),
                        it.size
                    )
                binding.dropAllBtn.isEnabled = true
                binding.dropAllBtn.text =
                    String.format(Locale.ENGLISH, getString(R.string.drop_queue_pattern), it.size)
                binding.actionDropAll.isEnabled = true
                binding.actionDropAll.text =
                    String.format(Locale.ENGLISH, getString(R.string.drop_queue_pattern), it.size)
            }
        }
    }

    private fun unbindObservers() {
        DownloadHandler.downloadInProgress.removeObservers(viewLifecycleOwner)
        DatabaseInstance.mDatabase.booksDownloadScheduleDao().allBooksLive?.removeObservers(
            viewLifecycleOwner
        )
        DownloadHandler.liveBookDownloadProgress.removeObservers(viewLifecycleOwner)
    }
}