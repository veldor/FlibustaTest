package net.veldor.flibusta_test.ui.subscribe_fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentSubscribeResultsBinding
import net.veldor.flibusta_test.model.adapter.SubscribeResultsAdapter
import net.veldor.flibusta_test.model.delegate.FoundItemActionDelegate
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.handler.SubscribesHandler
import net.veldor.flibusta_test.model.selections.opds.FoundEntity
import net.veldor.flibusta_test.model.view_model.SubscriptionsViewModel
import net.veldor.flibusta_test.model.worker.CheckSubscriptionsWorker
import net.veldor.flibusta_test.ui.DownloadBookSetupActivity
import java.util.concurrent.TimeUnit


class SubscribeResultFragment : Fragment(), FoundItemActionDelegate {
    private lateinit var viewModel: SubscriptionsViewModel
    private lateinit var binding: FragmentSubscribeResultsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSubscribeResultsBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this).get(SubscriptionsViewModel::class.java)
        binding.totalCheckButton.setOnClickListener {
            viewModel.fullCheckSubscribes()
            binding.fastCheckButton.isEnabled = false
            binding.totalCheckButton.isEnabled = false
            binding.waiter.visibility = View.VISIBLE
            (binding.resultsList.adapter as SubscribeResultsAdapter).notifyDataSetChanged()
            binding.hintContainer.visibility = View.GONE
            binding.cancelCheckBtn.visibility = View.VISIBLE
        }
        binding.fastCheckButton.setOnClickListener {
            viewModel.fastCheckSubscribes()
            binding.fastCheckButton.isEnabled = false
            binding.totalCheckButton.isEnabled = false
            binding.waiter.visibility = View.VISIBLE
            (binding.resultsList.adapter as SubscribeResultsAdapter).notifyDataSetChanged()
            binding.hintContainer.visibility = View.GONE
            binding.cancelCheckBtn.visibility = View.VISIBLE
        }

        binding.cancelCheckBtn.setOnClickListener {
            viewModel.cancelCheck()
            it.visibility = View.GONE
        }

        binding.resultsList.adapter = SubscribeResultsAdapter(
            requireContext(),
            SubscribesHandler.instance.subscribeResults,
            this
        )
        if(!(binding.resultsList.adapter as SubscribeResultsAdapter).isEmpty()){
            binding.hintContainer.visibility = View.GONE
        }
        binding.resultsList.layoutManager = LinearLayoutManager(requireContext())
        setupObservers()


        binding.autoDownloadSubscriptionResultsSwitcher.isChecked =
            PreferencesHandler.instance.autoDownloadSubscriptions

        binding.autoDownloadSubscriptionResultsSwitcher.setOnCheckedChangeListener { _, b ->
            PreferencesHandler.instance.autoDownloadSubscriptions = b
            if (b) {
                if (!PreferencesHandler.instance.rememberFavoriteFormat) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.subscription_favorite_save_title),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        binding.autoCheckSubscriptionsSwitcher.isChecked =
            PreferencesHandler.instance.autoCheckSubscriptions

        binding.autoCheckSubscriptionsSwitcher.setOnCheckedChangeListener { _, b ->
            PreferencesHandler.instance.autoCheckSubscriptions = b
            if (b) {
// Запланирую проверку подписок
                val startPeriodicalPlanner = PeriodicWorkRequest.Builder(
                    CheckSubscriptionsWorker::class.java, 24, TimeUnit.HOURS
                ).addTag(
                    CheckSubscriptionsWorker.PERIODIC_CHECK_TAG
                )
                val wm = WorkManager.getInstance(requireContext())
                wm.cancelAllWorkByTag(CheckSubscriptionsWorker.PERIODIC_CHECK_TAG)
                wm.enqueue(startPeriodicalPlanner.build())
            }
            else{
                val wm = WorkManager.getInstance(requireContext())
                wm.cancelAllWorkByTag(CheckSubscriptionsWorker.PERIODIC_CHECK_TAG)
            }
        }
        return binding.root
    }

    private fun setupObservers() {

        SubscribesHandler.instance.currentProgress.observe(viewLifecycleOwner){
            binding.currentSearchState.text = it
        }

        SubscribesHandler.instance.inProgress.observe(viewLifecycleOwner) {
            if (it) {
                binding.fastCheckButton.isEnabled = false
                binding.totalCheckButton.isEnabled = false
                binding.waiter.visibility = View.VISIBLE
                binding.hintContainer.visibility = View.GONE
                binding.cancelCheckBtn.visibility = View.VISIBLE
                binding.currentSearchState.visibility = View.VISIBLE
            } else {
                binding.fastCheckButton.isEnabled = true
                binding.totalCheckButton.isEnabled = true
                binding.waiter.visibility = View.GONE
                binding.cancelCheckBtn.visibility = View.GONE
                if((binding.resultsList.adapter as SubscribeResultsAdapter).isEmpty()){
                    binding.hintContainer.visibility = View.VISIBLE
                }
            }
        }

        SubscribesHandler.instance.foundValue.observe(viewLifecycleOwner) {
            if (it != null) {
                (binding.resultsList.adapter as SubscribeResultsAdapter?)?.bookFound(it)
                binding.resultsList.scrollToPosition(0)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (SubscribesHandler.instance.inProgress.value == true) {
            binding.fastCheckButton.isEnabled = false
            binding.totalCheckButton.isEnabled = false
            binding.waiter.visibility = View.VISIBLE
            binding.cancelCheckBtn.visibility = View.VISIBLE
        }
    }

    override fun buttonPressed(item: FoundEntity) {
        if (PreferencesHandler.instance.skipDownloadSetup && PreferencesHandler.instance.rememberFavoriteFormat && PreferencesHandler.instance.favoriteFormat != null) {
            viewModel.addToDownloadQueue(item.getFavoriteLink())
        } else {
            // show window for book download prepare
            val goDownloadIntent =
                Intent(requireContext(), DownloadBookSetupActivity::class.java)
            goDownloadIntent.putExtra("EXTRA_BOOK", item)
            startActivity(goDownloadIntent)
        }
    }

    override fun imageClicked(item: FoundEntity) {
        TODO("Not yet implemented")
    }

    override fun itemPressed(item: FoundEntity) {
        TODO("Not yet implemented")
    }

    override fun buttonLongPressed(item: FoundEntity, target: String, view: View) {
        TODO("Not yet implemented")
    }

    override fun menuItemPressed(item: FoundEntity, button: View) {
        TODO("Not yet implemented")
    }

    override fun loadMoreBtnClicked() {
        TODO("Not yet implemented")
    }

    override fun authorClicked(item: FoundEntity) {
        TODO("Not yet implemented")
    }

    override fun sequenceClicked(item: FoundEntity) {
        TODO("Not yet implemented")
    }

    override fun nameClicked(item: FoundEntity) {
        TODO("Not yet implemented")
    }

    override fun rightButtonPressed(item: FoundEntity) {
        TODO("Not yet implemented")
    }

    override fun leftButtonPressed(item: FoundEntity) {
        TODO("Not yet implemented")
    }

    override fun scrollTo(indexOf: Int) {
        TODO("Not yet implemented")
    }
}