package net.veldor.flibusta_test.model.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibusta_test.BR
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.BookmarkItemBinding
import net.veldor.flibusta_test.model.delegate.SomeActionDelegate
import net.veldor.flibusta_test.model.handler.BookmarkHandler.Companion.TYPE_CATEGORY
import net.veldor.flibusta_test.model.selections.BookmarkItem

class BookmarksAdapter(
    arrayList: ArrayList<BookmarkItem>?,
    val context: Context,
    val delegate: SomeActionDelegate
) :
    RecyclerView.Adapter<BookmarksAdapter.ViewHolder>() {

    private var values: ArrayList<BookmarkItem> = arrayListOf()


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
        notifyItemRangeInserted(0, 0)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun appendContent(results: ArrayList<BookmarkItem>) {
        val oldLength = itemCount
        values.addAll(results)
        if (oldLength > 0) {
            notifyItemRangeInserted(oldLength, results.size)
        } else {
            notifyDataSetChanged()
        }
    }


    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(values[i], i)
    }

    override fun getItemCount(): Int {
        return values.size
    }

    fun delete(item: BookmarkItem) {
        val position = values.lastIndexOf(item)
        if (position >= 0) {
            values.remove(item)
            notifyItemRemoved(position)
        }
    }

    fun change(item: BookmarkItem, newItem: BookmarkItem) {
        TODO("Not yet implemented")
    }

    fun getPosition(item: BookmarkItem): Int {
        return values.lastIndexOf(item)
    }

    inner class ViewHolder(private val binding: BookmarkItemBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {

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
        }
    }
}