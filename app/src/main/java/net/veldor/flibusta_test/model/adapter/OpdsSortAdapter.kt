package net.veldor.flibusta_test.model.adapter

import android.content.Context
import android.database.DataSetObserver
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.selections.SortOption

class OpdsSortAdapter(list: List<SortOption?>?, context: Context) : SpinnerAdapter {

    private var selected: Int = -1
    var notFirstSelection: Boolean = false
    private var inflater: LayoutInflater = LayoutInflater.from(context)
    private var itemList: List<SortOption?>? = null
    private var context:Context

    init {
        itemList = list
        this.context = context
    }

    override fun registerDataSetObserver(p0: DataSetObserver?) {
    }

    override fun unregisterDataSetObserver(p0: DataSetObserver?) {
    }

    override fun getCount(): Int {
        if (itemList == null) {
            return 1
        }
        return itemList!!.size
    }

    override fun getItem(position: Int): Any {
        if (itemList != null) {
            return itemList!![position]!!
        }
        return SortOption(0, context.getString(R.string.sort_option_name))
    }

    override fun getItemId(position: Int): Long {
        if(position > 0 && itemList != null && itemList!!.size >= position){
            return itemList!![position]!!.id.toLong()
        }
        return -1
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        if (convertView == null) {
            view = inflater.inflate(R.layout.sort_list_view, parent, false)
        }
        val label = view!!.findViewById<TextView>(R.id.sortLabel)
        if(itemList != null && itemList?.isNotEmpty() == true){
            label.text = itemList!![position]?.name
        }
        return view
    }

    override fun getItemViewType(p0: Int): Int {
        return Adapter.IGNORE_ITEM_VIEW_TYPE
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun isEmpty(): Boolean {
        return false
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        if (convertView == null) {
            view = inflater.inflate(R.layout.sort_dropdown_list_view, parent, false)
        }
        if(itemList.isNullOrEmpty()){
            view!!.findViewById<TextView>(R.id.itemName).text = context.getString(R.string.no_options_title)
        }
        else{
            view!!.findViewById<TextView>(R.id.itemName).text = itemList!![position]!!.name
        }
        val checkView = view.findViewById<ImageView>(R.id.checkedView)
        if(position == selected){
            checkView.visibility = View.VISIBLE
        }
        else{
            checkView.visibility = View.GONE
        }
        return view
    }

    fun setSelection(selectedOption: Int) {
        selected = selectedOption
    }


}