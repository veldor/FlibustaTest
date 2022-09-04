package net.veldor.flibusta_test.model.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibusta_test.BR
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.DownloadScheduleItemLayoutBinding
import net.veldor.flibusta_test.model.db.entity.BooksDownloadSchedule
import net.veldor.flibusta_test.model.delegate.SomeButtonPressedDelegate
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.helper.UrlHelper
import net.veldor.flibusta_test.model.selections.BooksDownloadProgress

class DownloadScheduleAdapter(
    arrayList: List<BooksDownloadSchedule>?,
    val delegate: SomeButtonPressedDelegate,
    val context: Context
) :
    RecyclerView.Adapter<DownloadScheduleAdapter.ViewHolder>() {

    private var loadedBookProgress: Double = 0.0
    private var loadedBookName: String? = null
    private var values: List<BooksDownloadSchedule> = arrayListOf()


    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(context)


    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = DownloadScheduleItemLayoutBinding.inflate(
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

    @SuppressLint("NotifyDataSetChanged")
    fun setList(it: List<BooksDownloadSchedule>?) {
        values = it ?: listOf()
        notifyDataSetChanged()
    }

    fun setProgress(progress: BooksDownloadProgress?) {
        if (progress != null) {
            var pos = -1
            values.forEach {
                if (it.name == progress.currentlyLoadedBookName) {
                    pos = values.indexOf(it)
                    return@forEach
                }
            }
            if (pos >= 0) {
                loadedBookName = progress.currentlyLoadedBookName
                val percentDone: Double = progress.bookLoadedSize.toDouble() / progress.bookFullSize.toDouble() * 100
                loadedBookProgress = percentDone
                notifyItemChanged(pos)
            }
        }
    }

    inner class ViewHolder(private val binding: DownloadScheduleItemLayoutBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {

        init {
            if (PreferencesHandler.instance.isEInk) {
                binding.bookName.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.invertable_black,
                        context.theme
                    )
                )
                binding.actionBtn.setColorFilter(
                    ResourcesCompat.getColor(context.resources, R.color.invertable_black, context.theme),
                    PorterDuff.Mode.SRC_ATOP
                )
            }

        }

        fun bind(item: BooksDownloadSchedule) {
            binding.setVariable(BR.item, item)
            binding.executePendingBindings()
            binding.pathToFile.text = UrlHelper.getDownloadedBookPath(item)
            binding.actionBtn.setOnClickListener {
                delegate.buttonPressed(item)
            }
            if (item.name == loadedBookName) {
                binding.bookDownloadProgress.visibility = View.VISIBLE
                binding.bookDownloadProgress.isIndeterminate = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    binding.bookDownloadProgress.setProgress(loadedBookProgress.toInt(), true)
                } else {
                    binding.bookDownloadProgress.progress = loadedBookProgress.toInt()
                }
            }
            else{
                binding.bookDownloadProgress.visibility = View.GONE
            }
        }

    }


    init {
        if (arrayList != null && arrayList.isNotEmpty()) {
            values = arrayList
        }
    }
}