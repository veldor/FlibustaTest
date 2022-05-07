package net.veldor.flibusta_test.model.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.BR
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FoundItemLightBinding
import net.veldor.flibusta_test.model.db.entity.DownloadedBooks
import net.veldor.flibusta_test.model.delegate.FoundItemActionDelegate
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.handler.SelectedSortTypeHandler
import net.veldor.flibusta_test.model.handler.SortHandler
import net.veldor.flibusta_test.model.interfaces.MyAdapterInterface
import net.veldor.flibusta_test.model.parser.OpdsParser
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_AUTHOR
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_AUTHORS
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_BOOK
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_GENRE
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_SEQUENCE
import net.veldor.flibusta_test.model.selections.opds.FoundEntity
import kotlin.coroutines.CoroutineContext

class FoundItemCompactAdapter(
    arrayList: ArrayList<FoundEntity>,
    val delegate: FoundItemActionDelegate,
    val context: Context
) :
    CoroutineScope,
    RecyclerView.Adapter<FoundItemCompactAdapter.ViewHolder>(), MyAdapterInterface {

    private var loadInProgress: Boolean = false
    private var lastSortOption: Int = -1
    private var hasNext: Boolean = false
    private var job: Job = Job()
    private var centerItemPressed: FoundEntity? = null
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    private var values: ArrayList<FoundEntity> = arrayListOf()


    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(context)

    override fun getResultsSize(): Int {
        return values.size
    }

    override fun setNextPageLink(link: String?) {
        hasNext = link != null
    }

    override fun setLoadInProgress(state: Boolean) {
        loadInProgress = state
        // notify last element changed
        notifyItemChanged(itemCount - 1)
    }

    override fun sort() {
        if (values.isNotEmpty()) {
            SortHandler().sortItems(values)
            notifyItemRangeChanged(0, values.size)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = FoundItemLightBinding.inflate(
            mLayoutInflater, viewGroup, false
        )
        return ViewHolder(binding)
    }

    override fun getItemId(position: Int): Long {
        if (values.size > position) {
            return values[position].itemId
        }
        return -1
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        if (i < values.size) {
            viewHolder.bind(values[i])
        } else {
            if (hasNext) {
                viewHolder.bindButton()
            }
        }
    }

    override fun getItemCount(): Int {
        if (hasNext) {
            return values.size + 1
        }
        return values.size
    }

    override fun clearList() {
        notifyItemRangeRemoved(0, itemCount)
        hasNext = false
        values = ArrayList()
        notifyItemRangeInserted(0, 0)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun appendContent(results: ArrayList<FoundEntity>) {
        lastSortOption = -1
        val oldLength = itemCount
        values.addAll(results)
        if (oldLength > 0) {
            if (SelectedSortTypeHandler.instance.sortRequired(values)) {
                sort()
            } else {
                notifyItemRangeInserted(oldLength, results.size)
            }
        } else {
            notifyDataSetChanged()
        }
    }

    inner class ViewHolder(private val binding: FoundItemLightBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {

        private lateinit var item: FoundEntity

        fun bind(item: FoundEntity) {
            this.item = item
            binding.setVariable(BR.item, item)
            binding.executePendingBindings()
            binding.name.visibility = View.VISIBLE
            binding.centerActionBtn.setOnClickListener {
                centerItemPressed = binding.item
                delegate.buttonPressed(item)
            }

            if (item.selected) {
                binding.rootView.setBackgroundColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.selected_item_background,
                        context.theme
                    )
                )
            } else {
                binding.root.background =
                    ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.genre_layout,
                        context.theme
                    )
            }

            if (item.type == TYPE_BOOK) {
                binding.name.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.book_name_color,
                        context.theme
                    )
                )
                binding.centerActionBtn.text = context.getString(R.string.download_message)
                if(item.downloadLinks.isEmpty()){
                    binding.centerActionBtn.visibility = View.INVISIBLE
                }
                else{
                    binding.centerActionBtn.visibility = View.VISIBLE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    binding.rootView.foreground = null
                }
            } else {
                if (item.content.isNotEmpty()) {
                    binding.author.visibility = View.VISIBLE
                    binding.author.text = item.content
                } else {
                    binding.author.visibility = View.GONE
                }
                if (PreferencesHandler.instance.hideOpdsResultsButtons) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        binding.rootView.foreground = with(TypedValue()) {
                            context.theme.resolveAttribute(
                                R.attr.selectableItemBackground, this, true
                            )
                            ContextCompat.getDrawable(context, resourceId)
                        }
                    }
                    binding.rootView.setOnClickListener {
                        centerItemPressed = binding.item
                        delegate.itemPressed(item)
                    }
                    binding.centerActionBtn.visibility = View.GONE
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        binding.rootView.foreground = null
                    }
                    binding.centerActionBtn.visibility = View.VISIBLE
                    binding.centerActionBtn.text = App.instance.getString(R.string.show_message)
                }
                when (item.type) {
                    TYPE_AUTHORS, TYPE_AUTHOR -> {
                        binding.name.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.author_text_color,
                                context.theme
                            )
                        )
                    }
                    TYPE_GENRE -> {
                        binding.name.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.genre_text_color,
                                context.theme
                            )
                        )
                    }
                    TYPE_SEQUENCE -> {
                        binding.name.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.sequences_text_color,
                                context.theme
                            )
                        )
                    }
                }
            }
        }

        fun bindButton() {
            // hide all useless
            binding.name.visibility = View.GONE
            binding.author.visibility = View.GONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                binding.rootView.foreground = null
            }
            if (PreferencesHandler.instance.isDisplayPagerButton && !loadInProgress) {
                binding.name.text = null
                binding.centerActionBtn.text = App.instance.getString(R.string.show_more_title)
                binding.centerActionBtn.visibility = View.VISIBLE
                binding.centerActionBtn.setOnClickListener {
                    binding.name.text =
                        App.instance.applicationContext.getString(R.string.loading_more_title)
                    binding.centerActionBtn.visibility = View.GONE
                    delegate.loadMoreBtnClicked()
                }
            } else {
                binding.name.text =
                    App.instance.applicationContext.getString(R.string.loading_more_title)
                binding.centerActionBtn.visibility = View.GONE
            }
        }
    }

    override fun getList(): ArrayList<FoundEntity> {
        return values
    }

    override fun getClickedItemId(): Long {
        if (centerItemPressed != null) {
            return centerItemPressed!!.itemId
        }
        return -1
    }

    override fun notEmpty(): Boolean {
        return values.isNotEmpty()
    }


    init {
        if (arrayList.size > 0) {
            Log.d("surprise", "i have books on start: ${arrayList.size}")
            values = arrayList
        }
    }

    override fun setHasNext(isNext: Boolean) {
        hasNext = isNext
    }

    override fun markClickedElement(clickedElementIndex: Long) {
        if (values.isNotEmpty()) {
            values.forEach {
                if (it.selected) {
                    it.selected = false
                    notifyItemChanged(getItemPositionById(it.itemId))
                }
            }
            val position = getItemPositionById(clickedElementIndex)
            if (position >= 0) {
                values[position].selected = true
                notifyItemChanged(position)
            }
        }
    }

    override fun markBookRead(item: FoundEntity) {}
    override fun markBookUnread(item: FoundEntity) {}
    override fun markAsDownloaded(item: DownloadedBooks?) {}
    override fun itemFiltered(item: FoundEntity) {
        if (values.contains(item)) {
            val num = values.indexOf(item)
            values.remove(item)
            notifyItemRemoved(num)
        }
    }

    override fun containsBooks(): Boolean {
        if (values.isNotEmpty()) {
            values.forEach {
                if (it.type == TYPE_BOOK) {
                    return true
                }
            }
        }
        return false
    }

    override fun containsAuthors(): Boolean {
        if (values.isNotEmpty()) {
            values.forEach {
                if (it.type == OpdsParser.TYPE_AUTHORS || it.type == OpdsParser.TYPE_AUTHOR) {
                    return true
                }
            }
        }
        return false
    }

    override fun containsGenres(): Boolean {
        if (values.isNotEmpty()) {
            values.forEach {
                if (it.type == OpdsParser.TYPE_GENRE) {
                    return true
                }
            }
        }
        return false
    }

    override fun containsSequences(): Boolean {
        if (values.isNotEmpty()) {
            values.forEach {
                if (it.type == OpdsParser.TYPE_SEQUENCE) {
                    return true
                }
            }
        }
        return false
    }

    override fun getItemPositionById(clickedItemId: Long): Int {
        if (values.isNotEmpty()) {
            values.forEach {
                if (it.itemId == clickedItemId) {
                    return values.indexOf(it)
                }
            }
        }
        return -1
    }
}