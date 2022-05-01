package net.veldor.flibusta_test.model.delegate

import android.view.View
import net.veldor.flibusta_test.model.selections.FileItem

interface FileItemClickedDelegate {
    fun itemClicked(item: FileItem, view: View, position: Int)
    fun itemLongClicked(item: FileItem, view: View, position: Int)
}