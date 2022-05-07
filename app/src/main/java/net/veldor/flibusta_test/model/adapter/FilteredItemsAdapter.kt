package net.veldor.flibusta_test.model.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibusta_test.BR
import net.veldor.flibusta_test.databinding.FilteredListViewBinding
import net.veldor.flibusta_test.model.selections.opds.FoundEntity

class FilteredItemsAdapter(
    arrayList: ArrayList<FoundEntity>?,
    val context: Context
) :
    RecyclerView.Adapter<FilteredItemsAdapter.ViewHolder>() {
    private var values: ArrayList<FoundEntity> = arrayListOf()


    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(context)


    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = FilteredListViewBinding.inflate(
            mLayoutInflater, viewGroup, false
        )
        return ViewHolder(binding)
    }


    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(values[i])
    }

    override fun getItemCount(): Int {
        return values.size
    }

    fun appendList(list: java.util.ArrayList<FoundEntity>) {
        if (values.isEmpty()) {
            values = list
            notifyDataSetChanged()
        } else {
            val previousSize = values.size
            values += list
            notifyItemRangeInserted(previousSize, list.size)
        }
    }

    fun clear() {
        val previousSize = values.size
        values = arrayListOf()
        notifyItemRangeRemoved(0, previousSize)
    }


    inner class ViewHolder(private val binding: FilteredListViewBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {
        fun bind(item: FoundEntity) {
            binding.setVariable(BR.item, item)
            binding.executePendingBindings()
            binding.filterReason.text = item.filterResult?.toString()
        }
    }

    init {
        if (arrayList != null && arrayList.isNotEmpty()) {
            values = arrayList
        }
    }
}