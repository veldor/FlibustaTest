package net.veldor.flibusta_test.view.components

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.SetupDownloadDialogBinding
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.helper.MimeHelper
import net.veldor.flibusta_test.model.helper.UrlHelper
import net.veldor.flibusta_test.model.interfaces.BookCheckInterface
import net.veldor.flibusta_test.model.selection.DownloadLink
import net.veldor.flibusta_test.model.selection.FoundEntity
import net.veldor.flibusta_test.view.DownloadBookSetupActivity
import net.veldor.flibusta_test.view.SearchActivity
import java.util.*

class BookDownloadSetupDialog : DialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { a ->
            if (book != null) {
                val fragment = (activity as SearchActivity?)?.getCurrentFragment()
                book!!.editedName = null
                book!!.downloadLinks.forEach {
                    it.editedName = null
                }
                val binding = SetupDownloadDialogBinding.inflate(layoutInflater)
                binding.bookNameView.setText(UrlHelper.getBookNameWithoutExtension(book!!))
                binding.bookNameView.doOnTextChanged { text, _, _, _ ->
                    book!!.editedName = text.toString()
                    book!!.downloadLinks.forEach {
                        it.editedName = text.toString()
                    }
                    binding.resultPathToBook.text =
                        UrlHelper.getDownloadedBookPath(selectedLink!!, true)
                }
                binding.changeDownloadDirBtn.setOnClickListener {
                    (fragment as BookCheckInterface).selectDownloadDir {
                        binding.pathToDownload.text = PreferencesHandler.rootDownloadDirPath
                        binding.resultPathToBook.text =
                            UrlHelper.getDownloadedBookPath(selectedLink!!, true)
                    }
                }
                binding.pathToDownload.text = PreferencesHandler.rootDownloadDirPath
                book!!.downloadLinks.forEach {
                    val radio = layoutInflater.inflate(
                        R.layout.radio_button,
                        binding.typeSelectRadioContainer,
                        false
                    )
                    (radio as RadioButton).text = MimeHelper.getDownloadMime(it.mime!!)
                    binding.typeSelectRadioContainer.addView(radio)
                    if (firstLinkSelected && selectedLink == it) {
                        radio.isChecked = true
                        (fragment as BookCheckInterface).checkBookAvailability(
                            it
                        ) { status: String ->
                            activity?.runOnUiThread {
                                binding.bookAvailabilityView.text = status
                                binding.resultPathToBook.text =
                                    UrlHelper.getDownloadedBookPath(selectedLink!!, true)
                            }
                        }
                    }
                    radio.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            selectedLink = it
                            binding.bookAvailabilityView.text = String.format(
                                Locale.ENGLISH,
                                getString(R.string.checking_format_template),
                                MimeHelper.getDownloadMime(it.mime!!)
                            )
                            (fragment as BookCheckInterface).checkBookAvailability(
                                it
                            ) { status: String ->
                                activity?.runOnUiThread {
                                    binding.bookAvailabilityView.text = status
                                    binding.resultPathToBook.text =
                                        UrlHelper.getDownloadedBookPath(selectedLink!!, true)
                                }
                            }
                        }
                    }
                    if (!firstLinkSelected) {
                        radio.isChecked = true
                        firstLinkSelected = true
                    }
                }
                return AlertDialog.Builder(a, R.style.dialogTheme)
                    .setView(binding.root)
                    .setPositiveButton(R.string.download_title) { _, _ ->
                        if (selectedLink == null) {
                            Toast.makeText(
                                requireContext(),
                                R.string.no_download_format_selected,
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            (fragment as BookCheckInterface).showBookDownloadOptions(book!!)
                            return@setPositiveButton
                        }
                        (fragment as BookCheckInterface).addToDownloadQueue(
                            selectedLink!!
                        )
                    }
                    .setNeutralButton(getString(R.string.show_extended_options_title)) { _, _ ->
                        val intent = Intent(requireContext(), DownloadBookSetupActivity::class.java)
                        intent.putExtra("EXTRA_BOOK", book)
                        startActivity(intent)
                    }
                    .create()
            }
            val builder = AlertDialog.Builder(a, R.style.dialogTheme)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")

    }

    fun setup(b: FoundEntity) {
        book = b
        firstLinkSelected = false
    }

    fun setup(bookInfo: FoundEntity, link: String) {
        book = bookInfo
        firstLinkSelected = true
        bookInfo.downloadLinks.forEach {
            if (it.url == link) {
                selectedLink = it
                return@forEach
            }
        }
    }

    companion object {
        var book: FoundEntity? = null
        var selectedLink: DownloadLink? = null
        var firstLinkSelected = false
    }
}