package net.veldor.flibusta_test.model.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibusta_test.BR
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FileItemBinding
import net.veldor.flibusta_test.model.selections.FileItem
import net.veldor.flibusta_test.ui.DownloadDirContentActivity
import java.text.SimpleDateFormat
import java.util.*

class ExplorerAdapter(
    arrayList: ArrayList<FileItem>?,
    val context: DownloadDirContentActivity
) :
    RecyclerView.Adapter<ExplorerAdapter.ViewHolder>() {

    private var values: ArrayList<FileItem> = arrayListOf()


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
        notifyItemRangeInserted(0, 0)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun appendContent(results: ArrayList<FileItem>) {
        val oldLength = itemCount
        values.addAll(results)
        if (oldLength > 0) {
            notifyItemRangeInserted(oldLength, results.size)
        } else {
            notifyDataSetChanged()
        }
    }


    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.bind(values[i], i)
    }

    override fun getItemCount(): Int {
        return values.size
    }

    fun delete(item: FileItem) {
        val position = values.lastIndexOf(item)
        if (position >= 0) {
            values.remove(item)
            notifyItemRemoved(position)
        }
    }

    inner class ViewHolder(private val binding: FileItemBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {

        fun bind(item: FileItem, position: Int) {
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
        return values
    }

    init {
        if (arrayList != null && arrayList.isNotEmpty()) {
            values = arrayList
        }
    }
}