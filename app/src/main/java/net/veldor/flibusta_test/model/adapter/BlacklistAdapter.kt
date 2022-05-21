package net.veldor.flibusta_test.model.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibusta_test.BR
import net.veldor.flibusta_test.databinding.BlacklistItemBinding
import net.veldor.flibusta_test.model.delegate.SomeActionDelegate
import net.veldor.flibusta_test.model.selections.blacklist.*
import java.util.*

class BlacklistAdapter(private var mItems: ArrayList<BlacklistItem>, private val context: Context, val delegate: SomeActionDelegate) :
    RecyclerView.Adapter<BlacklistAdapter.ViewHolder>() {
    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(context)

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = BlacklistItemBinding.inflate(mLayoutInflater, viewGroup, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(mItems[i])
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    fun changeList(autocompleteValues: ArrayList<BlacklistItem>) {
        val prevSize = mItems.size
        notifyItemRangeRemoved(0, prevSize)
        mItems = autocompleteValues
        notifyItemRangeInserted(0, mItems.size)
    }

    fun itemAdded(item: BlacklistItem?) {
        if (item != null) {
            // add item to top of list
            mItems.add(0, item)
            notifyItemInserted(0)
        }
    }

    fun itemRemoved(item: BlacklistItem?) {
        if (item != null) {
            var foundedItem: BlacklistItem? = null
            mItems.forEach {
                if (it.name == item.name && it.type == item.type) {
                    foundedItem = it
                }
            }
            if (foundedItem != null) {
                notifyItemRemoved(mItems.indexOf(foundedItem))
                mItems.remove(foundedItem)
            }
        }
    }

    fun getItemPosition(target: BlacklistItem): Int {
        return mItems.indexOf(target)
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
}