package net.veldor.flibusta_test.model.view_model

import android.os.Build
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.veldor.flibusta_test.model.delegate.SomeActionDelegate
import net.veldor.flibusta_test.model.handler.GrammarHandler
import net.veldor.flibusta_test.model.handler.SortHandler
import net.veldor.flibusta_test.model.selections.FileItem

class DirContentViewModel : ViewModel() {
    fun loadDirContent(dir: DocumentFile?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (dir != null && dir.isDirectory) {
                    val items = scan(dir.listFiles())
                    SortHandler().sortFiles(items, 0)
                    _liveFilesLoaded.postValue(items)
                }
            }
        }
    }

    private val _liveFilesLoaded = MutableLiveData<ArrayList<FileItem>>()
    val liveFilesLoaded: LiveData<ArrayList<FileItem>> = _liveFilesLoaded


    private fun scan(files: Array<DocumentFile>): ArrayList<FileItem> {
        val answer = ArrayList<FileItem>()
        if (files.isNotEmpty()) {
            var fileItem: FileItem
            for (df in files) {
                fileItem = FileItem()
                fileItem.name = df.name ?: ""
                fileItem.file = df
                answer.add(fileItem)
                if (df.isDirectory) {
                    fileItem.type = "_dir"
                } else {
                    fileItem.size = GrammarHandler.getTextSize(df.length())
                    if (fileItem.name.endsWith(".zip")) {
                        fileItem.type = fileItem.name.dropLast(4).substringAfterLast(".")
                    } else {
                        fileItem.type = fileItem.name.substringAfterLast(".")
                    }
                }
            }
        }
        return answer
    }

    fun sortList(list: java.util.ArrayList<FileItem>?, which: Int, delegate: SomeActionDelegate) {
        if (list != null) {
            viewModelScope.launch(Dispatchers.IO) {
                Log.d("surprise", "sortList: start sort")
                SortHandler().sortFiles(list, which)
                Log.d("surprise", "sortList: finish sort")
                delegate.actionDone()
            }
        }
    }
}