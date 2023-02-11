package net.veldor.flibusta_test.view

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
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
import net.veldor.flibusta_test.model.selection.BookmarkItem
import net.veldor.flibusta_test.model.view_model.BookmarksViewModel


class BookmarksActivity : BaseActivity(), SomeActionDelegate, SearchView.OnQueryTextListener {
    private lateinit var recycler: RecyclerView
    private lateinit var binding: ActivityBookmarksBinding
    private lateinit var viewModel: BookmarksViewModel
    private var inCategory = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[BookmarksViewModel::class.java]
        binding = ActivityBookmarksBinding.inflate(layoutInflater)
        setContentView(binding.drawerLayout)
        setupUI()
    }

    override fun setupUI() {
        super.setupUI()
        binding.filterListView.setOnQueryTextListener(this)
        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goToBookmarks)
        item.isEnabled = false
        item.isChecked = true
        recycler = binding.showDirContent
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = BookmarksAdapter(BookmarkHandler.get(null), this, this)
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
                        BookmarkHandler.get(
                            item.name
                        ),
                        binding.filterListView.query.toString()
                    )
                } else {
                    val intent = Intent(this, SearchActivity::class.java)
                    intent.putExtra("link", item.link)
                    Log.d("surprise", "actionDone: link is ${item.link}")
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
        val bookmarkNameTextView =
            layout.findViewById<TextInputEditText>(R.id.bookmarkName)
        bookmarkNameTextView.setText(item.name)
        linkValueView.setText(item.link)
        val categoryTextView =
            layout.findViewById<TextInputEditText>(R.id.addNewFolderText)
        val spinner = layout.findViewById<Spinner>(R.id.bookmarkFoldersSpinner)
        // select current book category
        spinner.adapter = BookmarkDirAdapter(
            this,
            BookmarkHandler.getBookmarkCategories(this)
        )
        spinner.setSelection(BookmarkHandler.getCategoryPosition(this, item))
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.change_bookmark_title))
            .setView(layout)
            .setPositiveButton(getString(R.string.change_item_title)) { _, _ ->
                // handle category
                val position = (recycler.adapter as BookmarksAdapter).getPosition(item)
                val oldCategoryName = BookmarkHandler.getBookmarkCategoryName(item)
                val category: BookmarkItem = if (categoryTextView.text?.isNotEmpty() == true) {
                    BookmarkHandler.addCategory(categoryTextView.text.toString())
                } else {
                    spinner.selectedItem as BookmarkItem
                }
                item.name = bookmarkNameTextView.text.toString()
                item.link = linkValueView.text.toString()

                viewModel.changeBookmark(
                    category,
                    item
                )
                if (category.name == oldCategoryName) {
                    Log.d("surprise", "BookmarksActivity.kt 152: in same")
                    (recycler.adapter as BookmarksAdapter).notifyItemChanged(position)
                } else {
                    Log.d("surprise", "BookmarksActivity.kt 156: removed")
                    (recycler.adapter as BookmarksAdapter).remove(position)
                }
            }
            .show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (inCategory) {
                inCategory = false
                (recycler.adapter as BookmarksAdapter).clearList()
                (recycler.adapter as BookmarksAdapter).appendContent(
                    BookmarkHandler.get(
                        null
                    ),
                    binding.filterListView.query.toString()
                )
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    override fun onQueryTextSubmit(query: String?): Boolean {
        (binding.showDirContent.adapter as BookmarksAdapter).filter.filter(query)
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        (binding.showDirContent.adapter as BookmarksAdapter).filter.filter(newText)
        return false
    }

}