package net.veldor.flibusta_test.view.components

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.BookDetailsDialogViewBinding
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.selection.FoundEntity
import net.veldor.flibusta_test.model.selection.OpdsStatement
import net.veldor.flibusta_test.view.SearchActivity
import net.veldor.flibusta_test.view.search_fragment.OpdsFragment

class BookDetailsDialog : DialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val binding = BookDetailsDialogViewBinding.inflate(layoutInflater)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                binding.bookContentView.text = Html.fromHtml(book.content, Html.FROM_HTML_MODE_COMPACT)
            }
            else{
                binding.bookContentView.text = Html.fromHtml(book.content)
            }
            val builder = AlertDialog.Builder(it, R.style.dialogTheme)
            builder.setView(binding.root)
            builder.setTitle(mTitle)
            builder
                .setPositiveButton(R.string.download_title) { _, _ ->
                val fragment = (activity as SearchActivity?)?.getCurrentFragment()
                if (fragment is OpdsFragment) {
                    fragment.showBookDownloadOptions(book)
                }
            }
                .setNegativeButton(R.string.close_message, null)

                .setNeutralButton(R.string.show_on_site_title) { _, _ ->
                    OpdsStatement.setPressedItem(book)
                    PreferencesHandler.lastWebViewLink = book.link
                    (activity as SearchActivity?)?.launchWebViewFromOpds()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        var mTitle: String? = null
        lateinit var book: FoundEntity
    }
}