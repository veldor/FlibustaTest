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
import android.widget.Filter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LiveData
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
import net.veldor.flibusta_test.model.interfaces.MyAdapterInterface
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_AUTHOR
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_AUTHORS
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_BOOK
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_GENRE
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_SEQUENCE
import net.veldor.flibusta_test.model.selections.opds.FoundEntity
import net.veldor.flibusta_test.model.selections.opds.SearchResult
import kotlin.coroutines.CoroutineContext

class FoundItemAdapter(
    arrayList: ArrayList<FoundEntity>,
    val delegate: FoundItemActionDelegate,
    val context: Context
) :
    CoroutineScope,
    RecyclerView.Adapter<FoundItemAdapter.ViewHolder>(), MyAdapterInterface {

    private val _size: MutableLiveData<Int> = MutableLiveData(0)
    override val liveSize: LiveData<Int> = _size

    private var selectedFilterOption: Int = R.id.filterName
    private var useFilter: Boolean = false
        set(state) {
            if (state) {
                // use filter
                originValues = resultValues
                _size.postValue(originValues.size)
            } else {
                // use normal list
                if (originValues.isNotEmpty()) {
                    resultValues = originValues
                    _size.postValue(resultValues.size)
                    notifyItemRangeChanged(0, originValues.size - 1)
                }
            }
            field = state
        }


    private var loadInProgress: Boolean = false
    private var lastSortOption: Int = -1
    private var hasNext: Boolean = false
    private var job: Job = Job()
    private var menuClicked: FoundEntity? = null
    private var itemInAction: FoundEntity? = null
    private var showCheckboxes = false
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private var resultValues: ArrayList<FoundEntity> = arrayListOf()
    private var originValues: ArrayList<FoundEntity> = arrayListOf()

    override fun setNextPageLink(link: String?) {
        hasNext = link != null
    }

    override fun sort() {
        if (resultValues.isNotEmpty()) {
            SortHandler().sortItems(resultValues)
            notifyItemRangeChanged(0, resultValues.size)
        }
    }

    override fun setLoadInProgress(state: Boolean) {
        loadInProgress = state
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

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.timer?.cancel()
    }

    override fun getItemCount(): Int {
        if (hasNext) {
            return resultValues.size + 1
        }
        return resultValues.size
    }

    override fun clearList() {
        notifyItemRangeRemoved(0, resultValues.size)
        resultValues = arrayListOf()
        notifyItemRangeInserted(0, 0)
        _size.postValue(0)
        hasNext = false
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun appendContent(results: ArrayList<FoundEntity>) {
        showCheckboxes = false
        lastSortOption = -1
        val oldLength = itemCount
        resultValues.addAll(results)
        _size.postValue(resultValues.size)
        if (oldLength > 0) {
            notifyItemRangeInserted(itemCount, results.size)
        } else {
            notifyDataSetChanged()
        }
    }

    inner class ViewHolder(private val binding: FoundItemBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {
        var timer: CountDownTimer? = null

        init {
            binding.menuButton.setOnClickListener {
                menuClicked = binding.item
                delegate.menuItemPressed(item, binding.menuButton)
            }

            if (PreferencesHandler.instance.isEInk) {
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
                binding.thirdBlockCenterElement.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.invertable_black,
                        context.theme
                    )
                )
                binding.thirdBlocRightElement.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.invertable_black,
                        context.theme
                    )
                )
                binding.thirdBlockLeftElement.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.invertable_black,
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
                binding.rootView.setBackgroundColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.white,
                        context.theme
                    )
                )
            }
        }

        private lateinit var item: FoundEntity

        init {
            binding.previewImage.setOnClickListener {
                delegate.imageClicked(item)
            }
        }

        fun bind(item: FoundEntity) {
            this.item = item
            binding.setVariable(BR.item, item)
            binding.executePendingBindings()
            drawTypical()
            // handle book button
            if (item.type == TYPE_BOOK) {
                drawBook()
            } else {
                drawNoBook()
            }
        }

        private fun drawTypical() {
            binding.centerActionBtn.setOnClickListener {
                itemInAction = binding.item
                delegate.buttonPressed(item)
                item.buttonPressed = true
                if (!PreferencesHandler.instance.isEInk)
                    binding.centerActionBtn.setTextColor(
                        ResourcesCompat.getColor(
                            context.resources,
                            R.color.dark_gray,
                            context.theme
                        )
                    )
            }

            // handle selected item
            if (item.selected) {
                Log.d("surprise", "FoundItemAdapter.kt 284: mark selected")
                binding.rootView.setBackgroundColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.selected_item_background,
                        context.theme
                    )
                )
            } else if (!PreferencesHandler.instance.isEInk) {
                binding.root.background =
                    ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.genre_layout,
                        context.theme
                    )
            }
            // handle button pressed
            if (item.buttonPressed) {
                if (!PreferencesHandler.instance.isEInk)
                    binding.centerActionBtn.setTextColor(
                        ResourcesCompat.getColor(
                            context.resources,
                            R.color.book_name_color,
                            context.theme
                        )
                    )
            } else {
                if (!PreferencesHandler.instance.isEInk)
                    binding.centerActionBtn.setTextColor(
                        ResourcesCompat.getColor(
                            context.resources,
                            R.color.dark_gray,
                            context.theme
                        )
                    )
            }
            // setup main button
            binding.centerActionBtn.setOnClickListener {
                itemInAction = binding.item
                delegate.buttonPressed(item)
                item.buttonPressed = true
                if (!PreferencesHandler.instance.isEInk)
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
            if (!PreferencesHandler.instance.isEInk)
                binding.root.setBackgroundColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.background_color,
                        context.theme
                    )
                )
            if (item.downloadLinks.isEmpty()) {
                binding.centerActionBtn.visibility = View.INVISIBLE
            } else {
                binding.centerActionBtn.visibility = View.VISIBLE
            }
            binding.firstInfoBlockRightParam.visibility = View.VISIBLE
            binding.firstInfoBlockLeftParam.visibility = View.VISIBLE
            binding.secondInfoBlockRightParam.visibility = View.VISIBLE
            binding.secondInfoBlockLeftParam.visibility = View.VISIBLE
            binding.thirdBlocRightElement.visibility = View.VISIBLE
            binding.thirdBlockLeftElement.visibility = View.VISIBLE
            binding.thirdBlockCenterElement.visibility = View.VISIBLE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                binding.rootView.foreground = null
            }
            binding.rootView.setOnClickListener {}
            binding.name.visibility = View.VISIBLE
            if (!PreferencesHandler.instance.isEInk) {
                binding.name.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.book_name_color,
                        context.theme
                    )
                )
            }
            binding.name.isClickable = true
            makeSelectable(binding.name)
            binding.name.setOnClickListener {
                delegate.nameClicked(item)
            }
            binding.menuButton.visibility = View.GONE
            binding.menuButton.setOnClickListener {
                delegate.menuItemPressed(item, binding.menuButton)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    it.showContextMenu(it.pivotX, it.pivotY)
                } else {
                    it.showContextMenu()
                }
            }

            if (PreferencesHandler.instance.showCovers) {
                if (PreferencesHandler.instance.showCoversByRequest) {
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
            // available formats
            binding.availableLinkFormats.isClickable = true
            binding.availableLinkFormats.isFocusable = true
            binding.availableLinkFormats.setTextColor(
                ResourcesCompat.getColor(
                    context.resources,
                    R.color.white,
                    null
                )
            )
            GrammarHandler.getAvailableDownloadFormats(item, binding.availableLinkFormats)
            binding.availableLinkFormats.visibility = View.VISIBLE
            binding.availableLinkFormats.setOnClickListener {
                Toast.makeText(
                    context,
                    "Это список доступных форматов. Скачать книгу можно нажав на кнопку ниже",
                    Toast.LENGTH_LONG
                ).show()
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
                binding.firstInfoBlockRightParam.text =
                    context.getString(R.string.no_translate_title)
            } else {
                binding.firstInfoBlockRightParam.text = item.translate
            }
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
            // genre
            binding.secondInfoBlockRightParam.text = item.genreComplex

            binding.thirdBlockLeftElement.text = item.format
            binding.thirdBlockLeftElement.visibility = View.VISIBLE
            binding.thirdBlockCenterElement.text = item.downloadsCount
            binding.thirdBlockCenterElement.visibility = View.VISIBLE
            binding.thirdBlocRightElement.text = item.size
            binding.thirdBlocRightElement.visibility = View.VISIBLE
            if (!PreferencesHandler.instance.isEInk)
                binding.centerActionBtn.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.book_name_color,
                        context.theme
                    )
                )
            binding.leftActionBtn.visibility = View.VISIBLE
            binding.rightActionBtn.visibility = View.VISIBLE
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
            binding.leftActionBtn.setOnClickListener {
                delegate.leftButtonPressed(item)
            }

            if (PreferencesHandler.instance.addFilterByLongClick) {
                makeSelectable(binding.firstInfoBlockLeftParam)
                binding.firstInfoBlockLeftParam.setOnLongClickListener {
                    delegate.buttonLongPressed(item, "author")
                    return@setOnLongClickListener true
                }
                makeSelectable(binding.secondInfoBlockLeftParam)
                binding.secondInfoBlockLeftParam.setOnLongClickListener {
                    delegate.buttonLongPressed(item, "sequence")
                    return@setOnLongClickListener true
                }
                makeSelectable(binding.secondInfoBlockRightParam)
                binding.secondInfoBlockRightParam.setOnLongClickListener {
                    delegate.buttonLongPressed(item, "genre")
                    return@setOnLongClickListener true
                }
            }
        }

        private fun makeSelectable(view: View) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                view.foreground = with(TypedValue()) {
                    context.theme.resolveAttribute(
                        R.attr.selectableItemBackground, this, true
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

        private fun drawNoBook() {
            // hide all useless
            binding.name.visibility = View.VISIBLE
            binding.name.isClickable = false
            binding.name.isFocusable = false
            if (PreferencesHandler.instance.addFilterByLongClick) {
                makeSelectable(binding.name)
                binding.name.setOnLongClickListener {
                    delegate.buttonLongPressed(item, "name")
                    return@setOnLongClickListener true
                }
                binding.name.setOnClickListener { delegate.itemPressed(item) }
            } else {
                makeNoSelectable(binding.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                binding.rootView.foreground = null
            }
            if (!PreferencesHandler.instance.isEInk) {
                when (item.type) {
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
                    TYPE_AUTHOR, TYPE_AUTHORS -> {
                        binding.name.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.author_text_color,
                                context.theme
                            )
                        )
                    }
                }
            }

            if (item.content.isNotEmpty()) {
                binding.availableLinkFormats.isClickable = false
                binding.availableLinkFormats.isFocusable = true
                binding.availableLinkFormats.text = item.content
                binding.availableLinkFormats.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.textColor,
                        null
                    )
                )
                binding.availableLinkFormats.visibility = View.VISIBLE
            } else {
                binding.availableLinkFormats.visibility = View.GONE
            }
            binding.menuButton.visibility = View.GONE
            binding.previewImage.visibility = View.GONE
            binding.firstInfoBlockRightParam.visibility = View.GONE
            binding.firstInfoBlockLeftParam.visibility = View.GONE
            binding.secondInfoBlockRightParam.visibility = View.GONE
            binding.secondInfoBlockLeftParam.visibility = View.GONE
            binding.thirdBlocRightElement.visibility = View.GONE
            binding.thirdBlockLeftElement.visibility = View.GONE
            binding.thirdBlockCenterElement.visibility = View.GONE
            binding.leftActionBtn.visibility = View.GONE
            binding.rightActionBtn.visibility = View.GONE

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
                    itemInAction = binding.item
                    delegate.itemPressed(item)
                }
                binding.centerActionBtn.visibility = View.GONE
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    binding.rootView.foreground = null
                }
                binding.centerActionBtn.visibility = View.VISIBLE
                binding.centerActionBtn.text = context.getString(R.string.show_message)
            }
            binding.leftActionBtn.visibility = View.GONE
        }

        fun bindButton() {
            // hide all useless
            binding.name.visibility = View.GONE
            binding.menuButton.visibility = View.GONE
            binding.previewImage.visibility = View.GONE
            binding.firstInfoBlockRightParam.visibility = View.GONE
            binding.firstInfoBlockLeftParam.visibility = View.GONE
            binding.secondInfoBlockRightParam.visibility = View.GONE
            binding.secondInfoBlockLeftParam.visibility = View.GONE
            binding.thirdBlocRightElement.visibility = View.GONE
            binding.thirdBlockCenterElement.visibility = View.GONE
            binding.thirdBlockLeftElement.visibility = View.GONE
            binding.availableLinkFormats.visibility = View.GONE
            binding.leftActionBtn.visibility = View.GONE
            binding.rightActionBtn.visibility = View.GONE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                binding.rootView.foreground = null
            }

            if (PreferencesHandler.instance.isDisplayPagerButton && !loadInProgress) {
                binding.name.text = null
                binding.centerActionBtn.text = context.getString(R.string.show_more_title)
                binding.centerActionBtn.visibility = View.VISIBLE
                binding.centerActionBtn.setOnClickListener {
                    binding.name.text =
                        context.getString(R.string.loading_more_title)
                    binding.centerActionBtn.visibility = View.GONE
                    delegate.loadMoreBtnClicked()
                }
            } else {
                binding.name.text =
                    context.getString(R.string.loading_more_title)
                binding.centerActionBtn.visibility = View.GONE
            }
        }
    }

    override fun getList(): ArrayList<FoundEntity> {
        return resultValues
    }


    override fun markBookRead(item: FoundEntity) {
        var position: Int = -1
        resultValues.forEach {
            if (it.id == item.id) {
                position = resultValues.lastIndexOf(it)
            }
        }
        if (position >= 0 && resultValues.size > position) {
            if (PreferencesHandler.instance.isOpdsUseFilter && PreferencesHandler.instance.isHideRead) {
                resultValues.removeAt(position)
                _size.postValue(resultValues.size)
                notifyItemRemoved(position)
            } else {
                resultValues[position].read = true
                notifyItemChanged(position)
            }
        }
    }

    override fun markAsDownloaded(item: FoundEntity) {
        var position: Int = -1
        resultValues.forEach {
            if (it.id == item.id) {
                position = resultValues.lastIndexOf(it)
            }
        }
        if (position >= 0 && resultValues.size > position) {
            if (PreferencesHandler.instance.isOpdsUseFilter && PreferencesHandler.instance.isHideDownloaded) {
                resultValues.removeAt(position)
                _size.postValue(resultValues.size)
                notifyItemRemoved(position)
            } else {
                resultValues[position].downloaded = true
                notifyItemChanged(position)
            }
        }
    }

    override fun markAsDownloaded(item: DownloadedBooks?) {
        var position: Int = -1
        resultValues.forEach {
            if (it.id == item?.bookId) {
                position = resultValues.lastIndexOf(it)
            }
        }
        if (position >= 0 && resultValues.size > position) {
            if (PreferencesHandler.instance.isOpdsUseFilter && PreferencesHandler.instance.isHideDownloaded) {
                resultValues.removeAt(position)
                _size.postValue(resultValues.size)
                notifyItemRemoved(position)
            } else {
                resultValues[position].downloaded = true
                notifyItemChanged(position)
            }
        }
    }

    override fun markBookUnread(item: FoundEntity) {
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

    override fun markAsNoDownloaded(item: FoundEntity) {
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

    override fun getClickedItemId(): Long {
        if (itemInAction != null) {
            return itemInAction!!.itemId
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
            _size.postValue(resultValues.size)
        }
    }

    override fun setHasNext(isNext: Boolean) {
        hasNext = isNext
    }

    override fun getResultsSize(): Int {
        Log.d("surprise", "FoundItemAdapter.kt 729: size is ${resultValues.size}")
        return resultValues.size
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

    override fun setFilterSelection(selected: Int) {
        selectedFilterOption = selected
    }

    override fun filterEnabled(): Boolean {
        return useFilter
    }

    override fun reapplyFilters(r: SearchResult) {
        val iterator = r.results.iterator()
        var node: FoundEntity
        while (iterator.hasNext()) {
            node = iterator.next()
            val checkResult = FilterHandler.check(node)
            if (!checkResult.result) {
                Log.d("surprise", "FoundItemAdapter.kt 1003: item filtered!!")
                iterator.remove()
                node.filterResult = checkResult
                r.filteredList.add(node)
            }
        }
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
}