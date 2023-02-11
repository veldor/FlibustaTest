package net.veldor.flibusta_test.model.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibusta_test.BR
import net.veldor.flibusta_test.databinding.SubscribeItemBinding
import net.veldor.flibusta_test.model.delegate.SomeActionDelegate
import net.veldor.flibusta_test.model.selection.subscribe.SubscribeItem

class SubscribeRulesAdapter(
    private var mItems: ArrayList<SubscribeItem>,
    context: Context,
    val delegate: SomeActionDelegate
) :
    RecyclerView.Adapter<SubscribeRulesAdapter.ViewHolder>(), Filterable {
    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(context)

    private var filteredValues: ArrayList<SubscribeItem> = mItems

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = SubscribeItemBinding.inflate(mLayoutInflater, viewGroup, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(filteredValues[i])
    }

    override fun getItemCount(): Int {
        return filteredValues.size
    }

    fun changeList(newValues: ArrayList<SubscribeItem>, filterRequest: String?) {
        val prevSize = filteredValues.size
        notifyItemRangeRemoved(0, prevSize)
        mItems = newValues
        filter.filter(filterRequest)
        notifyItemRangeInserted(0, filteredValues.size)
    }

    fun itemAdded(item: SubscribeItem?) {
        if (item != null) {
            // add item to top of list
            if (mItems.isEmpty()) {
                mItems.add(item)
            }
            filteredValues.add(0, item)
            notifyItemInserted(0)
        }
    }

    fun itemRemoved(item: SubscribeItem?) {
        if (item != null) {
            var foundedItem: SubscribeItem? = null
            var foundedMItem: SubscribeItem? = null
            filteredValues.forEach {
                if (it.name == item.name && it.type == item.type) {
                    foundedItem = it
                }
            }
            if (foundedItem != null) {
                notifyItemRemoved(filteredValues.indexOf(foundedItem))
                filteredValues.remove(foundedItem)
            }
            mItems.forEach {
                if (it.name == item.name && it.type == item.type) {
                    foundedMItem = it
                }
            }
            if (foundedMItem != null) {
                mItems.remove(foundedMItem)
            }
        }
    }

    fun getItemPosition(target: SubscribeItem): Int {
        return filteredValues.indexOf(target)
    }

    inner class ViewHolder(private val mBinding: SubscribeItemBinding) : RecyclerView.ViewHolder(
        mBinding.root
    ) {
        fun bind(item: SubscribeItem) {
            mBinding.setVariable(BR.item, item)
            mBinding.executePendingBindings()
            mBinding.name.text = item.name
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
                    val filteredList = ArrayList<SubscribeItem>()
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
                    val result: ArrayList<SubscribeItem> = arrayListOf()
                    r.forEach {
                        if (it is SubscribeItem) {
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