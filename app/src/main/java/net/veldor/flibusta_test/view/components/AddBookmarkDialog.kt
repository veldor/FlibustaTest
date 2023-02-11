package net.veldor.flibusta_test.view.components

import android.app.Dialog
import android.os.Bundle
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.adapter.BookmarkDirAdapter
import net.veldor.flibusta_test.model.handler.BookmarkHandler
import net.veldor.flibusta_test.model.selection.BookmarkItem
import java.util.*

class AddBookmarkDialog : DialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val layout = layoutInflater.inflate(R.layout.dialog_catalog_bookmark, null, false)
            val linkValueView = layout.findViewById<TextInputEditText>(R.id.linkValue)
            val bookmarkNameTextView =
                layout.findViewById<TextInputEditText>(R.id.bookmarkName)
            bookmarkNameTextView.setText(bookmarkReservedName)
            linkValueView.setText(link)
            val spinner = layout.findViewById<Spinner>(R.id.bookmarkFoldersSpinner)
            spinner.adapter = BookmarkDirAdapter(
                requireActivity(),
                BookmarkHandler.getBookmarkCategories(requireContext())
            )
            AlertDialog.Builder(requireActivity(), R.style.dialogTheme)
                .setTitle(getString(R.string.add_bookmark_title))
                .setView(layout)
                .setPositiveButton(getString(R.string.add_title)) { _, _ ->
                    val categoryTextView =
                        layout.findViewById<TextInputEditText>(R.id.addNewFolderText)
                    val category: BookmarkItem = if (categoryTextView.text?.isNotEmpty() == true) {
                        BookmarkHandler.addCategory(categoryTextView.text.toString())
                    } else {
                        spinner.selectedItem as BookmarkItem
                    }
                    BookmarkHandler.addBookmark(
                        category,
                        bookmarkNameTextView.text.toString(),
                        linkValueView.text.toString()
                    )
                    activity?.invalidateOptionsMenu()
                    if (category.id.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            String.format(
                                Locale.ENGLISH,
                                getString(R.string.add_bookmark_template),
                                bookmarkNameTextView.text.toString()
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            String.format(
                                Locale.ENGLISH,
                                getString(R.string.add_bookmark_with_category_template),
                                bookmarkNameTextView.text.toString(),
                                category.name
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        lateinit var bookmarkReservedName: String
        lateinit var link: String
    }
}