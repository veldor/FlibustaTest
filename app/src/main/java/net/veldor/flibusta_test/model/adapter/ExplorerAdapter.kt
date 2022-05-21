package net.veldor.flibusta_test.model.adapter

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibusta_test.BR
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FileItemBinding
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.selections.FileItem
import net.veldor.flibusta_test.ui.DownloadDirContentActivity
import java.text.SimpleDateFormat
import java.util.*

class ExplorerAdapter(
    arrayList: ArrayList<FileItem>?,
    val context: DownloadDirContentActivity
) :
    RecyclerView.Adapter<ExplorerAdapter.ViewHolder>(), Filterable {

    private var values: ArrayList<FileItem> = arrayListOf()
    private var filteredValues: ArrayList<FileItem> = arrayListOf()


    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(context)


    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = FileItemBinding.inflate(
            mLayoutInflater, viewGroup, false
        )
        return ViewHolder(binding)
    }

    fun clearList() {
        notifyItemRangeRemoved(0, itemCount)
        values = ArrayList()
        filteredValues = ArrayList()
        notifyItemRangeInserted(0, 0)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun appendContent(results: ArrayList<FileItem>, filterRequest: String?) {
        values.addAll(results)
        filter.filter(filterRequest)
    }


    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(filteredValues[i], i)
    }

    override fun getItemCount(): Int {
        return filteredValues.size
    }

    fun delete(item: FileItem) {
        val position = filteredValues.lastIndexOf(item)
        values.remove(item)
        if (position >= 0) {
            filteredValues.remove(item)
            notifyItemRemoved(position)
        }
    }

    inner class ViewHolder(private val binding: FileItemBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {

        init {
            if(PreferencesHandler.instance.isEInk){
                binding.name.setTextColor(ResourcesCompat.getColor(
                    context.resources,
                    R.color.black,
                    context.theme
                ))
                binding.authorName.setTextColor(ResourcesCompat.getColor(
                    context.resources,
                    R.color.black,
                    context.theme
                ))
                binding.itemType.setColorFilter(
                    ResourcesCompat.getColor(context.resources, R.color.black, context.theme),
                    PorterDuff.Mode.SRC_ATOP
                )
            }
        }

        fun bind(item: FileItem, position: Int) {
            Log.d("surprise", "ExplorerAdapter.kt 77: bind ${item.name}")
            binding.setVariable(BR.item, item)
            binding.executePendingBindings()
            val sdf = SimpleDateFormat("dd MMM yyyy hh:mm:ss", Locale.ENGLISH)
            val netDate = Date(item.file.lastModified())
            binding.authorName.text = sdf.format(netDate)
            binding.authorName.visibility = View.VISIBLE

            if (item.file.isDirectory) {
                binding.itemType.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.fp_folder,
                        context.theme
                    )
                )
                binding.fileSize.visibility = View.GONE

                binding.root.setOnLongClickListener {
                    context.itemLongClicked(item, binding.root, position)
                    return@setOnLongClickListener true
                }
            } else {
                binding.fileSize.text = item.size
                binding.fileSize.visibility = View.VISIBLE
                when (item.type) {
                    "fb2" -> {
                        binding.itemType.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.fb2_label,
                                context.theme
                            )
                        )
                    }
                    "epub" -> {
                        binding.itemType.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.epub_label,
                                context.theme
                            )
                        )
                    }
                    "mobi" -> {
                        binding.itemType.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.mobi_label,
                                context.theme
                            )
                        )
                    }
                    "chm" -> {
                        binding.itemType.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.chm_label,
                                context.theme
                            )
                        )
                    }
                    "djvu" -> {
                        binding.itemType.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.djvu_label,
                                context.theme
                            )
                        )
                    }
                    "doc" -> {
                        binding.itemType.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.doc_label,
                                context.theme
                            )
                        )
                    }
                    "docx" -> {
                        binding.itemType.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.docx_label,
                                context.theme
                            )
                        )
                    }
                    "htm" -> {
                        binding.itemType.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.htm_label,
                                context.theme
                            )
                        )
                    }
                    "html" -> {
                        binding.itemType.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.html_label,
                                context.theme
                            )
                        )
                    }
                    "pdf" -> {
                        binding.itemType.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.pdf_label,
                                context.theme
                            )
                        )
                    }
                    "prc" -> {
                        binding.itemType.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.prc_label,
                                context.theme
                            )
                        )
                    }
                    "rtf" -> {
                        binding.itemType.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.rtf_label,
                                context.theme
                            )
                        )
                    }
                    "txt" -> {
                        binding.itemType.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.txt_label,
                                context.theme
                            )
                        )
                    }
                    "zip" -> {
                        binding.itemType.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.zip_label,
                                context.theme
                            )
                        )
                    }
                    else -> {
                        binding.itemType.setImageDrawable(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.misc_label,
                                context.theme
                            )
                        )
                    }
                }
                binding.root.setOnLongClickListener { return@setOnLongClickListener false }
            }
            binding.root.setOnClickListener {
                context.itemClicked(item, binding.root, position)
            }
        }
    }


    fun getList(): ArrayList<FileItem> {
        return filteredValues
    }

    init {
        if (arrayList != null && arrayList.isNotEmpty()) {
            values = arrayList
            filteredValues = values
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString() ?: ""
                Log.d("surprise", "ExplorerAdapter.kt 267: filter for $charString")
                filteredValues = if (charString.isEmpty()) {
                    values
                } else {
                    val filteredList = ArrayList<FileItem>()
                    values
                        .filter {
                            (it.name.lowercase().contains(charString))

                        }
                        .forEach {
                            filteredList.add(it)
                        }
                    filteredList
                }
                return FilterResults().apply { values = filteredValues }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredValues = if (results?.values != null) {
                    val r = results.values as ArrayList<*>
                    val result: ArrayList<FileItem> = arrayListOf()
                    r.forEach {
                        if (it is FileItem) {
                            result.add(it)
                        }
                    }
                    result
                } else
                    arrayListOf()
                notifyItemRangeChanged(0, filteredValues.size)
            }
        }
    }
}