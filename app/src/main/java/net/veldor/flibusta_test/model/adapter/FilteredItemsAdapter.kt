package net.veldor.flibusta_test.model.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibusta_test.BR
import net.veldor.flibusta_test.databinding.FilteredListViewBinding
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.selections.opds.FoundEntity

class FilteredItemsAdapter(
    private val values: ArrayList<FoundEntity>,
    val context: Context
) :
    RecyclerView.Adapter<FilteredItemsAdapter.ViewHolder>() {


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

    fun requireUpdate() {
        Log.d("surprise", "requireUpdate: i updating this list of ${values.size}")
        notifyDataSetChanged()
    }


    inner class ViewHolder(private val binding: FilteredListViewBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {
        fun bind(item: FoundEntity) {
            binding.setVariable(BR.item, item)
            binding.executePendingBindings()
            binding.filterReason.text = item.filterResult?.toString()
            // скрою элементы как в основном вью
            binding.genreName.visibility =
                if (PreferencesHandler.instance.showFoundBookGenres) View.VISIBLE else View.GONE

            binding.sequenceName.visibility =
                if (PreferencesHandler.instance.showFoundBookSequences) View.VISIBLE else View.GONE

            binding.authorName.visibility =
                if (PreferencesHandler.instance.showAuthors) View.VISIBLE else View.GONE

            binding.translatorName.visibility =
                if (PreferencesHandler.instance.showFoundBookTranslators) View.VISIBLE else View.GONE
        }
    }
}