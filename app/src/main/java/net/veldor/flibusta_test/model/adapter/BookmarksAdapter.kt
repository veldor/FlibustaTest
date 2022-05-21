package net.veldor.flibusta_test.model.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibusta_test.BR
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.BookmarkItemBinding
import net.veldor.flibusta_test.model.delegate.SomeActionDelegate
import net.veldor.flibusta_test.model.handler.BookmarkHandler.Companion.TYPE_CATEGORY
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.selections.BookmarkItem
import net.veldor.flibusta_test.model.selections.FileItem

class BookmarksAdapter(
    arrayList: ArrayList<BookmarkItem>?,
    val context: Context,
    val delegate: SomeActionDelegate
) :
    RecyclerView.Adapter<BookmarksAdapter.ViewHolder>(), Filterable {

    private var values: ArrayList<BookmarkItem> = arrayListOf()
    private var filteredValues: ArrayList<BookmarkItem> = arrayListOf()


    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(context)


    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = BookmarkItemBinding.inflate(
            mLayoutInflater, viewGroup, false
        )
        return ViewHolder(binding)
    }

    fun clearList() {
        notifyItemRangeRemoved(0, itemCount)
        values = ArrayList()
        filteredValues = ArrayList()
        notifyItemRangeInserted(0, 0)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun appendContent(results: ArrayList<BookmarkItem>, filterRequest: String?) {
        values.addAll(results)
        filter.filter(filterRequest)
    }


    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(filteredValues[i], i)
    }

    override fun getItemCount(): Int {
        return filteredValues.size
    }

    fun delete(item: BookmarkItem) {
        val position = filteredValues.lastIndexOf(item)
        values.remove(item)
        if (position >= 0) {
            filteredValues.remove(item)
            notifyItemRemoved(position)
        }
    }

    fun getPosition(item: BookmarkItem): Int {
        return filteredValues.lastIndexOf(item)
    }

    fun remove(position: Int) {
        values.removeAt(filteredValues.lastIndexOf(filteredValues[position]))
        filteredValues.removeAt(position)
        notifyItemRemoved(position)
    }

    inner class ViewHolder(private val binding: BookmarkItemBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {

        init {
            if(PreferencesHandler.instance.isEInk){
                binding.name.setTextColor(ResourcesCompat.getColor(
                    context.resources,
                    R.color.black,
                    context.theme
                ))
            }
        }

        fun bind(item: BookmarkItem, position: Int) {
            binding.setVariable(BR.item, item)
            binding.executePendingBindings()
            if (item.type == TYPE_CATEGORY) {
                binding.linkText.visibility = View.GONE
                binding.itemType.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.ic_baseline_folder_24,
                        context.theme
                    )
                )
            } else {
                binding.linkText.visibility = View.VISIBLE
                binding.itemType.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.ic_baseline_bookmark_border_24,
                        context.theme
                    )
                )
            }

            binding.root.setOnClickListener {
                delegate.actionDone(item, "click")
            }
            binding.root.setOnLongClickListener {
                delegate.actionDone(item, binding.root)
                return@setOnLongClickListener true
            }
        }
    }

    init {
        if (arrayList != null && arrayList.isNotEmpty()) {
            values = arrayList
            filteredValues = values
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString() ?: ""
                filteredValues = if (charString.isEmpty()) {
                    values
                } else {
                    val filteredList = java.util.ArrayList<BookmarkItem>()
                    values
                        .filter {
                            (it.name.lowercase().contains(charString))

                        }
                        .forEach {
                            filteredList.add(it)
                        }
                    filteredList
                }
                return FilterResults().apply { values = filteredValues }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredValues = if (results?.values != null) {
                    val r = results.values as java.util.ArrayList<*>
                    val result: java.util.ArrayList<BookmarkItem> = arrayListOf()
                    r.forEach {
                        if (it is BookmarkItem) {
                            result.add(it)
                        }
                    }
                    result
                } else
                    arrayListOf()
                notifyItemRangeChanged(0, filteredValues.size)
            }
        }
    }
}