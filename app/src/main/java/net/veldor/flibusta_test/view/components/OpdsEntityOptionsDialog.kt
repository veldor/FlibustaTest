package net.veldor.flibusta_test.view.components

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.selection.FoundEntity
import net.veldor.flibusta_test.model.selection.OpdsStatement
import net.veldor.flibusta_test.model.selection.filter.BlacklistItem
import net.veldor.flibusta_test.model.selection.subscribe.SubscribeItem
import net.veldor.flibusta_test.view.SearchActivity
import net.veldor.flibusta_test.view.search_fragment.OpdsFragment

class OpdsEntityOptionsDialog : DialogFragment() {

    private val genreItems: Array<String>
    get() {
        return arrayOf(
            requireContext().getString(R.string.option_blacklist_genre),
            requireContext().getString(R.string.option_subscribe_genre),
            requireContext().getString(R.string.option_bookmark_genre),
        )
    }

    private val sequenceItems: Array<String>
    get() {
        return arrayOf(
            requireContext().getString(R.string.option_blacklist_sequece),
            requireContext().getString(R.string.option_subscribe_sequence),
            requireContext().getString(R.string.option_bookmark_sequence),
        )
    }

    private val authorItems: Array<String>
    get() {
        return arrayOf(
            requireContext().getString(R.string.option_blacklist_author),
            requireContext().getString(R.string.option_subscribe_author),
            requireContext().getString(R.string.option_bookmark_author),
            requireContext().getString(R.string.option_show_author_books_by_sequences),
            requireContext().getString(R.string.option_show_author_books_without_sequences),
            requireContext().getString(R.string.option_show_author_books_by_alphabet),
            requireContext().getString(R.string.option_show_author_books_by_time_of_registration),
        )
    }
    private val bookItems: Array<String>
    get() {
        return arrayOf(
            requireContext().getString(R.string.option_download_book),
            requireContext().getString(R.string.option_show_in_browser),
            requireContext().getString(R.string.option_blacklist_author),
            requireContext().getString(R.string.option_subscribe_author),
            requireContext().getString(R.string.option_bookmark_author),
            requireContext().getString(R.string.option_blacklist_genre),
            requireContext().getString(R.string.option_subscribe_genre),
            requireContext().getString(R.string.option_blacklist_sequece),
            requireContext().getString(R.string.option_subscribe_sequence),
            requireContext().getString(R.string.option_bookmark_sequence),
            requireContext().getString(R.string.option_blacklist_book_name),
            requireContext().getString(R.string.option_subscribe_book_name),
            requireContext().getString(R.string.do_mark_as_downloaded_title),
            requireContext().getString(R.string.do_mark_as_read_title),
        )
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it, R.style.dialogTheme)
            val items = when(foundEntity.type){
                "genre" -> {genreItems}
                "sequence" -> {sequenceItems}
                "author" -> {authorItems}
                else -> {
                if(foundEntity.downloaded){
                    bookItems[12] = requireContext().getString(R.string.do_mark_as_no_download_title)
                }
                if(foundEntity.read){
                    bookItems[13] = requireContext().getString(R.string.do_mark_as_no_read_title)
                }
                    bookItems
                }
            }
            builder.setItems(items){ _, which ->
                handleClickedItem(which)
            }
            builder.setTitle(foundEntity.name)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun handleClickedItem(which: Int) {
        when(foundEntity.type){
            FoundEntity.TYPE_AUTHOR ->{
                when(which){
                    0 -> {
                        val dialog = AddBlacklistItemDialog()
                        AddBlacklistItemDialog.type = BlacklistItem.TYPE_AUTHOR
                        AddBlacklistItemDialog.value = foundEntity.name!!
                        dialog.showNow(requireActivity().supportFragmentManager, "BLACKLIST ADD DIALOG")
                    }
                    1 -> {
                        val dialog = AddSubscribeItemDialog()
                        AddSubscribeItemDialog.type = SubscribeItem.TYPE_AUTHOR
                        AddSubscribeItemDialog.value = foundEntity.name!!
                        dialog.showNow(requireActivity().supportFragmentManager, "SUBSCRIBE ADD DIALOG")
                    }
                    2 -> {
                        val dialog = AddBookmarkDialog()
                        AddBookmarkDialog.link = foundEntity.link ?: ""
                        AddBookmarkDialog.bookmarkReservedName = foundEntity.name ?: ""
                        dialog.showNow(requireActivity().supportFragmentManager, "ADD BOOKMARK DIALOG")
                    }
                    3 -> {
                        val link = Regex("[^0-9]").replace(foundEntity.link!!, "")
                        val url = "/opds/authorsequences/$link"
                        (activity as SearchActivity?)?.openInOpds(url)
                    }
                    4 -> {
                        val link = Regex("[^0-9]").replace(foundEntity.link!!, "")
                        val url = "/opds/author/$link/authorsequenceless"
                        (activity as SearchActivity?)?.openInOpds(url)
                    }
                    5 -> {
                        val link = Regex("[^0-9]").replace(foundEntity.link!!, "")
                        val url = "/opds/author/$link/alphabet"
                        (activity as SearchActivity?)?.openInOpds(url)
                    }
                    6 -> {
                        val link = Regex("[^0-9]").replace(foundEntity.link!!, "")
                        val url = "/opds/author/$link/time"
                        (activity as SearchActivity?)?.openInOpds(url)
                    }
                }
            }
            FoundEntity.TYPE_GENRE ->{
                when(which){
                    0 -> {
                        val dialog = AddBlacklistItemDialog()
                        AddBlacklistItemDialog.type = BlacklistItem.TYPE_GENRE
                        AddBlacklistItemDialog.value = foundEntity.name!!
                        dialog.showNow(requireActivity().supportFragmentManager, "BLACKLIST ADD DIALOG")
                    }
                    1 -> {
                        val dialog = AddSubscribeItemDialog()
                        AddSubscribeItemDialog.type = SubscribeItem.TYPE_GENRE
                        AddSubscribeItemDialog.value = foundEntity.name!!
                        dialog.showNow(requireActivity().supportFragmentManager, "SUBSCRIBE ADD DIALOG")
                    }
                    2 -> {
                        val dialog = AddBookmarkDialog()
                        AddBookmarkDialog.link = foundEntity.link ?: ""
                        AddBookmarkDialog.bookmarkReservedName = foundEntity.name ?: ""
                        dialog.showNow(requireActivity().supportFragmentManager, "ADD BOOKMARK DIALOG")
                    }
                }
            }
            FoundEntity.TYPE_SEQUENCE ->{
                when(which){
                    0 -> {
                        val dialog = AddBlacklistItemDialog()
                        AddBlacklistItemDialog.type = BlacklistItem.TYPE_SEQUENCE
                        AddBlacklistItemDialog.value = foundEntity.name!!
                        dialog.showNow(requireActivity().supportFragmentManager, "BLACKLIST ADD DIALOG")
                    }
                    1 -> {
                        val dialog = AddSubscribeItemDialog()
                        AddSubscribeItemDialog.type = SubscribeItem.TYPE_SEQUENCE
                        AddSubscribeItemDialog.value = foundEntity.name!!
                        dialog.showNow(requireActivity().supportFragmentManager, "SUBSCRIBE ADD DIALOG")
                    }
                    2 -> {
                        val dialog = AddBookmarkDialog()
                        AddBookmarkDialog.link = foundEntity.link ?: ""
                        AddBookmarkDialog.bookmarkReservedName = foundEntity.name ?: ""
                        dialog.showNow(requireActivity().supportFragmentManager, "ADD BOOKMARK DIALOG")
                    }
                }
            }
            FoundEntity.TYPE_BOOK ->{
                when(which){
                    0 -> {
                        val dialog = BookDownloadSetupDialog()
                        dialog.setup(foundEntity)
                        dialog.showNow(requireActivity().supportFragmentManager, "BOOK DOWNLOAD DETAILS DIALOG")
                    }
                    1 -> {
                        OpdsStatement.setPressedItem(foundEntity)
                        PreferencesHandler.lastWebViewLink = foundEntity.link
                        (activity as SearchActivity?)?.launchWebViewFromOpds()
                    }

                    2 -> {
                        val dialog = AddBlacklistItemDialog()
                        AddBlacklistItemDialog.type = BlacklistItem.TYPE_AUTHOR
                        AddBlacklistItemDialog.value = foundEntity.author
                        dialog.showNow(requireActivity().supportFragmentManager, "BLACKLIST ADD DIALOG")
                    }
                    3 -> {
                        val dialog = AddSubscribeItemDialog()
                        AddSubscribeItemDialog.type = SubscribeItem.TYPE_AUTHOR
                        AddSubscribeItemDialog.value = foundEntity.author
                        dialog.showNow(requireActivity().supportFragmentManager, "SUBSCRIBE ADD DIALOG")
                    }
                    4 -> {
                        val dialog = AddBookmarkDialog()
                        if(foundEntity.authors.isNotEmpty()){
                            AddBookmarkDialog.link = foundEntity.authors[0].link ?: ""
                            AddBookmarkDialog.bookmarkReservedName = foundEntity.authors[0].name ?: ""
                            dialog.showNow(requireActivity().supportFragmentManager, "ADD BOOKMARK DIALOG")
                        }
                        else{
                            Toast.makeText(requireContext(), getString(R.string.no_author_title), Toast.LENGTH_SHORT).show()
                        }
                    }
                    5 -> {
                        val dialog = AddBlacklistItemDialog()
                        AddBlacklistItemDialog.type = BlacklistItem.TYPE_GENRE
                        AddBlacklistItemDialog.value = foundEntity.genreComplex
                        dialog.showNow(requireActivity().supportFragmentManager, "BLACKLIST ADD DIALOG")
                    }
                    6 -> {
                        val dialog = AddSubscribeItemDialog()
                        AddSubscribeItemDialog.type = SubscribeItem.TYPE_GENRE
                        AddSubscribeItemDialog.value = foundEntity.genreComplex
                        dialog.showNow(requireActivity().supportFragmentManager, "SUBSCRIBE ADD DIALOG")
                    }

                    7 -> {
                        if(foundEntity.sequencesComplex.isNotEmpty()){
                            val dialog = AddBlacklistItemDialog()
                            AddBlacklistItemDialog.type = BlacklistItem.TYPE_SEQUENCE
                            AddBlacklistItemDialog.value = foundEntity.sequencesComplex
                            dialog.showNow(requireActivity().supportFragmentManager, "BLACKLIST ADD DIALOG")
                        }
                        else{
                            Toast.makeText(requireContext(), getString(R.string.no_sequence_title), Toast.LENGTH_SHORT).show()
                        }
                    }
                    8 -> {
                        if(foundEntity.sequencesComplex.isNotEmpty()) {
                            val dialog = AddSubscribeItemDialog()
                            AddSubscribeItemDialog.type = SubscribeItem.TYPE_SEQUENCE
                            AddSubscribeItemDialog.value = foundEntity.sequencesComplex
                            dialog.showNow(
                                requireActivity().supportFragmentManager,
                                "SUBSCRIBE ADD DIALOG"
                            )
                        }
                        else{
                            Toast.makeText(requireContext(), getString(R.string.no_sequence_title), Toast.LENGTH_SHORT).show()
                        }
                    }
                    9 -> {
                        if(foundEntity.sequences.isNotEmpty()){
                            val dialog = AddBookmarkDialog()
                            AddBookmarkDialog.link = foundEntity.sequences[0].link ?: ""
                            AddBookmarkDialog.bookmarkReservedName = foundEntity.sequences[0].name ?: ""
                            dialog.showNow(requireActivity().supportFragmentManager, "ADD BOOKMARK DIALOG")
                        }
                        else{
                            Toast.makeText(requireContext(), getString(R.string.no_sequence_title), Toast.LENGTH_SHORT).show()
                        }
                    }
                    10 -> {
                            val dialog = AddBlacklistItemDialog()
                            AddBlacklistItemDialog.type = BlacklistItem.TYPE_BOOK
                            AddBlacklistItemDialog.value = foundEntity.name
                            dialog.showNow(requireActivity().supportFragmentManager, "BLACKLIST ADD DIALOG")
                    }
                    11 -> {
                            val dialog = AddSubscribeItemDialog()
                            AddSubscribeItemDialog.type = SubscribeItem.TYPE_BOOK
                            AddSubscribeItemDialog.value = foundEntity.name
                            dialog.showNow(
                                requireActivity().supportFragmentManager,
                                "SUBSCRIBE ADD DIALOG"
                            )
                    }
                    12 -> {
                        val fragment = (activity as SearchActivity?)?.getCurrentFragment()
                        if(fragment is OpdsFragment){
                            foundEntity.downloaded = !foundEntity.downloaded
                            fragment.rightButtonPressed(foundEntity)
                        }
                    }
                    13 -> {
                        val fragment = (activity as SearchActivity?)?.getCurrentFragment()
                        if(fragment is OpdsFragment){
                            foundEntity.read = !foundEntity.read
                            fragment.leftButtonPressed(foundEntity)
                        }
                    }
                }
            }
        }
    }

    companion object {
        lateinit var foundEntity: FoundEntity
    }
}