package net.veldor.flibusta_test.view.subscribe_fragments

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentSubscribeRulesBinding
import net.veldor.flibusta_test.databinding.InputFieldBinding
import net.veldor.flibusta_test.model.adapter.SubscribeRulesAdapter
import net.veldor.flibusta_test.model.delegate.SomeActionDelegate
import net.veldor.flibusta_test.model.selection.subscribe.*
import net.veldor.flibusta_test.view.components.AddBlacklistItemDialog
import net.veldor.flibusta_test.view.components.AddSubscribeItemDialog

class SubscribeFragment : Fragment(), SomeActionDelegate, SearchView.OnQueryTextListener {
    private lateinit var binding: FragmentSubscribeRulesBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSubscribeRulesBinding.inflate(layoutInflater)
        binding.filterListView.setOnQueryTextListener(this)

        val adapter =
            SubscribeRulesAdapter(
                SubscribeBooks.getSubscribeList(),
                requireActivity(),
                this
            )
        binding.resultsList.layoutManager = LinearLayoutManager(requireContext())
        binding.resultsList.adapter = adapter

        binding.addSubscriptionItemBtn.setOnClickListener {
            val dialog = AddSubscribeItemDialog()
            AddSubscribeItemDialog.type = when (binding.subscriptionType.checkedRadioButtonId) {
                R.id.subscribeBook -> {
                    SubscribeItem.TYPE_BOOK
                }
                R.id.subscribeAuthor -> {
                    SubscribeItem.TYPE_AUTHOR
                }
                R.id.searchGenre -> {
                    SubscribeItem.TYPE_GENRE
                }
                R.id.subscribeSequence -> {
                    SubscribeItem.TYPE_SEQUENCE
                }
                else -> {
                    SubscribeItem.TYPE_BOOK
                }
            }
            AddSubscribeItemDialog.value = null
            AddSubscribeItemDialog.callback = {
                when (binding.subscriptionType.checkedRadioButtonId) {
                    R.id.subscribeBook -> {
                        (binding.resultsList.adapter as SubscribeRulesAdapter).changeList(
                            SubscribeBooks.getSubscribeList(),
                            binding.filterListView.query.toString()
                        )
                    }
                    R.id.subscribeAuthor -> {
                        (binding.resultsList.adapter as SubscribeRulesAdapter).changeList(
                            SubscribeAuthors.getSubscribeList(),
                            binding.filterListView.query.toString()
                        )
                    }
                    R.id.subscribeSequence -> {
                        (binding.resultsList.adapter as SubscribeRulesAdapter).changeList(
                            SubscribeSequences.getSubscribeList(),
                            binding.filterListView.query.toString()
                        )
                    }
                    R.id.subscribeGenre -> {
                        (binding.resultsList.adapter as SubscribeRulesAdapter).changeList(
                            SubscribeGenre.getSubscribeList(),
                            binding.filterListView.query.toString()
                        )
                    }
                }
            }
            dialog.showNow(requireActivity().supportFragmentManager, AddBlacklistItemDialog.TAG)
        }

        binding.subscriptionType.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.subscribeBook -> {
                    (binding.resultsList.adapter as SubscribeRulesAdapter).changeList(
                        SubscribeBooks.getSubscribeList(),
                        binding.filterListView.query.toString()
                    )
                }
                R.id.subscribeAuthor -> {
                    (binding.resultsList.adapter as SubscribeRulesAdapter).changeList(
                        SubscribeAuthors.getSubscribeList(),
                        binding.filterListView.query.toString()
                    )
                }
                R.id.subscribeSequence -> {
                    (binding.resultsList.adapter as SubscribeRulesAdapter).changeList(
                        SubscribeSequences.getSubscribeList(),
                        binding.filterListView.query.toString()
                    )
                }
                R.id.subscribeGenre -> {
                    (binding.resultsList.adapter as SubscribeRulesAdapter).changeList(
                        SubscribeGenre.getSubscribeList(),
                        binding.filterListView.query.toString()
                    )
                }
            }
        }


        binding.useFilterBtn.setOnClickListener {
            binding.filterListView.visibility = View.VISIBLE
            it.visibility = View.GONE
        }

        return binding.root
    }


    override fun actionDone() {

    }

    override fun actionDone(item: Any, target: Any) {
        // show context menu for element
        if (item is View && target is SubscribeItem) {
            item.setOnCreateContextMenuListener { menu, _, _ ->
                var menuItem: MenuItem =
                    menu.add(getString(R.string.delete_item_title))
                menuItem.setOnMenuItemClickListener {
                    SubscribeType.delete(target)
                    (binding.resultsList.adapter as SubscribeRulesAdapter).itemRemoved(target)
                    return@setOnMenuItemClickListener true
                }
                menuItem =
                    menu.add(getString(R.string.edit_item_title))
                menuItem.setOnMenuItemClickListener {
                    showValueEditDialog(target)
                    return@setOnMenuItemClickListener true
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                item.showContextMenu(item.pivotX, item.pivotY)
            } else {
                item.showContextMenu()
            }
        }
    }

    private fun showValueEditDialog(target: SubscribeItem) {
        val bind = InputFieldBinding.inflate(layoutInflater)
        bind.input.hint = getString(R.string.edit_subscibe_hint)
        AlertDialog.Builder(requireActivity())
            .setView(bind.root)
            .setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                val text = bind.input.text?.toString()
                if (!text.isNullOrEmpty()) {
                    val position =
                        (binding.resultsList.adapter as SubscribeRulesAdapter).getItemPosition(
                            target
                        )
                    SubscribeType.change(target, text)
                    (binding.resultsList.adapter as SubscribeRulesAdapter).notifyItemChanged(
                        position
                    )
                }
            }
            .show()
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        (binding.resultsList.adapter as SubscribeRulesAdapter).filter.filter(query)
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        (binding.resultsList.adapter as SubscribeRulesAdapter).filter.filter(newText)
        return false
    }
}