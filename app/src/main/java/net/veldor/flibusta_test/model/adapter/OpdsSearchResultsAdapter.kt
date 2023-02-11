package net.veldor.flibusta_test.model.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Filter
import android.widget.Filterable
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import net.veldor.flibusta_test.BR
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FoundItemBinding
import net.veldor.flibusta_test.model.db.entity.DownloadedBooks
import net.veldor.flibusta_test.model.delegate.FoundItemActionDelegate
import net.veldor.flibusta_test.model.handler.*
import net.veldor.flibusta_test.model.helper.MimeHelper
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_AUTHOR
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_AUTHORS
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_BOOK
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_GENRE
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_SEQUENCE
import net.veldor.flibusta_test.model.selection.FoundEntity
import net.veldor.flibusta_test.model.selection.OpdsStatement
import kotlin.coroutines.CoroutineContext


class OpdsSearchResultsAdapter(
    val delegate: FoundItemActionDelegate,
    val context: Context
) :
    CoroutineScope,
    Filterable,
    RecyclerView.Adapter<OpdsSearchResultsAdapter.ViewHolder>() {

    private val resultValues = OpdsStatement.results
    private val filteredValues: ArrayList<FoundEntity> = arrayListOf()

    private var selectedFilterOption: Int = R.id.filterName
    private var useFilter: Boolean = false
        set(state) {
            if (state) {
                filteredValues.clear()
                resultValues.forEach {
                    filteredValues.add(it)
                }
                notifyItemRangeChanged(0, filteredValues.size)
                _size.postValue(filteredValues.size)
            } else {
                _size.postValue(resultValues.size)
                notifyItemRangeChanged(0, resultValues.size)
            }
            field = state
        }

    private var load = false

    var loadInProgress: Boolean
        set(value) {
            load = value
            if (value) {
                if (OpdsStatement.isNextPageLink()) {
                    // get last item, refresh it
                    notifyItemChanged(getResultsSize())
                } else {
                    notifyItemChanged(getResultsSize() - 1)
                }
            }
        }
        get() {
            return load
        }

    private var pressedItemId: String? = null

    private var job: Job = Job()
    private var itemInAction: FoundEntity? = null
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    fun sort() {
        if (useFilter) {
            if (filteredValues.isNotEmpty()) {
                SortHandler().sortItems(filteredValues)
                notifyItemRangeChanged(0, filteredValues.size)
            }
        } else {
            if (resultValues.isNotEmpty()) {
                SortHandler().sortItems(resultValues)
                notifyItemRangeChanged(0, resultValues.size)
            }
        }
    }

    private val mLayoutInflater: LayoutInflater =
        LayoutInflater.from(context)

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = FoundItemBinding.inflate(
            mLayoutInflater, viewGroup, false
        )
        return ViewHolder(binding)
    }

    override fun getItemId(position: Int): Long {
        if (useFilter) {
            if (filteredValues.size > position) {
                return filteredValues[position].itemId
            }
            return -1
        } else {
            if (resultValues.size > position) {
                return resultValues[position].itemId
            }
            return -1
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        if (useFilter) {
            if (i < filteredValues.size) {
                viewHolder.bind(filteredValues[i])
            } else {
                viewHolder.bindButton()
            }
        } else {
            if (i < resultValues.size) {
                viewHolder.bind(resultValues[i])
            } else {
                viewHolder.bindButton()
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.timer?.cancel()
    }

    override fun getItemCount(): Int {
        if (useFilter) {
            if (OpdsStatement.isNextPageLink()) {
                return filteredValues.size + 1
            }
            return filteredValues.size
        } else {
            if (OpdsStatement.isNextPageLink()) {
                return resultValues.size + 1
            }
            return resultValues.size
        }
    }

    private val _size: MutableLiveData<Int> = MutableLiveData(0)

    @SuppressLint("NotifyDataSetChanged")
    fun clearList() {
        Log.d("surprise", "OpdsSearchResultsAdapter: 147 clearing results list")
        resultValues.clear()
        filteredValues.clear()
        notifyDataSetChanged()
    }


    inner class ViewHolder(private val binding: FoundItemBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {
        var timer: CountDownTimer? = null

        init {
            binding.menuButton.setOnClickListener {
                delegate.menuItemPressed(item, binding.menuButton)
            }
            binding.previewImage.setOnClickListener {
                delegate.imageClicked(item)
            }
        }

        private lateinit var item: FoundEntity


        fun bind(item: FoundEntity) {
            this.item = item
            binding.setVariable(BR.item, item)
            binding.executePendingBindings()
            binding.centerActionBtn.isEnabled = true
            drawBase()
            if (item.type == TYPE_BOOK) {
                drawBook()
            } else {
                drawElement()
            }
            if (PreferencesHandler.isEInk) {
                paintForEInk()
            }
        }

        private fun paintForEInk() {
            binding.rootView.setCardBackgroundColor(
                ResourcesCompat.getColor(
                    context.resources,
                    R.color.white,
                    context.theme
                )
            )
            binding.mainView.setBackgroundColor(
                ResourcesCompat.getColor(
                    context.resources,
                    R.color.white,
                    context.theme
                )
            )
            binding.centerActionBtn.setTextColor(
                ResourcesCompat.getColor(
                    context.resources,
                    R.color.invertable_black,
                    context.theme
                )
            )
            binding.name.setTextColor(
                ResourcesCompat.getColor(
                    context.resources,
                    R.color.invertable_black,
                    context.theme
                )
            )
            binding.firstInfoBlockLeftParam.setTextColor(
                ResourcesCompat.getColor(
                    context.resources,
                    R.color.invertable_black,
                    context.theme
                )
            )
            binding.firstInfoBlockRightParam.setTextColor(
                ResourcesCompat.getColor(
                    context.resources,
                    R.color.invertable_black,
                    context.theme
                )
            )
            binding.secondInfoBlockLeftParam.setTextColor(
                ResourcesCompat.getColor(
                    context.resources,
                    R.color.invertable_black,
                    context.theme
                )
            )
            binding.secondInfoBlockRightParam.setTextColor(
                ResourcesCompat.getColor(
                    context.resources,
                    R.color.invertable_black,
                    context.theme
                )
            )
        }

        fun bindButton() {
            // скрою все элементы кроме главной кнопки
            hideAllExceptMainBtn()
            binding.centerActionBtn.setTextColor(
                ResourcesCompat.getColor(
                    context.resources,
                    R.color.invertable_black,
                    context.theme
                )
            )
            binding.centerActionBtn.text = context.getString(R.string.show_more_title)
            binding.centerActionBtn.setOnClickListener {
                if (!load) {
                    delegate.loadMoreBtnClicked()
                } else {
                    Log.d("surprise", "ViewHolder: 239 another load in progress")
                }
            }
        }


        private fun drawBase() {
            binding.menuButton.visibility = View.VISIBLE
            binding.availableLinkFormats.removeAllViews()
            // действие главной кнопки
            binding.centerActionBtn.setOnClickListener {
                itemInAction = binding.item
                delegate.buttonPressed(item)
                item.buttonPressed = true

                binding.centerActionBtn.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.dark_gray,
                        context.theme
                    )
                )
            }

            // handle selected no-book item
            if (pressedItemId != null && item.link == pressedItemId) {
                binding.rootView.setCardBackgroundColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.selected_item_background,
                        context.theme
                    )
                )
            } else {
                binding.rootView.setCardBackgroundColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.cardview_background,
                        context.theme
                    )
                )
            }
            // handle button pressed
            if (item.buttonPressed) {
                binding.centerActionBtn.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.book_name_color,
                        context.theme
                    )
                )
            } else {
                binding.centerActionBtn.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.dark_gray,
                        context.theme
                    )
                )
            }
        }


        private fun drawBook() {
            showBookItems()
            binding.centerActionBtn.setTextColor(
                ResourcesCompat.getColor(
                    context.resources,
                    R.color.book_name_color,
                    context.theme
                )
            )
            binding.name.setPadding(30, 10, 30, 10)
            binding.centerActionBtn.setOnLongClickListener(null)
            binding.rootView.setOnLongClickListener(null)
            binding.centerActionBtn.text = context.getString(R.string.download_message)
            if (item.downloadLinks.isEmpty()) {
                binding.centerActionBtn.visibility = View.INVISIBLE
            } else {
                binding.centerActionBtn.visibility = View.VISIBLE
            }
            binding.rootView.setOnClickListener {}
            binding.name.visibility = View.VISIBLE
            binding.name.setTextColor(
                ResourcesCompat.getColor(
                    context.resources,
                    R.color.book_name_color,
                    context.theme
                )
            )
            binding.name.isClickable = true
            makeSelectable(binding.name)
            binding.name.setOnClickListener {
                itemInAction = binding.item
                Log.d("surprise", "FoundItemAdapter.kt 373: clicked ${binding.item?.itemId}")
                delegate.nameClicked(item)
            }

            if (PreferencesHandler.showCovers) {
                if (PreferencesHandler.showCoversByRequest) {
                    binding.previewImage.visibility = View.VISIBLE
                    if (item.cover != null && item.cover!!.isFile && item.cover!!.exists() && item.cover!!.canRead()) {
                        binding.previewImage.setImageBitmap(
                            BitmapFactory.decodeFile(
                                item.cover!!.path
                            )
                        )
                        timer?.cancel()
                    } else if (item.coverUrl != null) {
                        binding.previewImage.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.image_wait_load,
                                context.theme
                            )
                        )
                        binding.previewImage.setOnClickListener {
                            // load image
                            binding.previewImage.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    context.resources,
                                    R.drawable.image_loading,
                                    context.theme
                                )
                            )
                            binding.previewImage.setOnClickListener {}
                            CoverHandler().loadPic(item)
                            timer =
                                object : CountDownTimer(30000.toLong(), 1000) {
                                    override fun onTick(millisUntilFinished: Long) {
                                        if (item.cover != null && item.cover!!.isFile && item.cover!!.exists() && item.cover!!.canRead()) {
                                            binding.previewImage.setImageBitmap(
                                                BitmapFactory.decodeFile(
                                                    item.cover!!.path
                                                )
                                            )
                                            timer?.cancel()
                                            binding.previewImage.setOnClickListener {
                                                delegate.imageClicked(item)
                                            }
                                        }
                                    }

                                    override fun onFinish() {
                                    }
                                }
                            timer?.start()
                        }
                    } else {
                        binding.previewImage.setOnClickListener {
                            delegate.imageClicked(item)
                        }
                        binding.previewImage.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.no_cover,
                                context.theme
                            )
                        )
                    }
                } else {
                    binding.previewImage.setOnClickListener {
                        delegate.imageClicked(item)
                    }
                    // гружу обложку
                    binding.previewImage.visibility = View.VISIBLE
                    if (item.coverUrl == null) {
                        binding.previewImage.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.no_cover,
                                context.theme
                            )
                        )
                    } else {
                        if (item.cover != null && item.cover!!.isFile && item.cover!!.exists() && item.cover!!.canRead()) {
                            binding.previewImage.setImageBitmap(BitmapFactory.decodeFile(item.cover!!.path))
                        } else {
                            binding.previewImage.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    context.resources,
                                    R.drawable.image_wait_load,
                                    context.theme
                                )
                            )
                            // periodic check cover loaded
                            timer =
                                object : CountDownTimer(30000.toLong(), 1000) {
                                    override fun onTick(millisUntilFinished: Long) {
                                        if (item.cover != null && item.cover!!.isFile && item.cover!!.exists() && item.cover!!.canRead()) {
                                            binding.previewImage.setImageBitmap(
                                                BitmapFactory.decodeFile(
                                                    item.cover!!.path
                                                )
                                            )
                                            timer?.cancel()
                                        }
                                    }

                                    override fun onFinish() {
                                    }
                                }
                            timer?.start()
                        }
                    }
                }

            } else {
                // скрою окно иконки
                binding.previewImage.visibility = View.GONE
            }
            if (PreferencesHandler.showFoundBookAvailableFormats) {
                binding.availableLinkFormats.removeAllViews()
                // available formats
                if (item.downloadLinks.isEmpty()) {
                    mLayoutInflater.inflate(
                        R.layout.no_download_links_view,
                        binding.availableLinkFormats,
                        true
                    )
                } else {
                    item.downloadLinks.forEach { link ->
                        val formatView = mLayoutInflater.inflate(
                            R.layout.available_format_view,
                            binding.availableLinkFormats,
                            false
                        )
                        (formatView as Button).text =
                            MimeHelper.getDownloadMime(link.mime!!)
                        GrammarHandler.colorizeFormatButton(formatView, link.mime!!)
                        binding.availableLinkFormats.addView(formatView)
                        formatView.setOnClickListener {
                            delegate.fastDownload(link)
                        }
                    }
                }
            }
            // author
            if (!item.author.isNullOrEmpty()) {
                binding.firstInfoBlockLeftParam.isClickable = true
                binding.firstInfoBlockLeftParam.text = item.author
                makeSelectable(binding.firstInfoBlockLeftParam)
                binding.firstInfoBlockLeftParam.setOnClickListener {
                    itemInAction = binding.item
                    delegate.authorClicked(item)
                }
            } else {
                binding.firstInfoBlockLeftParam.isClickable = false
                binding.firstInfoBlockLeftParam.text =
                    context.getString(R.string.author_unknown_title)
                makeNoSelectable(binding.firstInfoBlockLeftParam)
            }
            // translate
            if (item.translate.isNullOrEmpty()) {
                binding.firstInfoBlockRightParam.visibility = View.GONE
            } else {
                binding.firstInfoBlockRightParam.text = item.translate
            }
            if (PreferencesHandler.showFoundBookSequences) {
                // sequence
                if (item.sequencesComplex.isNotEmpty()) {
                    binding.secondInfoBlockLeftParam.isClickable = true
                    makeSelectable(binding.secondInfoBlockLeftParam)
                    binding.secondInfoBlockLeftParam.text = item.sequencesComplex
                    binding.secondInfoBlockLeftParam.setOnClickListener {
                        itemInAction = binding.item
                        delegate.sequenceClicked(item)
                    }
                } else {
                    binding.secondInfoBlockLeftParam.text =
                        context.getString(R.string.no_sequence_title)
                    binding.secondInfoBlockLeftParam.isClickable = false
                    binding.secondInfoBlockLeftParam.setOnClickListener {}
                    makeNoSelectable(binding.secondInfoBlockLeftParam)
                }
            }
            // genre
            if (item.genreComplex.isNullOrEmpty()) {
                binding.secondInfoBlockRightParam.visibility = View.GONE
            } else {
                binding.secondInfoBlockRightParam.text = item.genreComplex
            }

            binding.thirdBlockLeftElement.text = item.format
            binding.thirdBlockCenterElement.text = item.downloadsCount
            binding.thirdBlockRightElement.text = item.size
            binding.centerActionBtn.setTextColor(
                ResourcesCompat.getColor(
                    context.resources,
                    R.color.book_name_color,
                    context.theme
                )
            )
            if (!item.read) {
                binding.leftActionBtn.background.setColorFilter(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.dark_gray,
                        context.theme
                    ), PorterDuff.Mode.SRC_ATOP
                )
            } else {
                binding.leftActionBtn.background.setColorFilter(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.genre_text_color,
                        context.theme
                    ), PorterDuff.Mode.SRC_ATOP
                )
            }
            if (PreferencesHandler.showFoundBookDownloadBtn) {
                if (!item.downloaded) {
                    binding.rightActionBtn.background.setColorFilter(
                        ResourcesCompat.getColor(
                            context.resources,
                            R.color.dark_gray,
                            context.theme
                        ), PorterDuff.Mode.SRC_ATOP
                    )
                } else {
                    binding.rightActionBtn.background.setColorFilter(
                        ResourcesCompat.getColor(
                            context.resources,
                            R.color.genre_text_color,
                            context.theme
                        ), PorterDuff.Mode.SRC_ATOP
                    )
                }

                binding.rightActionBtn.setOnClickListener {
                    delegate.rightButtonPressed(item)
                }
            }
            binding.leftActionBtn.setOnClickListener {
                delegate.leftButtonPressed(item)
            }
            makeSelectable(binding.firstInfoBlockLeftParam)
            binding.firstInfoBlockLeftParam.setOnLongClickListener {
                delegate.buttonLongPressed(item, "author", binding.firstInfoBlockLeftParam)
                return@setOnLongClickListener true
            }
            makeSelectable(binding.secondInfoBlockLeftParam)
            binding.secondInfoBlockLeftParam.setOnLongClickListener {
                delegate.buttonLongPressed(item, "sequence", binding.secondInfoBlockLeftParam)
                return@setOnLongClickListener true
            }
            makeSelectable(binding.secondInfoBlockRightParam)
            binding.secondInfoBlockRightParam.setOnLongClickListener {
                delegate.buttonLongPressed(item, "genre", binding.secondInfoBlockRightParam)
                return@setOnLongClickListener true
            }
        }

        private fun showBookItems() {

            binding.firstInfoBlockLeftParam.visibility =
                if (PreferencesHandler.showAuthors) View.VISIBLE else View.GONE

            binding.firstInfoBlockRightParam.visibility =
                if (PreferencesHandler.showFoundBookTranslators) View.VISIBLE else View.GONE

            binding.secondInfoBlockLeftParam.visibility =
                if (PreferencesHandler.showFoundBookSequences) View.VISIBLE else View.GONE

            binding.secondInfoBlockRightParam.visibility =
                if (PreferencesHandler.showFoundBookGenres) View.VISIBLE else View.GONE

            binding.thirdBlockLeftElement.visibility =
                if (PreferencesHandler.showFoundBookFormat) View.VISIBLE else View.GONE

            binding.thirdBlockCenterElement.visibility =
                if (PreferencesHandler.showFoundBookDownloads) View.VISIBLE else View.GONE

            binding.thirdBlockRightElement.visibility =
                if (PreferencesHandler.showFoundBookSize) View.VISIBLE else View.GONE

            binding.availableLinkFormats.visibility =
                if (PreferencesHandler.showFoundBookAvailableFormats) View.VISIBLE else View.GONE

            binding.leftActionBtn.visibility =
                if (PreferencesHandler.showFoundBookReadBtn) View.VISIBLE else View.GONE

            binding.rightActionBtn.visibility =
                if (PreferencesHandler.showFoundBookDownloadBtn) View.VISIBLE else View.GONE
        }

        private fun showElementItems() {
            binding.name.visibility = View.VISIBLE
            binding.firstInfoBlockLeftParam.visibility = View.GONE
            binding.firstInfoBlockRightParam.visibility = View.GONE
            binding.secondInfoBlockLeftParam.visibility = View.GONE
            binding.secondInfoBlockRightParam.visibility = View.GONE
            binding.thirdBlockLeftElement.visibility = View.GONE
            binding.thirdBlockCenterElement.visibility = View.GONE
            binding.thirdBlockRightElement.visibility = View.GONE
            binding.availableLinkFormats.visibility = View.GONE
            binding.leftActionBtn.visibility = View.GONE
            binding.rightActionBtn.visibility = View.GONE
            binding.previewImage.visibility = View.GONE
        }


        private fun drawElement() {
            showElementItems()

            binding.rootView.setOnLongClickListener {
                delegate.buttonLongPressed(item, "name", binding.rootView)
                return@setOnLongClickListener true
            }

            if (PreferencesHandler.hideOpdsResultsButtons) {
                binding.centerActionBtn.visibility = View.GONE
                binding.name.setPadding(50, 100, 50, 100)
                binding.rootView.setOnClickListener {
                    itemInAction = binding.item
                    delegate.itemPressed(item)
                }
            } else {
                binding.centerActionBtn.setOnLongClickListener {
                    delegate.buttonLongPressed(item, "name", binding.rootView)
                    return@setOnLongClickListener true
                }
                binding.name.setPadding(30, 10, 30, 10)
                binding.centerActionBtn.visibility = View.VISIBLE
                binding.centerActionBtn.text = context.getString(R.string.show_message)
            }
            // hide all useless
            binding.name.isClickable = false
            binding.name.isFocusable = false
            if (PreferencesHandler.addFilterByLongClick) {
                makeSelectable(binding.name)
                binding.name.setOnLongClickListener {
                    delegate.buttonLongPressed(item, "name", binding.name)
                    return@setOnLongClickListener true
                }
                binding.name.setOnClickListener { delegate.itemPressed(item) }
            } else {
                makeNoSelectable(binding.name)
            }
            when (item.type) {
                TYPE_GENRE -> {
                    binding.centerActionBtn.setTextColor(
                        ResourcesCompat.getColor(
                            context.resources,
                            R.color.genre_text_color,
                            context.theme
                        )
                    )
                    binding.name.setTextColor(
                        ResourcesCompat.getColor(
                            context.resources,
                            R.color.genre_text_color,
                            context.theme
                        )
                    )
                }
                TYPE_SEQUENCE -> {
                    binding.centerActionBtn.setTextColor(
                        ResourcesCompat.getColor(
                            context.resources,
                            R.color.sequences_text_color,
                            context.theme
                        )
                    )
                    binding.name.setTextColor(
                        ResourcesCompat.getColor(
                            context.resources,
                            R.color.sequences_text_color,
                            context.theme
                        )
                    )
                }
                TYPE_AUTHOR, TYPE_AUTHORS -> {
                    binding.centerActionBtn.setTextColor(
                        ResourcesCompat.getColor(
                            context.resources,
                            R.color.author_text_color,
                            context.theme
                        )
                    )
                    binding.name.setTextColor(
                        ResourcesCompat.getColor(
                            context.resources,
                            R.color.author_text_color,
                            context.theme
                        )
                    )
                }
            }
            if (PreferencesHandler.showElementDescription) {
                if (item.content.isNotEmpty()) {
                    binding.thirdBlockCenterElement.visibility = View.VISIBLE
                    binding.thirdBlockCenterElement.text = item.content
                    binding.thirdBlockCenterElement.setTextColor(
                        ResourcesCompat.getColor(
                            context.resources,
                            R.color.textColor,
                            null
                        )
                    )
                }
            } else {
                binding.thirdBlockCenterElement.visibility = View.GONE
            }
        }

        private fun makeSelectable(view: View) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                view.foreground = with(TypedValue()) {
                    context.theme.resolveAttribute(
                        androidx.appcompat.R.attr.selectableItemBackground, this, true
                    )
                    ContextCompat.getDrawable(context, resourceId)
                }
            }
        }

        private fun makeNoSelectable(view: View) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                view.foreground = null
            }
        }

        private fun hideAllExceptMainBtn() {
            showElementItems()
            binding.menuButton.visibility = View.GONE
            binding.name.visibility = View.GONE
            binding.availableLinkFormats.visibility = View.GONE
        }
    }

    fun getList(): ArrayList<FoundEntity> {
        return resultValues
    }

    fun setFilterEnabled(state: Boolean) {
        useFilter = state
    }

    fun setFilterSelection(selected: Int) {
        selectedFilterOption = selected
    }


    fun markBookRead(item: FoundEntity) {
        var position: Int = -1
        resultValues.forEach {
            if (it.id == item.id) {
                position = resultValues.lastIndexOf(it)
            }
        }
        if (position >= 0 && resultValues.size > position) {
            if (PreferencesHandler.isOpdsUseFilter && PreferencesHandler.isHideRead) {
                resultValues.removeAt(position)
                _size.postValue(resultValues.size)
                notifyItemRemoved(position)
            } else {
                resultValues[position].read = true
                notifyItemChanged(position)
            }
        }
    }

    fun markAsDownloaded(item: FoundEntity) {
        var position: Int = -1
        resultValues.forEach {
            if (it.id == item.id) {
                position = resultValues.lastIndexOf(it)
            }
        }
        if (position >= 0 && resultValues.size > position) {
            if (PreferencesHandler.isOpdsUseFilter && PreferencesHandler.isHideDownloaded) {
                resultValues.removeAt(position)
                _size.postValue(resultValues.size)
                notifyItemRemoved(position)
            } else {
                resultValues[position].downloaded = true
                notifyItemChanged(position)
            }
        }
    }

    fun markAsDownloaded(item: DownloadedBooks?) {
        var position: Int = -1
        resultValues.forEach {
            if (it.id == item?.bookId) {
                position = resultValues.lastIndexOf(it)
            }
        }
        if (position >= 0 && resultValues.size > position) {
            if (PreferencesHandler.isOpdsUseFilter && PreferencesHandler.isHideDownloaded) {
                resultValues.removeAt(position)
                _size.postValue(resultValues.size)
                notifyItemRemoved(position)
            } else {
                resultValues[position].downloaded = true
                notifyItemChanged(position)
            }
        }
    }

    fun markBookUnread(item: FoundEntity) {
        var position: Int = -1
        resultValues.forEach {
            if (it.id == item.id) {
                position = resultValues.lastIndexOf(it)
            }
        }
        if (position >= 0 && resultValues.size > position) {
            resultValues[position].read = false
            notifyItemChanged(position)
        }
    }

    fun markAsNoDownloaded(item: FoundEntity) {
        var position: Int = -1
        resultValues.forEach {
            if (it.id == item.id) {
                position = resultValues.lastIndexOf(it)
            }
        }
        if (position >= 0 && resultValues.size > position) {
            resultValues[position].downloaded = false
            notifyItemChanged(position)
        }
    }

    private fun getResultsSize(): Int {
        return resultValues.size
    }

    fun containsBooks(): Boolean {
        if (resultValues.isNotEmpty()) {
            resultValues.forEach {
                if (it.type == TYPE_BOOK) {
                    return true
                }
            }
        }
        return false
    }

    fun containsAuthors(): Boolean {
        if (resultValues.isNotEmpty()) {
            resultValues.forEach {
                if (it.type == TYPE_AUTHORS || it.type == TYPE_AUTHOR) {
                    return true
                }
            }
        }
        return false
    }

    fun containsGenres(): Boolean {
        if (resultValues.isNotEmpty()) {
            resultValues.forEach {
                if (it.type == TYPE_GENRE) {
                    return true
                }
            }
        }
        return false
    }

    fun containsSequences(): Boolean {
        if (resultValues.isNotEmpty()) {
            resultValues.forEach {
                if (it.type == TYPE_SEQUENCE) {
                    return true
                }
            }
        }
        return false
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                filteredValues.clear()
                val charString = constraint?.toString() ?: ""
                if (charString.isEmpty()) {
                    resultValues.forEach {
                        filteredValues.add(it)
                    }
                    return FilterResults().apply { values = resultValues }
                }
                resultValues
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
                    .forEach { filteredValues.add(it) }
                return FilterResults().apply { values = filteredValues }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
                _size.postValue(results?.count)
            }
        }
    }

    fun addItem(item: FoundEntity?): Int {
        // проверю, не фильтруется ли контент
        if (item != null) {
            resultValues.add(item)
            notifyItemInserted(resultValues.indexOf(item))
            if (pressedItemId != null && item.link == pressedItemId) {
                Log.d("surprise", "scrollToPressed: search for scroll to $pressedItemId")
                // scroll to current item
                delegate.scrollTo(resultValues.indexOf(item))
            }
        }
        return resultValues.size
    }

    fun setPressedId(pressedItemId: String?) {
        if (pressedItemId != null) {
            this.pressedItemId = pressedItemId
        }
    }

    fun scrollToPressed() {
        if (pressedItemId != null) {
            Log.d("surprise", "scrollToPressed: search for scroll to $pressedItemId")
            resultValues.forEach {
                if (it.link == pressedItemId) {
                    delegate.scrollTo(resultValues.indexOf(it))
                }
            }
        }
    }

    fun hide(item: FoundEntity) {
        var position: Int = -1
        resultValues.forEach {
            if (it.link == item.link) {
                position = resultValues.lastIndexOf(it)
            }
        }
        if (position >= 0 && resultValues.size > position) {
            resultValues.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}