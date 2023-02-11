package net.veldor.flibusta_test.view.components

import android.content.Context
import android.util.AttributeSet
import net.veldor.flibusta_test.model.adapter.DownloadFormatAdapter
import net.veldor.flibusta_test.model.selection.DownloadFormat


class DownloadFormatSpinner(context: Context, attributeSet: AttributeSet) :
    androidx.appcompat.widget.AppCompatSpinner(context, attributeSet) {

    private var myAdapter: DownloadFormatAdapter? = null

    fun setSortList(list: List<DownloadFormat?>, mime: String?) {
        myAdapter = DownloadFormatAdapter(list, context, mime)
        adapter = myAdapter
        setSelection(myAdapter!!.getSelectedOption())
    }

    fun notifySelection() {
        if (myAdapter?.notFirstSelection != true) {
            myAdapter?.notFirstSelection = true
        }
    }

    init {
        myAdapter = DownloadFormatAdapter(null, context, null)
        adapter = myAdapter
    }
}