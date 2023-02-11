package net.veldor.flibusta_test.model.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibusta_test.BR
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FilteredListItemBinding
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.selection.FoundEntity

class FilteredItemsAdapter(
    private val values: ArrayList<FoundEntity>,
    val context: Context
) :
    RecyclerView.Adapter<FilteredItemsAdapter.ViewHolder>() {


    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(context)


    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = FilteredListItemBinding.inflate(
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

    @SuppressLint("NotifyDataSetChanged")
    fun requireUpdate() {
        Log.d("surprise", "requireUpdate: i updating this list of ${values.size}")
        notifyDataSetChanged()
    }


    inner class ViewHolder(private val binding: FilteredListItemBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {

        init {
            if (PreferencesHandler.isEInk) {

                binding.bookName.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.invertable_black,
                        context.theme
                    )
                )
                binding.authorName.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.invertable_black,
                        context.theme
                    )
                )
                binding.translatorName.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.invertable_black,
                        context.theme
                    )
                )
                binding.genreName.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.einkTextColor,
                        context.theme
                    )
                )
                binding.sequenceName.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.einkTextColor,
                        context.theme
                    )
                )
            }
        }
        fun bind(item: FoundEntity) {
            binding.setVariable(BR.item, item)
            binding.executePendingBindings()
            binding.filterReason.text = item.filterResult?.toString()
            // скрою элементы как в основном вью
            binding.genreName.visibility =
                if (PreferencesHandler.showFoundBookGenres) View.VISIBLE else View.GONE

            binding.sequenceName.visibility =
                if (PreferencesHandler.showFoundBookSequences) View.VISIBLE else View.GONE

            binding.authorName.visibility =
                if (PreferencesHandler.showAuthors) View.VISIBLE else View.GONE

            binding.translatorName.visibility =
                if (PreferencesHandler.showFoundBookTranslators) View.VISIBLE else View.GONE
        }
    }
}