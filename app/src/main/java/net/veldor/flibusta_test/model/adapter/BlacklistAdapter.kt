package net.veldor.flibusta_test.model.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibusta_test.BR
import net.veldor.flibusta_test.databinding.BlacklistItemBinding
import net.veldor.flibusta_test.model.delegate.SomeActionDelegate
import net.veldor.flibusta_test.model.selections.blacklist.BlacklistItem

class BlacklistAdapter(
    private var mItems: ArrayList<BlacklistItem>,
    context: Context,
    val delegate: SomeActionDelegate
) :
    RecyclerView.Adapter<BlacklistAdapter.ViewHolder>(), Filterable {
    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(context)

    private var filteredValues: ArrayList<BlacklistItem> = mItems

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = BlacklistItemBinding.inflate(mLayoutInflater, viewGroup, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(filteredValues[i])
    }

    override fun getItemCount(): Int {
        return filteredValues.size
    }

    fun changeList(newValues: ArrayList<BlacklistItem>, filterRequest: String?) {
        val prevSize = filteredValues.size
        notifyItemRangeRemoved(0, prevSize)
        mItems = newValues
        filter.filter(filterRequest)
        notifyItemRangeInserted(0, filteredValues.size)
    }

    fun itemAdded(item: BlacklistItem?) {
        Log.d("surprise", "BlacklistAdapter.kt 48: added")
        if (item != null) {
            // add item to top of list
            mItems.add(0, item)
            notifyItemInserted(0)
        }
    }

    fun itemRemoved(item: BlacklistItem?) {
        if (item != null) {
            var foundedItem: BlacklistItem? = null
            filteredValues.forEach {
                if (it.name == item.name && it.type == item.type) {
                    foundedItem = it
                }
            }
            if (foundedItem != null) {
                mItems.remove(foundedItem)
                notifyItemRemoved(filteredValues.indexOf(foundedItem))
                filteredValues.remove(foundedItem)
            }
        }
    }

    fun getItemPosition(target: BlacklistItem): Int {
        return filteredValues.indexOf(target)
    }

    inner class ViewHolder(private val mBinding: BlacklistItemBinding) : RecyclerView.ViewHolder(
        mBinding.root
    ) {
        fun bind(item: BlacklistItem) {
            mBinding.setVariable(BR.blacklists, item)
            mBinding.executePendingBindings()
            mBinding.rootView.setOnClickListener {
                delegate.actionDone(mBinding.rootView, item)
            }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString() ?: ""
                filteredValues = if (charString.isEmpty()) {
                    mItems
                } else {
                    val filteredList = ArrayList<BlacklistItem>()
                    mItems
                        .filter {
                            (it.name.lowercase().contains(constraint!!))
                        }
                        .forEach { filteredList.add(it) }
                    filteredList
                }
                return FilterResults().apply { values = filteredValues }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredValues = if (results?.values == null)
                    ArrayList()
                else {
                    val r = results.values as ArrayList<*>
                    val result: ArrayList<BlacklistItem> = arrayListOf()
                    r.forEach {
                        if (it is BlacklistItem) {
                            result.add(it)
                        }
                    }
                    result
                }
                notifyItemRangeChanged(0, filteredValues.size)
            }
        }
    }
}