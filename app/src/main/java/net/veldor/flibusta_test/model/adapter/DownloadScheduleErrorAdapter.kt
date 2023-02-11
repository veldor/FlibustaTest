package net.veldor.flibusta_test.model.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibusta_test.BR
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.DownloadScheduleErrorItemLayoutBinding
import net.veldor.flibusta_test.model.db.entity.DownloadError
import net.veldor.flibusta_test.model.delegate.SomeButtonPressedDelegate
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.helper.UrlHelper

class DownloadScheduleErrorAdapter(
    arrayList: List<DownloadError>?,
    val delegate: SomeButtonPressedDelegate,
    val context: Context
) :
    RecyclerView.Adapter<DownloadScheduleErrorAdapter.ViewHolder>() {

    private var values: List<DownloadError> = arrayListOf()


    private var mLayoutInflater: LayoutInflater =
        LayoutInflater.from(context)


    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val binding = DownloadScheduleErrorItemLayoutBinding.inflate(
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
    fun setList(it: List<DownloadError>?) {
        values = it ?: listOf()
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: DownloadScheduleErrorItemLayoutBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {
        init {
            if(PreferencesHandler.isEInk){
                binding.bookName.setTextColor(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.invertable_black,
                        context.theme
                    )
                )
            }
        }

        fun bind(item: DownloadError) {
            binding.setVariable(BR.item, item)
            binding.executePendingBindings()
            binding.errorReason.text = item.error
            binding.pathToFile.text = UrlHelper.getDownloadedBookPath(item)
            binding.actionBtn.setOnClickListener {
                delegate.buttonPressed(item)
            }
        }
    }


    init {
        if (arrayList != null && arrayList.isNotEmpty()) {
            values = arrayList
        }
    }
}