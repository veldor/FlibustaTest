package net.veldor.flibusta_test.model.selections.opds

import android.util.Log
import androidx.room.Ignore
import net.veldor.flibusta_test.model.handler.FormatHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.helper.MimeHelper
import net.veldor.flibusta_test.model.selections.DownloadLink
import net.veldor.flibusta_test.model.selections.blacklist.FilteringResult
import java.io.File
import java.io.Serializable
import kotlin.random.Random

class FoundEntity : Serializable {
    fun getFavoriteLink(): DownloadLink? {
        if (downloadLinks.isNotEmpty()) {
            val favorite = PreferencesHandler.instance.favoriteFormat
            Log.d("surprise", "FoundEntity.kt 14: $favorite")
            if (favorite != null) {
                downloadLinks.forEach {
                    Log.d("surprise", "FoundEntity.kt 18: ${it.mime}")
                    if (FormatHandler.isSame(it.mime, favorite)) {
                        return it
                    }
                }
            }
            return downloadLinks[0]
        }
        return null
    }

    @Ignore
    var systemId: String? = null
    val itemId: Long = Random.nextLong(1, Long.MAX_VALUE)
    var filterResult: FilteringResult? = null
    var content: String = ""
    var selected: Boolean = false
    var buttonPressed: Boolean = false
    var description: String = ""
    var language: String = ""
    var coverUrl: String? = null
    var type: String? = null
    var id: String? = null
    var link: String? = null
    var name: String? = null
    var author: String? = null
    var downloadsCount: String? = null
    var translate: String? = null
    var size: String? = null
    var format: String? = null
    val downloadLinks = ArrayList<DownloadLink>()
    var selectedLink: DownloadLink? = null
    val genres = ArrayList<FoundEntity>()
    var genreComplex: String? = null
    val sequences = ArrayList<FoundEntity>()
    val authors = ArrayList<FoundEntity>()
    var sequencesComplex: String = ""
    var cover: File? = null

    var read = false
    var downloaded = false

    // год выпуска
    var publicationYear = ""

    // время выкладки на сайте
    var publishTime = ""
}