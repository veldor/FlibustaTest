package net.veldor.flibusta_test.view

import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.ActivityDownloadBookSetupBinding
import net.veldor.flibusta_test.model.delegate.PictureLoadedDelegate
import net.veldor.flibusta_test.model.handler.FormatHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.helper.MimeHelper
import net.veldor.flibusta_test.model.helper.UrlHelper
import net.veldor.flibusta_test.model.selection.DownloadLink
import net.veldor.flibusta_test.model.selection.FoundEntity
import net.veldor.flibusta_test.model.view_model.OpdsViewModel
import net.veldor.flibusta_test.view.search_fragment.DownloadPreferencesFragment
import java.util.*


class DownloadBookSetupActivity : AppCompatActivity(), Preference.OnPreferenceChangeListener,
    PictureLoadedDelegate {
    private var selectedLink: DownloadLink? = null
    private lateinit var book: FoundEntity
    private lateinit var binding: ActivityDownloadBookSetupBinding
    private lateinit var viewModel: OpdsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[OpdsViewModel::class.java]
        book = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("EXTRA_BOOK", FoundEntity::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("EXTRA_BOOK") as FoundEntity
        }

        binding = ActivityDownloadBookSetupBinding.inflate(layoutInflater)
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as DownloadPreferencesFragment).prefChangedDelegate =
            this
        setupUI()
        setContentView(binding.root)
    }

    private fun setupUI() {

        var firstLinkSelected = false
        book.downloadLinks.forEach {
            val radio = layoutInflater.inflate(
                R.layout.radio_button,
                binding.typeSelectRadioContainer,
                false
            )
            (radio as RadioButton).text = MimeHelper.getDownloadMime(it.mime!!)
            binding.typeSelectRadioContainer.addView(radio)
            radio.setOnCheckedChangeListener { _, isChecked ->
                binding.formatAvailabilityProgress.visibility = View.VISIBLE
                if (isChecked) {
                    selectedLink = it
                    binding.formatAvailability.text = String.format(
                        Locale.ENGLISH,
                        getString(R.string.checking_format_template),
                        MimeHelper.getDownloadMime(it.mime!!)
                    )
                    viewModel.checkFormatAvailability(
                        this,
                        it
                    ) { status: String ->
                        runOnUiThread {
                            binding.formatAvailabilityProgress.visibility = View.INVISIBLE
                            binding.formatAvailability.text = status
                        }
                    }
                    binding.bookName.text = String.format(
                        Locale.ENGLISH,
                        "%s.%s",
                        book.name,
                        MimeHelper.getDownloadMime(it.mime!!)
                    )
                }
                binding.pathToDownload.text = UrlHelper.getDownloadedBookPath(selectedLink!!, true)
            }
            if (!firstLinkSelected) {
                radio.isChecked = true
                firstLinkSelected = true
            }
        }

        when {
            book.cover != null -> {
                binding.previewImage.setImageBitmap(BitmapFactory.decodeFile(book.cover!!.path))
            }
            book.coverUrl != null -> {
                viewModel.downloadPic(book, this)
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
        binding.startDownloadButton.setOnClickListener {
            if (selectedLink != null && selectedLink?.id != null) {
                // download book in current format
                viewModel.addToDownloadQueue(selectedLink)
                if (PreferencesHandler.rememberFavoriteFormat) {
                    PreferencesHandler.favoriteFormat =
                        FormatHandler.getShortFromFullMimeWithoutZip(selectedLink!!.mime!!)
                }
                finish()
            } else {
                Toast.makeText(this, getString(R.string.no_download_links_title), Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun getDownloadedFileName() {
        if (selectedLink != null) {
            binding.pathToDownload.text = UrlHelper.getDownloadedBookPath(selectedLink!!, true)
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("surprise", "DownloadBookSetupActivity.kt 155: changing name")
            getDownloadedFileName()
        }, 500)
        return true
    }

    override fun pictureLoaded() {
        runOnUiThread {
            try {
                binding.previewImage.setImageBitmap(BitmapFactory.decodeFile(book.cover!!.path))
            } catch (_: Throwable) {

            }
        }
    }
}