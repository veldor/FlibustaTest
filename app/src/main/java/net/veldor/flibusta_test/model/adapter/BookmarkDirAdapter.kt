package net.veldor.flibusta_test.model.adapter

import android.content.Context
import android.database.DataSetObserver
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.ImageView
import android.widget.SpinnerAdapter
import android.widget.TextView
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.selection.BookmarkItem

class BookmarkDirAdapter(val context: Context, private val bookmarks: List<BookmarkItem>) : SpinnerAdapter {

    var selected: Int = -1
    private var inflater: LayoutInflater = LayoutInflater.from(context)

    override fun registerDataSetObserver(p0: DataSetObserver?) {
    }

    override fun unregisterDataSetObserver(p0: DataSetObserver?) {
    }

    override fun getCount(): Int {
        return bookmarks.size
    }

    override fun getItem(position: Int): Any {
        return bookmarks[position]
    }

    override fun getItemId(p0: Int): Long {
        return 0
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        if (convertView == null) {
            view = inflater.inflate(R.layout.format_view, parent, false)
        }
        val label = view!!.findViewById<TextView>(R.id.sortLabel)
        if (bookmarks.isNotEmpty()) {
            label.text = bookmarks[position].name
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
            view = inflater.inflate(R.layout.format_dropdown_list_view, parent, false)
        }
        if (bookmarks.isEmpty()) {
            view!!.findViewById<TextView>(R.id.itemName).text =
                context.getString(R.string.no_options_title)
        } else {
            view!!.findViewById<TextView>(R.id.itemName).text =
                bookmarks[position].name
        }
        val checkView = view.findViewById<ImageView>(R.id.checkedView)
        if (position == selected) {
            checkView.visibility = View.VISIBLE
        } else {
            checkView.visibility = View.INVISIBLE
        }
        return view
    }
}