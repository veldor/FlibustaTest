package net.veldor.flibusta_test.model.data_source

import androidx.recyclerview.widget.DiffUtil
import net.veldor.flibusta_test.model.db.entity.DownloadedBooks

class DownloadedBooksDiffCallback : DiffUtil.ItemCallback<DownloadedBooks>() {

    override fun areItemsTheSame(oldItem: DownloadedBooks, newItem: DownloadedBooks): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DownloadedBooks, newItem: DownloadedBooks): Boolean {
        return oldItem.bookId == newItem.bookId
    }

}