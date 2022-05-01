package net.veldor.flibusta_test.model.selections.opds

import net.veldor.flibusta_test.model.selections.DownloadLink
import net.veldor.flibusta_test.model.selections.blacklist.FilteringResult
import java.io.File
import java.io.Serializable
import kotlin.random.Random

class FoundEntity : Serializable {
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