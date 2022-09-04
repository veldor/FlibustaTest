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
import net.veldor.flibusta_test.model.selections.OpdsStatement
import net.veldor.flibusta_test.model.selections.opds.FoundEntity
import net.veldor.flibusta_test.model.selections.opds.SearchResult
import kotlin.coroutines.CoroutineContext


class NewFoundItemAdapter(
    private val resultValues: ArrayList<FoundEntity>,
    val delegate: FoundItemActionDelegate,
    val context: Context
) :
    CoroutineScope,
    RecyclerView.Adapter<NewFoundItemAdapter.ViewHolder>(), MyAdapterInterface {


    private var pressedItemId: String? = null

    private var job: Job = Job()
    private var itemInAction: FoundEntity? = null
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun setNextPageLink(link: String?) {
    }

    override fun sort() {
        if (resultValues.isNotEmpty()) {
            SortHandler().sortItems(resultValues)
            notifyItemRangeChanged(0, resultValues.size)
        }
    }

    override fun setLoadInProgress(state: Boolean) {
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
            viewHolder.bindButton()
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.timer?.cancel()
    }

    override fun getItemCount(): Int {
        if (OpdsStatement.instance.isNextPageLink()) {
            return resultValues.size + 1
        }
        return resultValues.size
    }

    private val _size: MutableLiveData<Int> = MutableLiveData(0)
    override val liveSize: LiveData<Int> = _size

    @SuppressLint("NotifyDataSetChanged")
    override fun clearList() {
        resultValues.clear()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun appendContent(results: ArrayList<FoundEntity>) {
    }


    override fun loadPreviousResults(results: java.util.ArrayList<FoundEntity>) {

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
            drawBase()
            if (item.type == TYPE_BOOK) {
                drawBook()
            } else {
                drawElement()
            }
            if (PreferencesHandler.instance.isEInk) {
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
                delegate.loadMoreBtnClicked()
            }
        }


        private fun drawBase() {
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
            if (PreferencesHandler.instance.showFoundBookAvailableFormats) {
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
                binding.availableLinkFormats.setOnClickListener {
                    Toast.makeText(
                        context,
                        "Это список доступных форматов. Скачать книгу можно нажав на кнопку ниже",
                        Toast.LENGTH_LONG
                    ).show()
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
            if (PreferencesHandler.instance.showFoundBookSequences) {
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
            if (PreferencesHandler.instance.showFoundBookDownloadBtn) {
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
            binding.menuButton.visibility = View.VISIBLE

            binding.firstInfoBlockLeftParam.visibility =
                if (PreferencesHandler.instance.showAuthors) View.VISIBLE else View.GONE

            binding.firstInfoBlockRightParam.visibility =
                if (PreferencesHandler.instance.showFoundBookTranslators) View.VISIBLE else View.GONE

            binding.secondInfoBlockLeftParam.visibility =
                if (PreferencesHandler.instance.showFoundBookSequences) View.VISIBLE else View.GONE

            binding.secondInfoBlockRightParam.visibility =
                if (PreferencesHandler.instance.showFoundBookGenres) View.VISIBLE else View.GONE

            binding.thirdBlockLeftElement.visibility =
                if (PreferencesHandler.instance.showFoundBookFormat) View.VISIBLE else View.GONE

            binding.thirdBlockCenterElement.visibility =
                if (PreferencesHandler.instance.showFoundBookDownloads) View.VISIBLE else View.GONE

            binding.thirdBlockRightElement.visibility =
                if (PreferencesHandler.instance.showFoundBookSize) View.VISIBLE else View.GONE

            binding.availableLinkFormats.visibility =
                if (PreferencesHandler.instance.showFoundBookAvailableFormats) View.VISIBLE else View.GONE

            binding.leftActionBtn.visibility =
                if (PreferencesHandler.instance.showFoundBookReadBtn) View.VISIBLE else View.GONE

            binding.rightActionBtn.visibility =
                if (PreferencesHandler.instance.showFoundBookDownloadBtn) View.VISIBLE else View.GONE
        }

        private fun showElementItems() {
            binding.menuButton.visibility = View.GONE
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
            binding.menuButton.visibility = View.GONE
            binding.previewImage.visibility = View.GONE
        }


        private fun drawElement() {
            showElementItems()

            binding.rootView.setOnLongClickListener {
                delegate.buttonLongPressed(item, "name", binding.rootView)
                return@setOnLongClickListener true
            }

            if (PreferencesHandler.instance.hideOpdsResultsButtons) {
                binding.centerActionBtn.visibility = View.GONE
                binding.name.setPadding(50, 100, 50, 100)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    /*binding.rootView.foreground = with(TypedValue()) {
                        context.theme.resolveAttribute(
                            R.attr.selectableItemBackground, this, true
                        )
                        ContextCompat.getDrawable(context, resourceId)
                    }*/
                }
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
            if (PreferencesHandler.instance.addFilterByLongClick) {
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
            if (PreferencesHandler.instance.showElementDescription) {
                if (item.content.isNotEmpty()) {
                    binding.availableLinkFormats.visibility = View.VISIBLE
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
                }
            } else {
                binding.availableLinkFormats.visibility = View.GONE
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

        private fun hideAllExceptMainBtn() {
            showElementItems()
            binding.name.visibility = View.GONE
            binding.availableLinkFormats.visibility = View.GONE
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
        return -1
    }

    override fun notEmpty(): Boolean {
        return resultValues.isNotEmpty()
    }


    override fun setHasNext(isNext: Boolean) {

    }

    override fun getResultsSize(): Int {
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
    }

    override fun setFilterSelection(selected: Int) {
    }

    override fun filterEnabled(): Boolean {
        return false
    }

    override fun reapplyFilters(results: SearchResult) {
    }

    override fun getFilter(): Filter {
        Log.d("surprise", "getFilter: getting filter!")
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                return FilterResults().apply { values = resultValues }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
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