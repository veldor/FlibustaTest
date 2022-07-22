package net.veldor.flibusta_test.model.handler

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.format.Formatter
import android.text.style.BackgroundColorSpan
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.selections.opds.FoundEntity
import java.util.*

object GrammarHandler {

    fun getAuthorLastName(authorName: String?): String {
        if (authorName != null && authorName.isNotEmpty()) {
            val delimiter = authorName.indexOf(" ")
            return if (delimiter >= 0) {
                authorName.substring(0, delimiter)
            } else {
                authorName
            }
        }
        return "Автор неизвестен"
    }

    fun getColoredString(mString: String?, colorId: Int, context: Context): Spannable {
        val spannable: Spannable = SpannableString(mString)
        if (PreferencesHandler.instance.isEInk) {
            spannable.setSpan(
                BackgroundColorSpan(
                    ResourcesCompat.getColor(
                        context.resources,
                        R.color.invertable_black,
                        context.theme
                    )
                ),
                0,
                spannable.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else {
            spannable.setSpan(
                BackgroundColorSpan(colorId),
                0,
                spannable.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }

    fun isValidUrl(newValue: String): Boolean {
        return "^https?://.+\$".toRegex().matches(newValue)
    }

    @kotlin.jvm.JvmStatic
    fun clearDirName(dirName: String): String {
        return Regex("[^\\d\\w ]").replace(dirName, "")
    }

    @kotlin.jvm.JvmStatic
    fun createAuthorDirName(author: FoundEntity): String {
        val dirname: String = author.name!!
        return if (dirname.length > 100) {
            dirname.substring(0, 100).trim()
        } else dirname.trim()
    }

    @kotlin.jvm.JvmStatic
    val random: Int
        get() {
            val min = 1000
            val max = 9999
            val r = Random()
            return r.nextInt(max - min + 1) + min
        }

    @kotlin.jvm.JvmStatic
    val longRandom: Int
        get() {
            val min = 100000000
            val max = 999999999
            val r = Random()
            return r.nextInt(max - min + 1) + min
        }


    @kotlin.jvm.JvmStatic
    fun getAvailableDownloadFormats(item: FoundEntity, view: TextView) {
        view.text = ""
        if (item.downloadLinks.isEmpty()) {
            view.text = getColoredString("Не найдены ссылки для загрузки", Color.RED, view.context)
        }
        item.downloadLinks.forEach {
            if (it.mime != null) {
                if (it.mime!!.contains("fb2")) {
                    view.append(getColoredString(" FB2 ", Color.parseColor("#FFE91E63"), view.context))
                    view.append(" ")
                } else if (it.mime!!.contains("mobi")) {
                    view.append(getColoredString(" MOBI ", Color.parseColor("#FF9C27B0"), view.context))
                    view.append(" ")
                } else if (it.mime!!.contains("epub")) {
                    view.append(getColoredString(" EPUB ", Color.parseColor("#FF673AB7"), view.context))
                    view.append(" ")
                } else if (it.mime!!.contains("pdf")) {
                    view.append(getColoredString(" PDF ", Color.parseColor("#FF3F51B5"), view.context))
                    view.append(" ")
                } else if (it.mime!!.contains("txt")) {
                    view.append(getColoredString(" TXT ", Color.parseColor("#FF2196F3"), view.context))
                    view.append(" ")
                } else if (it.mime!!.contains("html")) {
                    view.append(getColoredString(" HTML ", Color.parseColor("#FF009688"), view.context))
                    view.append(" ")
                } else if (it.mime!!.contains("doc")) {
                    view.append(getColoredString(" DOC ", Color.parseColor("#FF4CAF50"), view.context))
                    view.append(" ")
                } else if (it.mime!!.contains("djvu")) {
                    view.append(getColoredString(" DJVU ", Color.parseColor("#FFFF9800"), view.context))
                    view.append(" ")
                } else if (it.mime!!.contains("rtf")) {
                    view.append(getColoredString(" RTF ", Color.parseColor("#030303"), view.context))
                    view.append(" ")
                } else {
                    view.append(getColoredString(it.mime!!, Color.parseColor("#030303"), view.context))
                    view.append(" ")
                }
            }
        }
    }

    fun getBookIdentifierFromLink(url: String?): String {
        val result = url?.filter { it.isDigit() }
        return result ?: ""
    }

    fun getTextSize(size: Long): String {
        return Formatter.formatFileSize(
            App.instance,
            size
        )
    }

    fun clearBookName(name: String): String {
        return name.replace("_", " ")
    }

    fun isValidBridges(newValue: String): Boolean {
        return true
    }
}