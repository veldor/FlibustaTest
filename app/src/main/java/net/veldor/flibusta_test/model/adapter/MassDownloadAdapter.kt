package net.veldor.flibusta_test.model.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibusta_test.BR
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.MassDownloadListViewBinding
import net.veldor.flibusta_test.model.delegate.CheckboxDelegate
import net.veldor.flibusta_test.model.handler.FormatHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_BOOK
import net.veldor.flibusta_test.model.selections.opds.FoundEntity

class MassDownloadAdapter(
    arrayList: List<FoundEntity>?,
    val delegate: CheckboxDelegate,
    val context: Context
) :
    RecyclerView.Adapter<MassDownloadAdapter.ViewHolder>() {

    private var selectedFormat: String? = null
    private var values: List<FoundEntity> = arrayListOf()


    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(context)


    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = MassDownloadListViewBinding.inflate(
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

    fun setList(it: List<FoundEntity>?, selectedFormat: String) {
        values = it ?: listOf()
        changeDownloadFormat(selectedFormat)
    }


    inner class ViewHolder(private val binding: MassDownloadListViewBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {
        fun bind(item: FoundEntity) {
            binding.setVariable(BR.item, item)
            binding.executePendingBindings()
            binding.checkBox.setOnCheckedChangeListener { _, state ->
                delegate.checked(state)
                if (state) {
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
                    binding.selectedFormat.setTextColor(FormatHandler.getTextColor(item.selectedLink!!.mime, context))
                } else {
                    binding.selectedFormat.text = context.getString(R.string.skip_title)
                    binding.selectedFormat.setTextColor(
                        ResourcesCompat.getColor(
                            context.resources,
                            R.color.black,
                            context.theme
                        )
                    )
                }
            }
            if (item.selectedLink != null) {
                binding.selectedFormat.text =
                    FormatHandler.getShortFromFullMimeWithoutZip(item.selectedLink!!.mime)
                binding.selectedFormat.setTextColor(FormatHandler.getTextColor(
                    item.selectedLink!!.mime,
                    context
                ))
                binding.checkBox.isChecked = true
            } else {
                // no link for download
                binding.selectedFormat.text = context.getString(R.string.skip_title)
                binding.selectedFormat.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.black,
                        context.theme
                    )
                )
                binding.checkBox.isChecked = false
            }
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
            if (it.selectedLink == null) {
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
           if(it.selectedLink == null){
               it.downloadLinks.forEach outer@{ link ->
                   if (FormatHandler.isSame(link.mime, selectedFormat)) {
                       it.selectedLink = link
                       return@outer
                   }
               }
               if (it.selectedLink == null) {
                   it.selectedLink = it.downloadLinks[0]
               }
            }
            else{
                it.selectedLink = null
            }
        }
        notifyItemRangeChanged(0, values.size)
    }

    fun checkUnloaded() {
        values.forEach {
            if(it.downloaded){
                it.selectedLink = null
            }
            else{
                it.downloadLinks.forEach outer@{ link ->
                    if (FormatHandler.isSame(link.mime, selectedFormat)) {
                        it.selectedLink = link
                        return@outer
                    }
                }
                if (it.selectedLink == null) {
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

    fun changeDownloadFormat(selectedFormat: String) {
        Log.d("surprise", "changeDownloadFormat: selected $selectedFormat")
        this.selectedFormat = selectedFormat
        values.forEach {
            if(it.type == TYPE_BOOK){
                it.selectedLink = null
                it.downloadLinks.forEach outer@{ link ->
                    if (FormatHandler.isSame(link.mime, selectedFormat)) {
                        it.selectedLink = link
                        return@outer
                    }
                }
                if (it.selectedLink == null) {
                    if (PreferencesHandler.instance.strictDownloadFormat) {
                        it.selectedLink = null
                    } else {
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


    init {
        if (arrayList != null && arrayList.isNotEmpty()) {
            values = arrayList
        }
    }
}