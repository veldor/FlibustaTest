package net.veldor.flibusta_test.ui.filter_fragments

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
import com.google.android.material.textfield.TextInputEditText
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentFilterRulesBinding
import net.veldor.flibusta_test.model.adapter.BlacklistAdapter
import net.veldor.flibusta_test.model.delegate.SomeActionDelegate
import net.veldor.flibusta_test.model.selections.blacklist.*

class FilterRulesFragment : Fragment(), SomeActionDelegate, SearchView.OnQueryTextListener {
    private lateinit var binding: FragmentFilterRulesBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFilterRulesBinding.inflate(layoutInflater)

        binding.filterListView.setOnQueryTextListener(this)

        val adapter =
            BlacklistAdapter(BlacklistBooks.instance.getBlacklist(), requireActivity(), this)
        binding.resultsList.layoutManager = LinearLayoutManager(requireContext())
        binding.resultsList.adapter = adapter

        binding.blacklistType.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.blacklistBook -> {
                    (binding.resultsList.adapter as BlacklistAdapter).changeList(BlacklistBooks.instance.getBlacklist(), binding.filterListView.query.toString())
                }
                R.id.blacklistAuthor -> {
                    (binding.resultsList.adapter as BlacklistAdapter).changeList(BlacklistAuthors.instance.getBlacklist(), binding.filterListView.query.toString())
                }
                R.id.blacklistSequence -> {
                    (binding.resultsList.adapter as BlacklistAdapter).changeList(BlacklistSequences.instance.getBlacklist(), binding.filterListView.query.toString())
                }
                R.id.blacklistGenre -> {
                    (binding.resultsList.adapter as BlacklistAdapter).changeList(BlacklistGenre.instance.getBlacklist(), binding.filterListView.query.toString())
                }
                R.id.blacklistFormat -> {
                    (binding.resultsList.adapter as BlacklistAdapter).changeList(BlacklistFormat.instance.getBlacklist(), binding.filterListView.query.toString())
                }
            }
        }

        binding.addToBlacklistBtn.setOnClickListener {
            addToBlacklist()
        }

        binding.blacklistItemInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Do whatever you want here
                addToBlacklist()
                true
            } else false
        }

        return binding.root
    }


    private fun addToBlacklist() {
        val value = binding.blacklistItemInput.text.toString().trim { it <= ' ' }
        if (value.isNotEmpty()) {
            // добавлю подписку в зависимости от типа
            val newItem = when (binding.blacklistType.checkedRadioButtonId) {
                R.id.blacklistBook -> {
                    BlacklistBooks.instance.addValue(value)
                }
                R.id.blacklistAuthor -> {
                    BlacklistAuthors.instance.addValue(value)
                }
                R.id.blacklistSequence -> {
                    BlacklistSequences.instance.addValue(value)
                }
                R.id.blacklistGenre -> {
                    BlacklistGenre.instance.addValue(value)
                }
                R.id.blacklistFormat -> {
                    BlacklistFormat.instance.addValue(value)
                }
                else -> null
            }
            if (newItem != null) {
                (binding.resultsList.adapter as BlacklistAdapter).itemAdded(newItem)
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
        if (item is View && target is BlacklistItem) {
            item.setOnCreateContextMenuListener { menu, _, _ ->
                var menuItem: MenuItem =
                    menu.add(getString(R.string.delete_item_title))
                menuItem.setOnMenuItemClickListener {
                    BlacklistType.delete(target)
                    (binding.resultsList.adapter as BlacklistAdapter).itemRemoved(target)
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

    private fun showValueEditDialog(target: BlacklistItem) {
        val view = layoutInflater.inflate(R.layout.input_field, null)
        view.findViewById<TextInputEditText?>(R.id.input)?.setText(target.name)
        AlertDialog.Builder(requireActivity())
            .setView(view)
            .setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                val text = view.findViewById<TextInputEditText?>(R.id.input)?.text?.toString()
                if (!text.isNullOrEmpty()) {
                    val position =
                        (binding.resultsList.adapter as BlacklistAdapter).getItemPosition(target)
                    BlacklistType.change(target, text)
                    (binding.resultsList.adapter as BlacklistAdapter).notifyItemChanged(position)
                }
            }
            .show()
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        (binding.resultsList.adapter as BlacklistAdapter).filter.filter(query)
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        (binding.resultsList.adapter as BlacklistAdapter).filter.filter(newText)
        return false
    }
}