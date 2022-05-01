package net.veldor.flibusta_test.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityDownloadBookSetupBinding
import net.veldor.flibusta_test.model.adapter.FormatAdapter
import net.veldor.flibusta_test.model.components.FormatSpinner
import net.veldor.flibusta_test.model.delegate.FormatAvailabilityCheckDelegate
import net.veldor.flibusta_test.model.delegate.PictureLoadedDelegate
import net.veldor.flibusta_test.model.handler.FormatHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.helper.UrlHelper
import net.veldor.flibusta_test.model.selections.DownloadLink
import net.veldor.flibusta_test.model.selections.opds.FoundEntity
import net.veldor.flibusta_test.model.view_model.OpdsViewModel
import net.veldor.flibusta_test.ui.browser_fragments.DownloadPreferencesFragment
import java.util.*


class DownloadBookSetupActivity : AppCompatActivity(), Preference.OnPreferenceChangeListener,
    PictureLoadedDelegate, FormatAvailabilityCheckDelegate {
    private lateinit var selectedLink: DownloadLink
    private lateinit var sortShowSpinner: FormatSpinner
    private var book: FoundEntity? = null
    private lateinit var binding: ActivityDownloadBookSetupBinding
    private lateinit var viewModel: OpdsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(OpdsViewModel::class.java)
        viewModel.setFormatDelegate(this)
        book = intent.getSerializableExtra("EXTRA_BOOK") as FoundEntity
        if (book == null) {
            finish()
        }
        binding = ActivityDownloadBookSetupBinding.inflate(layoutInflater)
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as DownloadPreferencesFragment).prefChangedDelegate =
            this
        setupUI()
        setContentView(binding.root)
    }

    private fun setupUI() {
        binding.bookName.text = book?.name
        when {
            book?.cover != null -> {
                binding.previewImage.setImageBitmap(BitmapFactory.decodeFile(book!!.cover!!.path))
            }
            book?.coverUrl != null -> {
                viewModel.downloadPic(book!!, this)
            }
            else -> {
                binding.previewImage.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        App.instance.resources,
                        R.drawable.no_cover,
                        null
                    )
                )
            }
        }
        sortShowSpinner = binding.sortShowSpinner
        if (PreferencesHandler.instance.rememberFavoriteFormat && PreferencesHandler.instance.favoriteFormat != null) {
            Log.d("surprise", "setupUI: select with favorite format ${PreferencesHandler.instance.favoriteFormat}")
            sortShowSpinner.setSortList(
                book!!.downloadLinks,
                PreferencesHandler.instance.favoriteFormat
            )
        } else {
            sortShowSpinner.setSortList(
                book!!.downloadLinks,
                null
            )
        }
        sortShowSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?
                , pos: Int,
                id: Long
            ) {
                binding.formatAvailability.text = getString(R.string.checking_availability_title)
                binding.formatAvailability.setTextColor(
                    ResourcesCompat.getColor(
                        App.instance.resources,
                        R.color.black,
                        null
                    )
                )
                val selectedValue = (parent.adapter as FormatAdapter).getItem(pos) as String
                book?.downloadLinks?.forEach {
                    if(FormatHandler.isSame(it.mime, selectedValue)){
                        selectedLink = it
                        return@forEach
                    }
                }
                viewModel.checkFormatAvailability(selectedLink)
                getDownloadedFileName()
                if (sortShowSpinner.notFirstSelection) {
                    (parent.adapter as FormatAdapter).setSelection(binding.sortShowSpinner.getItemAtPosition(pos) as String)
                    if (PreferencesHandler.instance.rememberFavoriteFormat) {
                        Log.d("surprise", "onItemSelected: save here")
                        PreferencesHandler.instance.favoriteFormat =
                            FormatHandler.getShortFromFullMimeWithoutZip(selectedLink.mime)
                        Log.d("surprise", "onItemSelected: saved ${PreferencesHandler.instance.favoriteFormat}")
                    }
                }
                sortShowSpinner.notifySelection()
            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {
                Log.d("surprise", "TimetableFragment onNothingSelected 54: nothing selected")
            }
        }
        binding.startDownloadButton.setOnClickListener {
            // download book in current format
            viewModel.addToDownloadQueue(selectedLink)
            if (PreferencesHandler.instance.rememberFavoriteFormat) {
                Log.d("surprise", "setupUI: save here")
                PreferencesHandler.instance.favoriteFormat = FormatHandler.getShortFromFullMimeWithoutZip(selectedLink.mime!!)
            }
            finish()
        }
    }

    private fun getDownloadedFileName() {
        binding.pathToFile.text = UrlHelper.getDownloadedBookPath(selectedLink, true)
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        Handler().postDelayed({
            getDownloadedFileName()
        }, 500)
        return true
    }

    override fun pictureLoaded() {
        runOnUiThread {
            try {
                binding.previewImage.setImageBitmap(BitmapFactory.decodeFile(book!!.cover!!.path))
            } catch (_: Throwable) {

            }
        }
    }

    override fun formatAvailable(size: String?) {
        runOnUiThread {
            binding.formatAvailability.text =
                String.format(Locale.ENGLISH, getString(R.string.format_available_pattern), size)
            binding.formatAvailability.setTextColor(
                ResourcesCompat.getColor(
                    App.instance.resources,
                    R.color.genre_text_color,
                    null
                )
            )
        }
    }

    override fun formatUnavailable() {
        runOnUiThread {
            binding.formatAvailability.text = getString(R.string.format_not_available_title)
            binding.formatAvailability.setTextColor(
                ResourcesCompat.getColor(
                    App.instance.resources,
                    R.color.book_name_color,
                    null
                )
            )
        }
    }
}