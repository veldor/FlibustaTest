package net.veldor.flibusta_test.model.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibusta_test.BR
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.SubscribeResultBinding
import net.veldor.flibusta_test.model.delegate.FoundItemActionDelegate
import net.veldor.flibusta_test.model.selection.FoundEntity

class SubscribeResultsAdapter(
    private val context: Context,
    private var mBooks: ArrayList<FoundEntity>,
    val delegate: FoundItemActionDelegate
) : RecyclerView.Adapter<SubscribeResultsAdapter.ViewHolder>() {


    private var lastSize: Int = mBooks.size
    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(context)

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = SubscribeResultBinding.inflate(mLayoutInflater, viewGroup, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(mBooks[i])
    }

    override fun getItemCount(): Int {
        return mBooks.size
    }

    fun bookFound(book: FoundEntity) {
        //mBooks.add(0, book)
        if(mBooks.size == lastSize + 1){
            notifyItemInserted(0)
        }
        else{
            notifyDataSetChanged()
        }
        lastSize = mBooks.size
    }

    fun clear() {
        notifyItemRangeRemoved(0, mBooks.size)
        mBooks = arrayListOf()
    }

    fun isEmpty(): Boolean {
        return mBooks.isEmpty()
    }

    inner class ViewHolder(private val binding: SubscribeResultBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {
        fun bind(foundedBook: FoundEntity) {
            binding.setVariable(BR.book, foundedBook)
            binding.executePendingBindings()
            if (foundedBook.buttonPressed) {
                binding.centerActionBtn.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.dark_gray,
                        context.theme
                    )
                )
            } else {
                binding.centerActionBtn.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.book_name_color,
                        context.theme
                    )
                )
            }
            binding.centerActionBtn.setOnClickListener {
                foundedBook.buttonPressed = true
                binding.centerActionBtn.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.dark_gray,
                        context.theme
                    )
                )
                delegate.buttonPressed(foundedBook)
            }
        }
    }

}