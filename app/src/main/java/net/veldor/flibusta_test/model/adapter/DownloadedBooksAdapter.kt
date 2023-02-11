package net.veldor.flibusta_test.model.adapter

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.documentfile.provider.DocumentFile
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.FpFilerowBinding
import net.veldor.flibusta_test.model.db.entity.DownloadedBooks
import net.veldor.flibusta_test.model.handler.GrammarHandler
import net.veldor.flibusta_test.model.listener.DownloadedBookClicked
import java.io.File
import java.net.URI

class DownloadedBooksAdapter(
    diffUtilCallback: DiffUtil.ItemCallback<DownloadedBooks>
) :
    PagedListAdapter<DownloadedBooks, DownloadedBooksAdapter.DownloadedBooksViewHolder>(
        diffUtilCallback
    ) {
    private var mLayoutInflater: LayoutInflater? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadedBooksViewHolder {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(parent.context)
        }
        val binding: FpFilerowBinding = FpFilerowBinding.inflate(mLayoutInflater!!, parent, false)
        return DownloadedBooksViewHolder(binding)
    }

    var listener: DownloadedBookClicked? = null

    inner class DownloadedBooksViewHolder(binding: FpFilerowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var mBinding: FpFilerowBinding = binding

        fun bind(item: DownloadedBooks?) {
            if (item?.destination != null) {
                if (item.destination!!.startsWith("content")) {
                    val file = DocumentFile.fromSingleUri(
                        mBinding.root.context,
                        Uri.parse(item.destination)
                    )
                    if (file?.isFile == true) {
                        Log.d("surprise", "DownloadedBooksViewHolder: 50 ${file.name}")
                        mBinding.name.text = file.name?.replace(" ${item.bookId}", "")
                        mBinding.sizeView.text =
                            GrammarHandler.humanReadableByteCountBin(file.length())
                        mBinding.fileCreateTime.text =
                            GrammarHandler.timestampToDate(file.lastModified())
                        if (file.name?.endsWith("fb2") == true) {
                            mBinding.itemType.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    mBinding.root.context.resources,
                                    R.drawable.fb2_label,
                                    mBinding.root.context.theme
                                )
                            )
                        } else if (file.name?.endsWith("epub") == true) {
                            mBinding.itemType.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    mBinding.root.context.resources,
                                    R.drawable.epub_label,
                                    mBinding.root.context.theme
                                )
                            )
                        } else if (file.name?.endsWith("mobi") == true) {
                            mBinding.itemType.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    mBinding.root.context.resources,
                                    R.drawable.mobi_label,
                                    mBinding.root.context.theme
                                )
                            )
                        } else if (file.name?.endsWith("chm") == true) {
                            mBinding.itemType.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    mBinding.root.context.resources,
                                    R.drawable.chm_label,
                                    mBinding.root.context.theme
                                )
                            )
                        } else if (file.name?.endsWith("djvu") == true) {
                            mBinding.itemType.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    mBinding.root.context.resources,
                                    R.drawable.djvu_label,
                                    mBinding.root.context.theme
                                )
                            )
                        } else if (file.name?.endsWith("doc") == true) {
                            mBinding.itemType.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    mBinding.root.context.resources,
                                    R.drawable.doc_label,
                                    mBinding.root.context.theme
                                )
                            )
                        } else if (file.name?.endsWith("docx") == true) {
                            mBinding.itemType.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    mBinding.root.context.resources,
                                    R.drawable.docx_label,
                                    mBinding.root.context.theme
                                )
                            )
                        } else if (file.name?.endsWith("htm") == true) {
                            mBinding.itemType.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    mBinding.root.context.resources,
                                    R.drawable.htm_label,
                                    mBinding.root.context.theme
                                )
                            )
                        } else if (file.name?.endsWith("html") == true) {
                            mBinding.itemType.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    mBinding.root.context.resources,
                                    R.drawable.html_label,
                                    mBinding.root.context.theme
                                )
                            )
                        } else if (file.name?.endsWith("pdf") == true) {
                            mBinding.itemType.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    mBinding.root.context.resources,
                                    R.drawable.pdf_label,
                                    mBinding.root.context.theme
                                )
                            )
                        } else if (file.name?.endsWith("prc") == true) {
                            mBinding.itemType.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    mBinding.root.context.resources,
                                    R.drawable.prc_label,
                                    mBinding.root.context.theme
                                )
                            )
                        } else if (file.name?.endsWith("rtf") == true) {
                            mBinding.itemType.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    mBinding.root.context.resources,
                                    R.drawable.rtf_label,
                                    mBinding.root.context.theme
                                )
                            )
                        } else if (file.name?.endsWith("txt") == true) {
                            mBinding.itemType.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    mBinding.root.context.resources,
                                    R.drawable.txt_label,
                                    mBinding.root.context.theme
                                )
                            )
                        } else if (file.name?.endsWith("zip") == true) {
                            mBinding.itemType.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    mBinding.root.context.resources,
                                    R.drawable.zip_label,
                                    mBinding.root.context.theme
                                )
                            )
                        } else {
                            mBinding.itemType.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    mBinding.root.context.resources,
                                    R.drawable.misc_label,
                                    mBinding.root.context.theme
                                )
                            )
                        }
                    } else {
                        mBinding.name.text =
                            mBinding.root.context.getString(R.string.no_file_found_title)
                    }
                } else {
                    val file = File(
                        URI.create(Uri.parse(item.destination!!).toString())
                    )
                    if (file.exists()) {
                        mBinding.name.text = file.name.replace(" ${item.bookId}", "")
                        mBinding.sizeView.text =
                            GrammarHandler.humanReadableByteCountBin(file.length())
                        mBinding.fileCreateTime.text =
                            GrammarHandler.timestampToDate(file.lastModified())
                        when (file.extension) {
                            "fb2" -> {
                                mBinding.itemType.setImageDrawable(
                                    ResourcesCompat.getDrawable(
                                        mBinding.root.context.resources,
                                        R.drawable.fb2_label,
                                        mBinding.root.context.theme
                                    )
                                )
                            }
                            "epub" -> {
                                mBinding.itemType.setImageDrawable(
                                    ResourcesCompat.getDrawable(
                                        mBinding.root.context.resources,
                                        R.drawable.epub_label,
                                        mBinding.root.context.theme
                                    )
                                )
                            }
                            "mobi" -> {
                                mBinding.itemType.setImageDrawable(
                                    ResourcesCompat.getDrawable(
                                        mBinding.root.context.resources,
                                        R.drawable.mobi_label,
                                        mBinding.root.context.theme
                                    )
                                )
                            }
                            "chm" -> {
                                mBinding.itemType.setImageDrawable(
                                    ResourcesCompat.getDrawable(
                                        mBinding.root.context.resources,
                                        R.drawable.chm_label,
                                        mBinding.root.context.theme
                                    )
                                )
                            }
                            "djvu" -> {
                                mBinding.itemType.setImageDrawable(
                                    ResourcesCompat.getDrawable(
                                        mBinding.root.context.resources,
                                        R.drawable.djvu_label,
                                        mBinding.root.context.theme
                                    )
                                )
                            }
                            "doc" -> {
                                mBinding.itemType.setImageDrawable(
                                    ResourcesCompat.getDrawable(
                                        mBinding.root.context.resources,
                                        R.drawable.doc_label,
                                        mBinding.root.context.theme
                                    )
                                )
                            }
                            "docx" -> {
                                mBinding.itemType.setImageDrawable(
                                    ResourcesCompat.getDrawable(
                                        mBinding.root.context.resources,
                                        R.drawable.docx_label,
                                        mBinding.root.context.theme
                                    )
                                )
                            }
                            "htm" -> {
                                mBinding.itemType.setImageDrawable(
                                    ResourcesCompat.getDrawable(
                                        mBinding.root.context.resources,
                                        R.drawable.htm_label,
                                        mBinding.root.context.theme
                                    )
                                )
                            }
                            "html" -> {
                                mBinding.itemType.setImageDrawable(
                                    ResourcesCompat.getDrawable(
                                        mBinding.root.context.resources,
                                        R.drawable.html_label,
                                        mBinding.root.context.theme
                                    )
                                )
                            }
                            "pdf" -> {
                                mBinding.itemType.setImageDrawable(
                                    ResourcesCompat.getDrawable(
                                        mBinding.root.context.resources,
                                        R.drawable.pdf_label,
                                        mBinding.root.context.theme
                                    )
                                )
                            }
                            "prc" -> {
                                mBinding.itemType.setImageDrawable(
                                    ResourcesCompat.getDrawable(
                                        mBinding.root.context.resources,
                                        R.drawable.prc_label,
                                        mBinding.root.context.theme
                                    )
                                )
                            }
                            "rtf" -> {
                                mBinding.itemType.setImageDrawable(
                                    ResourcesCompat.getDrawable(
                                        mBinding.root.context.resources,
                                        R.drawable.rtf_label,
                                        mBinding.root.context.theme
                                    )
                                )
                            }
                            "txt" -> {
                                mBinding.itemType.setImageDrawable(
                                    ResourcesCompat.getDrawable(
                                        mBinding.root.context.resources,
                                        R.drawable.txt_label,
                                        mBinding.root.context.theme
                                    )
                                )
                            }
                            "zip" -> {
                                mBinding.itemType.setImageDrawable(
                                    ResourcesCompat.getDrawable(
                                        mBinding.root.context.resources,
                                        R.drawable.zip_label,
                                        mBinding.root.context.theme
                                    )
                                )
                            }
                            else -> {
                                mBinding.itemType.setImageDrawable(
                                    ResourcesCompat.getDrawable(
                                        mBinding.root.context.resources,
                                        R.drawable.misc_label,
                                        mBinding.root.context.theme
                                    )
                                )
                            }
                        }
                    } else {
                        mBinding.name.text = App.instance.getString(R.string.file_not_exists_title)
                    }
                }
            } else {
                mBinding.name.text =
                    mBinding.root.context.getString(R.string.no_destination_found_title)
            }

            mBinding.root.setOnClickListener {
                listener?.clicked(item)
            }
        }
    }

    override fun onBindViewHolder(holder: DownloadedBooksViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun itemDeleted(item: DownloadedBooks) {
        val index = currentList?.indexOf(item)

        if (index != null) {
            notifyItemRemoved(index)
        }
    }
}