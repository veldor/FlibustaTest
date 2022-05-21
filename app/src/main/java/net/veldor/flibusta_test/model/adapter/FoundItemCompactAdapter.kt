package net.veldor.flibusta_test.model.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

    private var useFilter: Boolean = false
        set(state) {
            if (state) {
                originValues = resultValues
                _size.postValue(originValues.size)
            } else {
                if (originValues.isNotEmpty()) {
                    resultValues = originValues
                    _size.postValue(resultValues.size)
                    notifyItemRangeChanged(0, originValues.size - 1)
                }
            }
            field = state
        }

    private var selectedFilterOption: Int = 0
    private var loadInProgress: Boolean = false
    private var lastSortOption: Int = -1
    private var hasNext: Boolean = false
    private var job: Job = Job()
    private var centerItemPressed: FoundEntity? = null
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    private var resultValues: ArrayList<FoundEntity> = arrayListOf()
    private var originValues: ArrayList<FoundEntity> = arrayListOf()


    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(context)

    override fun getResultsSize(): Int {
        return resultValues.size
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
        if (resultValues.isNotEmpty()) {
            SortHandler().sortItems(resultValues)
            notifyItemRangeChanged(0, resultValues.size)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = FoundItemLightBinding.inflate(
            mLayoutInflater, viewGroup, false
        )
        return ViewHolder(binding)
    }

    override fun getItemId(position: Int): Long {
        if (resultValues.size > position) {
            return resultValues[position].itemId
        }
        return -1
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        if (i < resultValues.size) {
            viewHolder.bind(resultValues[i])
        } else {
            if (hasNext) {
                viewHolder.bindButton()
            }
        }
    }

    override fun getItemCount(): Int {
        if (hasNext) {
            return resultValues.size + 1
        }
        return resultValues.size
    }

    private val _size: MutableLiveData<Int> = MutableLiveData(0)
    override val liveSize: LiveData<Int> = _size

    override fun clearList() {
        notifyItemRangeRemoved(0, itemCount)
        hasNext = false
        resultValues = ArrayList()
        notifyItemRangeInserted(0, 0)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun appendContent(results: ArrayList<FoundEntity>) {
        lastSortOption = -1
        val oldLength = itemCount
        resultValues.addAll(results)
        if (oldLength > 0) {
            if (SelectedSortTypeHandler.instance.sortRequired(resultValues)) {
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
                if (item.downloadLinks.isEmpty()) {
                    binding.centerActionBtn.visibility = View.INVISIBLE
                } else {
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
        return resultValues
    }

    override fun getClickedItemId(): Long {
        if (centerItemPressed != null) {
            return centerItemPressed!!.itemId
        }
        return -1
    }

    override fun notEmpty(): Boolean {
        return resultValues.isNotEmpty()
    }


    init {
        if (arrayList.size > 0) {
            Log.d("surprise", "i have books on start: ${arrayList.size}")
            resultValues = arrayList
        }
    }

    override fun setHasNext(isNext: Boolean) {
        hasNext = isNext
    }

    override fun markClickedElement(clickedElementIndex: Long) {
        if (resultValues.isNotEmpty()) {
            resultValues.forEach {
                if (it.selected) {
                    it.selected = false
                    notifyItemChanged(getItemPositionById(it.itemId))
                }
            }
            val position = getItemPositionById(clickedElementIndex)
            if (position >= 0) {
                resultValues[position].selected = true
                notifyItemChanged(position)
            }
        }
    }

    override fun markBookRead(item: FoundEntity) {}
    override fun markBookUnread(item: FoundEntity) {}
    override fun markAsDownloaded(item: DownloadedBooks?) {}
    override fun itemFiltered(item: FoundEntity) {
        if (resultValues.contains(item)) {
            val num = resultValues.indexOf(item)
            resultValues.remove(item)
            notifyItemRemoved(num)
        }
    }

    override fun setFilterEnabled(state: Boolean) {
        useFilter = state
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString() ?: ""
                resultValues = if (charString.isEmpty()) {
                    originValues
                } else {
                    val filteredList = ArrayList<FoundEntity>()
                    originValues
                        .filter {
                            when (selectedFilterOption) {
                                R.id.filterName -> {
                                    (it.name?.lowercase()?.contains(constraint!!) == true)
                                }
                                R.id.filterAuthor -> {
                                    (it.author?.lowercase()?.contains(constraint!!) == true)
                                }
                                R.id.filterGenre -> {
                                    (it.genreComplex?.lowercase()?.contains(constraint!!) == true)
                                }
                                R.id.filterSequence -> {
                                    (it.sequencesComplex.lowercase().contains(constraint!!))
                                }
                                else -> {
                                    (it.translate?.lowercase()?.contains(constraint!!) == true)
                                }
                            }
                        }
                        .forEach { filteredList.add(it) }
                    filteredList
                }
                return FilterResults().apply { values = resultValues }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                resultValues = if (results?.values == null)
                    ArrayList()
                else {
                    val r = results.values as ArrayList<*>
                    val result: ArrayList<FoundEntity> = arrayListOf()
                    r.forEach {
                        if (it is FoundEntity) {
                            result.add(it)
                        }
                    }
                    _size.postValue(result.size)
                    result
                }
                notifyDataSetChanged()
            }
        }
    }

    override fun containsBooks(): Boolean {
        if (resultValues.isNotEmpty()) {
            resultValues.forEach {
                if (it.type == TYPE_BOOK) {
                    return true
                }
            }
        }
        return false
    }

    override fun containsAuthors(): Boolean {
        if (resultValues.isNotEmpty()) {
            resultValues.forEach {
                if (it.type == TYPE_AUTHORS || it.type == TYPE_AUTHOR) {
                    return true
                }
            }
        }
        return false
    }

    override fun containsGenres(): Boolean {
        if (resultValues.isNotEmpty()) {
            resultValues.forEach {
                if (it.type == TYPE_GENRE) {
                    return true
                }
            }
        }
        return false
    }

    override fun containsSequences(): Boolean {
        if (resultValues.isNotEmpty()) {
            resultValues.forEach {
                if (it.type == TYPE_SEQUENCE) {
                    return true
                }
            }
        }
        return false
    }

    override fun getItemPositionById(clickedItemId: Long): Int {
        if (resultValues.isNotEmpty()) {
            resultValues.forEach {
                if (it.itemId == clickedItemId) {
                    return resultValues.indexOf(it)
                }
            }
        }
        return -1
    }

    override fun setFilterSelection(selected: Int) {
        selectedFilterOption = selected
    }

    override fun filterEnabled(): Boolean {
        return useFilter
    }
}