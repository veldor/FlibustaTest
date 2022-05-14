package net.veldor.flibusta_test.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityBookmarksBinding
import net.veldor.flibusta_test.model.adapter.BookmarkDirAdapter
import net.veldor.flibusta_test.model.adapter.BookmarksAdapter
import net.veldor.flibusta_test.model.delegate.SomeActionDelegate
import net.veldor.flibusta_test.model.handler.BookmarkHandler
import net.veldor.flibusta_test.model.selections.BookmarkItem
import net.veldor.flibusta_test.model.view_model.BookmarksViewModel
import java.util.*

class BookmarksActivity : BaseActivity(), SomeActionDelegate {
    private lateinit var recycler: RecyclerView
    private lateinit var binding: ActivityBookmarksBinding
    private lateinit var viewModel: BookmarksViewModel
    private var inCategory = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        viewModel = ViewModelProvider(this).get(BookmarksViewModel::class.java)
        binding = ActivityBookmarksBinding.inflate(layoutInflater)
        setContentView(binding.drawerLayout)
        setupUI()
    }

    override fun setupUI() {
        super.setupUI()
        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goToBookmarks)
        item.isEnabled = false
        item.isChecked = true
        recycler = binding.showDirContent
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = BookmarksAdapter(BookmarkHandler.instance.get(null), this, this)
    }

    override fun actionDone() {
        TODO("Not yet implemented")
    }

    override fun actionDone(item: Any, target: Any) {
        if (item is BookmarkItem && target is String) {
            if (target == "click") {
                if (item.type == BookmarkHandler.TYPE_CATEGORY) {
                    inCategory = true
                    (recycler.adapter as BookmarksAdapter).clearList()
                    (recycler.adapter as BookmarksAdapter).appendContent(
                        BookmarkHandler.instance.get(
                            item.name
                        )
                    )
                } else {
                    val intent = Intent(this, BrowserActivity::class.java)
                    intent.putExtra("link", item.link)
                    startActivity(intent)
                }
            }
        } else if (item is BookmarkItem && target is View) {
            if (item.type == BookmarkHandler.TYPE_CATEGORY) {
                // show context menu
                target.setOnCreateContextMenuListener { menu, _, _ ->
                    val menuItem: MenuItem =
                        menu.add(getString(R.string.delete_item_title))
                    menuItem.setOnMenuItemClickListener {
                        viewModel.deleteCategory(item)
                        (binding.showDirContent.adapter as BookmarksAdapter).delete(item)
                        return@setOnMenuItemClickListener true
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    target.showContextMenu(target.pivotX, target.pivotY)
                } else {
                    target.showContextMenu()
                }
            } else if (item.type == BookmarkHandler.TYPE_BOOKMARK) {
                // show context menu
                target.setOnCreateContextMenuListener { menu, _, _ ->
                    var menuItem: MenuItem =
                        menu.add(getString(R.string.delete_item_title))
                    menuItem.setOnMenuItemClickListener {
                        viewModel.deleteBookmark(item)
                        (binding.showDirContent.adapter as BookmarksAdapter).delete(item)
                        return@setOnMenuItemClickListener true
                    }
                    menuItem =
                        menu.add(getString(R.string.change_item_title))
                    menuItem.setOnMenuItemClickListener {
                        showChangeBookmarkDialog(item)
                        return@setOnMenuItemClickListener true
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    target.showContextMenu(target.pivotX, target.pivotY)
                } else {
                    target.showContextMenu()
                }
            }
        }
    }

    private fun showChangeBookmarkDialog(item: BookmarkItem) {
        val layout = layoutInflater.inflate(R.layout.dialog_catalog_bookmark, null, false)
        val linkValueView = layout.findViewById<TextInputEditText>(R.id.linkValue)
        val checkbox = layout.findViewById<CheckBox>(R.id.addNewBookmarkFolderCheckBox)
        checkbox.visibility = View.GONE
        val bookmarkNameTextView =
            layout.findViewById<TextInputEditText>(R.id.bookmarkName)
        bookmarkNameTextView.setText(item.name)
        linkValueView.setText(item.link)
        val categoryTextView =
            layout.findViewById<TextInputEditText>(R.id.addNewFolderText)
        categoryTextView.visibility = View.GONE
        val spinner = layout.findViewById<Spinner>(R.id.bookmarkFoldersSpinner)
        spinner.visibility = View.GONE
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.change_bookmark_title))
            .setView(layout)
            .setPositiveButton(getString(R.string.change_item_title)) { _, _ ->
                val newValue = BookmarkItem(
                    name = bookmarkNameTextView.text.toString(),
                    type = BookmarkHandler.TYPE_BOOKMARK,
                    link = linkValueView.text.toString()
                )
                viewModel.changeBookmark(
                    item,
                    newValue
                )
                val position = (recycler.adapter as BookmarksAdapter).getPosition(item)
                item.name = newValue.name
                item.link = newValue.link
                (recycler.adapter as BookmarksAdapter).notifyItemChanged(position)
            }
            .show()
    }

    override fun onBackPressed() {
        if (inCategory) {
            inCategory = false
            (recycler.adapter as BookmarksAdapter).clearList()
            (recycler.adapter as BookmarksAdapter).appendContent(
                BookmarkHandler.instance.get(
                    null
                )
            )
        } else {
            super.onBackPressed()
        }
    }
}