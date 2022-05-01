package net.veldor.flibusta_test.model.components

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import net.veldor.flibusta_test.model.adapter.OpdsSortAdapter
import net.veldor.flibusta_test.model.selections.SortOption


class SortShowSpinner(context: Context, attributeSet: AttributeSet) :
    androidx.appcompat.widget.AppCompatSpinner(context, attributeSet) {


    private var myAdapter: OpdsSortAdapter? = null

    fun setSortList(list: List<SortOption?>, selectedOption: Int) {
        myAdapter = OpdsSortAdapter(list, context)
        myAdapter?.setSelection(selectedOption)
        adapter = myAdapter
        setSelection(selectedOption)
    }

    fun notifySelection() {
        Log.d("surprise", "PersonShowSpinner notifySelection 23: selection");
        if (myAdapter?.notFirstSelection == true) {
            Log.d("surprise", "PersonShowSpinner notifySelection 26: change saved");
        } else {
            Log.d("surprise", "PersonShowSpinner notifySelection 30: skip first selection");
            myAdapter?.notFirstSelection = true
        }
    }

    init {
        myAdapter = OpdsSortAdapter(null, context)
        adapter = myAdapter
    }
}