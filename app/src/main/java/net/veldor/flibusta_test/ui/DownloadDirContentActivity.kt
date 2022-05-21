package net.veldor.flibusta_test.ui

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityDownloadedBinding
import net.veldor.flibusta_test.model.adapter.ExplorerAdapter
import net.veldor.flibusta_test.model.delegate.FileItemClickedDelegate
import net.veldor.flibusta_test.model.delegate.SomeActionDelegate
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.helper.BookActionsHelper
import net.veldor.flibusta_test.model.selections.FileItem
import net.veldor.flibusta_test.model.view_model.DirContentViewModel

class DownloadDirContentActivity : BaseActivity(), FileItemClickedDelegate, SomeActionDelegate,
    SearchView.OnQueryTextListener {
    private var mConfirmExit: Long = 0
    private lateinit var recycler: RecyclerView
    private lateinit var viewModel: DirContentViewModel
    private lateinit var binding: ActivityDownloadedBinding

    private var currentDir: DocumentFile? = null
    private var mainDir: DocumentFile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        viewModel = ViewModelProvider(this).get(DirContentViewModel::class.java)
        binding = ActivityDownloadedBinding.inflate(layoutInflater)
        setContentView(binding.drawerLayout)
        setupUI()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.liveFilesLoaded.observe(this) {
            binding.waiter.visibility = View.INVISIBLE
            if (binding.showDirContent.adapter == null) {
                val adapter = ExplorerAdapter(it, this)
                binding.showDirContent.adapter = adapter
            } else {
                (binding.showDirContent.adapter as ExplorerAdapter).clearList()
                (binding.showDirContent.adapter as ExplorerAdapter).appendContent(
                    it,
                    binding.filterListView.query.toString()
                )
            }
        }
    }

    override fun setupUI() {
        super.setupUI()
        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goToFileList)
        item.isEnabled = false
        item.isChecked = true

        binding.filterListView.setOnQueryTextListener(this)
        recycler = binding.showDirContent
        recycler.layoutManager = LinearLayoutManager(this)
        // получу список файлов из папки
        currentDir = PreferencesHandler.instance.getDownloadDir()
        mainDir = PreferencesHandler.instance.getDownloadDir()
        viewModel.loadDirContent(PreferencesHandler.instance.getDownloadDir())
    }

    override fun itemClicked(item: FileItem, view: View, position: Int) {
        if (item.file.isDirectory) {
            currentDir = item.file
            viewModel.loadDirContent(item.file)
        } else {
            view.setOnCreateContextMenuListener { menu, _, _ ->
                var menuItem: MenuItem =
                    menu.add(getString(R.string.share_title))
                menuItem.setOnMenuItemClickListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        BookActionsHelper.shareBook(item.file)
                    }
                    return@setOnMenuItemClickListener true
                }
                menuItem =
                    menu.add(getString(R.string.open_title))
                menuItem.setOnMenuItemClickListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        BookActionsHelper.openBook(item.file)
                    }
                    return@setOnMenuItemClickListener true
                }
                menuItem =
                    menu.add(getString(R.string.delete_item_title))
                menuItem.setOnMenuItemClickListener {
                    item.file.delete()
                    (binding.showDirContent.adapter as ExplorerAdapter).delete(item)
                    return@setOnMenuItemClickListener true
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                view.showContextMenu(view.pivotX, view.pivotY)
            } else {
                view.showContextMenu()
            }
        }
    }

    override fun itemLongClicked(item: FileItem, view: View, position: Int) {
        Log.d("surprise", "itemLongClicked: long clicked")
        if (item.file.isDirectory) {
            view.setOnCreateContextMenuListener { menu, _, _ ->
                val menuItem: MenuItem =
                    menu.add(getString(R.string.delete_item_title))
                menuItem.setOnMenuItemClickListener {
                    item.file.delete()
                    (binding.showDirContent.adapter as ExplorerAdapter).delete(item)
                    return@setOnMenuItemClickListener true
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                view.showContextMenu(view.pivotX, view.pivotY)
            } else {
                view.showContextMenu()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START) //CLOSE Nav Drawer!
                return true
            }
            if (currentDir != null && currentDir?.uri != mainDir?.uri) {
                viewModel.loadDirContent(currentDir?.parentFile)
                binding.waiter.visibility = View.VISIBLE
                currentDir = currentDir?.parentFile
                return true
            }
            if (mConfirmExit != 0L) {
                if (mConfirmExit > System.currentTimeMillis() - 3000) {
                    // выйду из приложения
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.press_back_again_for_exit_title),
                        Toast.LENGTH_SHORT
                    ).show()
                    mConfirmExit = System.currentTimeMillis()
                    return true
                }
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.press_back_again_for_exit_title),
                    Toast.LENGTH_SHORT
                ).show()
                mConfirmExit = System.currentTimeMillis()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.explorer_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.action_sort) {
            selectSorting()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun selectSorting() {
        val dialog = AlertDialog.Builder(this, R.style.dialogTheme)
        dialog.setTitle("Выберите тип сортировки")
            .setItems(bookSortOptions) { _: DialogInterface?, which: Int ->
                binding.waiter.visibility = View.VISIBLE
                viewModel.sortList(
                    (binding.showDirContent.adapter as ExplorerAdapter).getList(),
                    which,
                    this
                )
            }
        dialog.show()
    }

    private val bookSortOptions = arrayOf(
        "По имени от А",
        "По имени от Я",
        "По размеру от большого",
        "По размеру от малого",
        "По типу от А",
        "По типу от Z",
        "По времени скачивания от свежих",
        "По времени скачивания от старых",
    )

    override fun actionDone() {
        runOnUiThread {
            binding.waiter.visibility = View.INVISIBLE
            (binding.showDirContent.adapter as ExplorerAdapter).notifyItemRangeChanged(
                0,
                (binding.showDirContent.adapter as ExplorerAdapter).itemCount
            )
        }
    }

    override fun actionDone(item: Any, target: Any) {
        TODO("Not yet implemented")
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        (binding.showDirContent.adapter as ExplorerAdapter).filter.filter(query)
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        (binding.showDirContent.adapter as ExplorerAdapter).filter.filter(newText)
        return false
    }

}