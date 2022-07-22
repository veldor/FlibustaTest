package net.veldor.flibusta_test.ui.subscribe_fragments

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.android.material.textfield.TextInputEditText
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentSubscribeRulesBinding
import net.veldor.flibusta_test.model.adapter.SubscribeRulesAdapter
import net.veldor.flibusta_test.model.delegate.SomeActionDelegate
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.selections.subscribe.*
import net.veldor.flibusta_test.model.worker.CheckSubscriptionsWorker
import java.util.concurrent.TimeUnit

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
                SubscribeBooks.instance.getSubscribeList(),
                requireActivity(),
                this
            )
        binding.resultsList.layoutManager = LinearLayoutManager(requireContext())
        binding.resultsList.adapter = adapter

        binding.blacklistType.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.blacklistBook -> {
                    (binding.resultsList.adapter as SubscribeRulesAdapter).changeList(
                        SubscribeBooks.instance.getSubscribeList(),
                        binding.filterListView.query.toString()
                    )
                }
                R.id.blacklistAuthor -> {
                    (binding.resultsList.adapter as SubscribeRulesAdapter).changeList(
                        SubscribeAuthors.instance.getSubscribeList(),
                        binding.filterListView.query.toString()
                    )
                }
                R.id.blacklistSequence -> {
                    (binding.resultsList.adapter as SubscribeRulesAdapter).changeList(
                        SubscribeSequences.instance.getSubscribeList(),
                        binding.filterListView.query.toString()
                    )
                }
                R.id.blacklistGenre -> {
                    (binding.resultsList.adapter as SubscribeRulesAdapter).changeList(
                        SubscribeGenre.instance.getSubscribeList(),
                        binding.filterListView.query.toString()
                    )
                }
            }
        }

        binding.addToBlacklistBtn.setOnClickListener {
            addToSubscribes()
        }

        binding.blacklistItemInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Do whatever you want here
                addToSubscribes()
                true
            } else false
        }

        binding.useFilterBtn.setOnClickListener {
            binding.filterListView.visibility = View.VISIBLE
            it.visibility = View.GONE
        }

        return binding.root
    }


    private fun addToSubscribes() {
        val value = binding.blacklistItemInput.text.toString().trim { it <= ' ' }
        if (value.isNotEmpty()) {
            // добавлю подписку в зависимости от типа
            val newItem = when (binding.blacklistType.checkedRadioButtonId) {
                R.id.blacklistBook -> {
                    SubscribeBooks.instance.addValue(value)
                }
                R.id.blacklistAuthor -> {
                    SubscribeAuthors.instance.addValue(value)
                }
                R.id.blacklistSequence -> {
                    SubscribeSequences.instance.addValue(value)
                }
                R.id.blacklistGenre -> {
                    SubscribeGenre.instance.addValue(value)
                }
                else -> null
            }
            if (newItem != null) {
                (binding.resultsList.adapter as SubscribeRulesAdapter).itemAdded(newItem)
                Toast.makeText(requireContext(), "Добавляю значение $value", Toast.LENGTH_LONG)
                    .show()
            } else {
                Toast.makeText(requireContext(), "Значение $value уже в списке.", Toast.LENGTH_LONG)
                    .show()
            }
            binding.blacklistItemInput.setText("")
        } else {
            Toast.makeText(requireContext(), "Введите значение", Toast.LENGTH_LONG).show()
            binding.blacklistItemInput.requestFocus()
        }
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
        val view = layoutInflater.inflate(R.layout.input_field, null)
        view.findViewById<TextInputEditText?>(R.id.input)?.setText(target.name)
        AlertDialog.Builder(requireActivity())
            .setView(view)
            .setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                val text = view.findViewById<TextInputEditText?>(R.id.input)?.text?.toString()
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