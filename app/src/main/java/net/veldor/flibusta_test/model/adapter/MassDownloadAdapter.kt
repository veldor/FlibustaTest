package net.veldor.flibusta_test.model.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibusta_test.BR
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.MassDownloadListItemBinding
import net.veldor.flibusta_test.model.delegate.CheckboxDelegate
import net.veldor.flibusta_test.model.handler.FormatHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.parser.OpdsParser
import net.veldor.flibusta_test.model.selection.FoundEntity

class MassDownloadAdapter(
    arrayList: ArrayList<FoundEntity>?,
    val delegate: CheckboxDelegate,
    val context: Context
) :
    RecyclerView.Adapter<MassDownloadAdapter.ViewHolder>() {

    private var selectedFormat: String? = null
    private var values: ArrayList<FoundEntity> = arrayListOf()

    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(context)


    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = MassDownloadListItemBinding.inflate(
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

    fun setList(it: ArrayList<FoundEntity>?, selectedFormat: String) {
        values = it ?: arrayListOf()
        changeDownloadFormat(selectedFormat)
    }


    inner class ViewHolder(private val binding: MassDownloadListItemBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {
        init {
            if (PreferencesHandler.isEInk) {
                binding.cardView.setCardBackgroundColor(
                    ResourcesCompat.getColor(context.resources, R.color.white, context.theme)
                )

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

            binding.root.setOnClickListener {
                binding.checkBox.performClick()
            }
        }

        fun bind(item: FoundEntity) {
            binding.setVariable(BR.item, item)
            binding.executePendingBindings()

            if (item.name == null) {
                binding.root.showShimmer(true)
            } else {
                binding.root.stopShimmer()
                binding.root.hideShimmer()
            }
            binding.checkBox.setOnCheckedChangeListener { _, state ->
                delegate.checked(state)
                if (state) {
                    if (item.downloadLinks.isEmpty()) {
                        binding.checkBox.isChecked = false
                        binding.selectedFormat.text = "--"
                        binding.selectedFormat.setTextColor(
                            ResourcesCompat.getColor(
                                context.resources,
                                R.color.textColor,
                                context.theme
                            )
                        )
                        Toast.makeText(
                            context,
                            context.getString(R.string.no_download_links_title),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnCheckedChangeListener
                    }
                    if (selectedFormat != null) {
                        item.downloadLinks.forEach { link ->
                            if (FormatHandler.isSame(link.mime, selectedFormat)) {
                                item.selectedLink = link
                                return@forEach
                            }
                        }
                        if (item.selectedLink == null) {
                            item.selectedLink = item.downloadLinks[0]
                        }
                    } else {
                        item.selectedLink = item.downloadLinks[0]
                    }

                } else {
                    item.selectedLink = null
                }
                if (item.selectedLink != null) {
                    binding.selectedFormat.text =
                        FormatHandler.getShortFromFullMimeWithoutZip(item.selectedLink!!.mime)
                    binding.selectedFormat.setTextColor(
                        FormatHandler.getTextColor(
                            item.selectedLink!!.mime,
                            context
                        )
                    )
                } else {
                    binding.selectedFormat.text = context.getString(R.string.skip_title)
                    binding.selectedFormat.setTextColor(
                        ResourcesCompat.getColor(
                            context.resources,
                            R.color.invertable_black,
                            context.theme
                        )
                    )
                }
            }
            if (item.selectedLink != null) {
                binding.selectedFormat.text =
                    FormatHandler.getShortFromFullMimeWithoutZip(item.selectedLink!!.mime)
                binding.selectedFormat.setTextColor(
                    FormatHandler.getTextColor(
                        item.selectedLink!!.mime,
                        context
                    )
                )
                binding.checkBox.isChecked = true
            } else {
                // no link for download
                binding.selectedFormat.text = context.getString(R.string.skip_title)
                binding.selectedFormat.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.textColor,
                        context.theme
                    )
                )
                binding.checkBox.isChecked = false
            }

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

    fun checkAll() {
        values.forEach {
            it.downloadLinks.forEach outer@{ link ->
                if (FormatHandler.isSame(link.mime, selectedFormat)) {
                    it.selectedLink = link
                    return@outer
                }
            }
            if (it.selectedLink == null && it.downloadLinks.isNotEmpty() && !PreferencesHandler.strictDownloadFormat) {
                it.selectedLink = it.downloadLinks[0]
            }
        }
        notifyItemRangeChanged(0, values.size)
    }

    fun uncheckAll() {
        values.forEach {
            it.selectedLink = null
        }
        notifyItemRangeChanged(0, values.size)
    }

    fun reverseCheckAll() {
        values.forEach {
            if (it.selectedLink == null) {
                it.downloadLinks.forEach outer@{ link ->
                    if (FormatHandler.isSame(link.mime, selectedFormat)) {
                        it.selectedLink = link
                        return@outer
                    }
                }
                if (it.selectedLink == null && it.downloadLinks.isNotEmpty()) {
                    it.selectedLink = it.downloadLinks[0]
                }
            } else {
                it.selectedLink = null
            }
        }
        notifyItemRangeChanged(0, values.size)
    }

    fun checkUnloaded() {
        values.forEach {
            if (it.downloaded) {
                it.selectedLink = null
            } else {
                it.downloadLinks.forEach outer@{ link ->
                    if (FormatHandler.isSame(
                            link.mime,
                            selectedFormat
                        )
                    ) {
                        it.selectedLink = link
                        return@outer
                    }
                }
                if (it.selectedLink == null && it.downloadLinks.isNotEmpty() && !PreferencesHandler.strictDownloadFormat) {
                    it.selectedLink = it.downloadLinks[0]
                }
            }
        }
        notifyItemRangeChanged(0, values.size)
    }

    fun countSelected(): Int {
        var counter = 0
        values.forEach {
            if (it.selectedLink != null) {
                counter++
            }
        }
        return counter
    }

    @SuppressLint("NotifyDataSetChanged")
    fun changeDownloadFormat(selectedFormat: String) {
        this.selectedFormat = selectedFormat
        values.forEach {
            if (it.type == OpdsParser.TYPE_BOOK) {
                it.selectedLink = null
                it.downloadLinks.forEach outer@{ link ->
                    if (FormatHandler.isSame(link.mime, selectedFormat)) {
                        it.selectedLink = link
                        return@outer
                    }
                }
                if (it.selectedLink == null) {
                    if (PreferencesHandler.strictDownloadFormat || it.downloadLinks.isEmpty() || it.downloadLinks[0].mime == null) {
                        it.selectedLink = null
                    } else if (it.downloadLinks.isNotEmpty()) {
                        it.selectedLink = it.downloadLinks[0]
                    }
                }
            }
        }
        notifyDataSetChanged()
    }

    fun setStrictFormat() {
        if (selectedFormat != null) {
            changeDownloadFormat(selectedFormat!!)
        }
    }

    fun getList(): List<FoundEntity> {
        return values
    }

    fun updateBookInfo(book: FoundEntity) {
        Log.d("surprise", "MassDownloadAdapter.kt 249: update book info ${book.id}")
        var positon = -1
        // check position of book
        values.forEach { listItem ->
            if (listItem.id == book.id) {
                positon = values.indexOf(listItem)
                Log.d("surprise", "MassDownloadAdapter.kt 256: have book in position $positon")
                return@forEach
            }
        }
        if (positon >= 0) {
            values[positon] = book
            // set checked
            book.selectedLink = null
            if (book.downloadLinks.isNotEmpty()) {
                book.downloadLinks.forEach outer@{ link ->
                    if (FormatHandler.isSame(link.mime, selectedFormat)) {
                        book.selectedLink = link
                        delegate.checked(true)
                        return@outer
                    }
                }
                if (book.selectedLink == null) {
                    if (PreferencesHandler.strictDownloadFormat || book.downloadLinks.isEmpty()) {
                        book.selectedLink = null
                        if (book.name == null) {
                            book.downloadLinks.forEach outer@{ link ->
                                if (link.name != null) {
                                    book.name = link.name!!
                                    notifyItemChanged(positon)
                                    return
                                }
                            }
                            book.name = "no links"
                        }
                        delegate.checked(false)
                    } else if (book.downloadLinks.isNotEmpty()) {
                        book.downloadLinks.forEach outer@{ link ->
                            if (link.mime != null) {
                                book.selectedLink = link
                                delegate.checked(true)
                                notifyItemChanged(positon)
                                return
                            }
                        }
                        book.name = "no links"
                    } else {
                        Log.d("surprise", "MassDownloadAdapter.kt 277: notify book has no links")
                        book.name = "no links"
                    }
                }
            } else {
                Log.d("surprise", "MassDownloadAdapter.kt 277: notify book has no links")
                book.name = "no links"
            }
            notifyItemChanged(positon)
        }
    }


    init {
        if (arrayList != null && arrayList.isNotEmpty()) {
            values = arrayList
        }
    }
}