package net.veldor.flibusta_test.model.components

import android.content.Context
import android.util.AttributeSet
import net.veldor.flibusta_test.model.adapter.FormatAdapter
import net.veldor.flibusta_test.model.selections.DownloadLink


class FormatSpinner(context: Context, attributeSet: AttributeSet) :
    androidx.appcompat.widget.AppCompatSpinner(context, attributeSet) {


    var notFirstSelection: Boolean = false
    private var myAdapter: FormatAdapter? = null

    fun setSortList(list: List<DownloadLink?>, mime: String?) {
        myAdapter = FormatAdapter(list, context)
        myAdapter?.setSelection(mime)
        adapter = myAdapter
        setSelection(myAdapter!!.selected)
    }

    fun notifySelection() {
        if (!notFirstSelection) {
            notFirstSelection = true
        }
    }

    init {
        myAdapter = FormatAdapter(null, context)
        adapter = myAdapter
    }
}