package net.veldor.flibusta_test.view.blacklist_fragments

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
import com.google.android.material.textfield.TextInputEditText
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FragmentBlacklistRulesBinding
import net.veldor.flibusta_test.model.adapter.BlacklistAdapter
import net.veldor.flibusta_test.model.delegate.SomeActionDelegate
import net.veldor.flibusta_test.model.selection.filter.*
import net.veldor.flibusta_test.view.components.AddBlacklistItemDialog

class BlacklistRulesFragment : Fragment(), SomeActionDelegate, SearchView.OnQueryTextListener {
    private lateinit var binding: FragmentBlacklistRulesBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBlacklistRulesBinding.inflate(layoutInflater)

        binding.addBlacklistItemBtn.setOnClickListener {
            val dialog = AddBlacklistItemDialog()
            AddBlacklistItemDialog.type = when (binding.blacklistType.checkedRadioButtonId) {
                R.id.blacklistBook -> {
                    BlacklistItem.TYPE_BOOK
                }
                R.id.blacklistAuthor -> {
                    BlacklistItem.TYPE_AUTHOR
                }
                R.id.blacklistSequence -> {
                    BlacklistItem.TYPE_SEQUENCE
                }
                R.id.blacklistGenre -> {
                    BlacklistItem.TYPE_GENRE
                }
                R.id.blacklistFormat -> {
                    BlacklistItem.TYPE_FORMAT
                }
                else -> {
                    BlacklistItem.TYPE_BOOK
                }
            }
            AddBlacklistItemDialog.value = null
            AddBlacklistItemDialog.callback = {
                when (binding.blacklistType.checkedRadioButtonId) {
                    R.id.blacklistBook -> {
                        (binding.resultsList.adapter as BlacklistAdapter).changeList(
                            BlacklistBooks.getBlacklist(),
                            binding.filterListView.query.toString()
                        )
                    }
                    R.id.blacklistAuthor -> {
                        (binding.resultsList.adapter as BlacklistAdapter).changeList(
                            BlacklistAuthors.getBlacklist(),
                            binding.filterListView.query.toString()
                        )
                    }
                    R.id.blacklistSequence -> {
                        (binding.resultsList.adapter as BlacklistAdapter).changeList(
                            BlacklistSequences.getBlacklist(),
                            binding.filterListView.query.toString()
                        )
                    }
                    R.id.blacklistGenre -> {
                        (binding.resultsList.adapter as BlacklistAdapter).changeList(
                            BlacklistGenre.getBlacklist(),
                            binding.filterListView.query.toString()
                        )
                    }
                    R.id.blacklistFormat -> {
                        (binding.resultsList.adapter as BlacklistAdapter).changeList(
                            BlacklistFormat.getBlacklist(),
                            binding.filterListView.query.toString()
                        )
                    }
                }
            }
            dialog.showNow(requireActivity().supportFragmentManager, AddBlacklistItemDialog.TAG)
        }

        binding.filterListView.setOnQueryTextListener(this)

        binding.blacklistType.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.blacklistBook -> {
                    (binding.resultsList.adapter as BlacklistAdapter).changeList(
                        BlacklistBooks.getBlacklist(),
                        binding.filterListView.query.toString()
                    )
                }
                R.id.blacklistAuthor -> {
                    (binding.resultsList.adapter as BlacklistAdapter).changeList(
                        BlacklistAuthors.getBlacklist(),
                        binding.filterListView.query.toString()
                    )
                }
                R.id.blacklistSequence -> {
                    (binding.resultsList.adapter as BlacklistAdapter).changeList(
                        BlacklistSequences.getBlacklist(),
                        binding.filterListView.query.toString()
                    )
                }
                R.id.blacklistGenre -> {
                    (binding.resultsList.adapter as BlacklistAdapter).changeList(
                        BlacklistGenre.getBlacklist(),
                        binding.filterListView.query.toString()
                    )
                }
                R.id.blacklistFormat -> {
                    (binding.resultsList.adapter as BlacklistAdapter).changeList(
                        BlacklistFormat.getBlacklist(),
                        binding.filterListView.query.toString()
                    )
                }
            }
        }
        val adapter =
            BlacklistAdapter(BlacklistBooks.getBlacklist(), requireActivity(), this)
        binding.resultsList.layoutManager = LinearLayoutManager(requireContext())
        binding.resultsList.adapter = adapter



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