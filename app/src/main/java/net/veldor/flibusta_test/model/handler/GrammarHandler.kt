package net.veldor.flibusta_test.model.handler

import android.content.Context
import android.text.format.Formatter
import android.util.Base64OutputStream
import android.widget.Button
import androidx.core.content.res.ResourcesCompat
import androidx.documentfile.provider.DocumentFile
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.selection.FoundEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.CharacterIterator
import java.text.SimpleDateFormat
import java.text.StringCharacterIterator
import java.util.*
import kotlin.math.abs

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

    fun humanReadableByteCountBin(bytes: Long): String {
        val absB = if (bytes == Long.MIN_VALUE) Long.MAX_VALUE else abs(bytes)
        if (absB < 1024) {
            return "$bytes b"
        }
        var value = absB
        val ci: CharacterIterator = StringCharacterIterator("KMGTPE")
        var i = 40
        while (i >= 0 && absB > 0xfffccccccccccccL shr i) {
            value = value shr 10
            ci.next()
            i -= 10
        }
        value *= java.lang.Long.signum(bytes).toLong()
        return String.format("%.1f %cb", value / 1024.0, ci.current())
    }

    fun clearDirName(dirName: String): String {
        return Regex("[^\\d\\w ]").replace(dirName, "")
    }

    fun createAuthorDirName(author: FoundEntity): String {
        val dirname: String = author.name!!
        return if (dirname.length > 100) {
            dirname.substring(0, 100).trim()
        } else dirname.trim()
    }

    val random: Int
        get() {
            val min = 1000
            val max = 9999
            val r = Random()
            return r.nextInt(max - min + 1) + min
        }

    val longRandom: Int
        get() {
            val min = 100000000
            val max = 999999999
            val r = Random()
            return r.nextInt(max - min + 1) + min
        }

    fun colorizeFormatButton(button: Button, mime: String) {
        when (mime) {
            "application/fb2+zip", "application/fb2" -> {
                button.setBackgroundColor(
                    ResourcesCompat.getColor(
                        button.context.resources,
                        R.color.fb2_color,
                        button.context.theme
                    )
                )
            }
            "application/html+zip", "application/html" -> {
                button.setBackgroundColor(
                    ResourcesCompat.getColor(
                        button.context.resources,
                        R.color.html_color,
                        button.context.theme
                    )
                )
            }
            "application/txt+zip", "application/txt" -> {
                button.setBackgroundColor(
                    ResourcesCompat.getColor(
                        button.context.resources,
                        R.color.txt_color,
                        button.context.theme
                    )
                )
            }
            "application/rtf+zip", "application/rtf" -> {
                button.setBackgroundColor(
                    ResourcesCompat.getColor(
                        button.context.resources,
                        R.color.rtf_color,
                        button.context.theme
                    )
                )
            }

            "application/epub+zip", "application/epub" -> {
                button.setBackgroundColor(
                    ResourcesCompat.getColor(
                        button.context.resources,
                        R.color.epub_color,
                        button.context.theme
                    )
                )
            }
            "application/x-mobipocket-ebook" -> {
                button.setBackgroundColor(
                    ResourcesCompat.getColor(
                        button.context.resources,
                        R.color.mobi_color,
                        button.context.theme
                    )
                )
            }
            "application/djvu" -> {
                button.setBackgroundColor(
                    ResourcesCompat.getColor(
                        button.context.resources,
                        R.color.djvu_color,
                        button.context.theme
                    )
                )
            }
            "application/pdf" -> {
                button.setBackgroundColor(
                    ResourcesCompat.getColor(
                        button.context.resources,
                        R.color.pdf_color,
                        button.context.theme
                    )
                )
            }
            "application/pdf" -> {
                button.setBackgroundColor(
                    ResourcesCompat.getColor(
                        button.context.resources,
                        R.color.pdf_color,
                        button.context.theme
                    )
                )
            }
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> {
                button.setBackgroundColor(
                    ResourcesCompat.getColor(
                        button.context.resources,
                        R.color.docx_color,
                        button.context.theme
                    )
                )
            }
            "application/msword" -> {
                button.setBackgroundColor(
                    ResourcesCompat.getColor(
                        button.context.resources,
                        R.color.doc_color,
                        button.context.theme
                    )
                )
            }
            "application/vnd.ms-htmlhelp" -> {
                button.setBackgroundColor(
                    ResourcesCompat.getColor(
                        button.context.resources,
                        R.color.chm_color,
                        button.context.theme
                    )
                )
            }
            "application/prc" -> {
                button.setBackgroundColor(
                    ResourcesCompat.getColor(
                        button.context.resources,
                        R.color.prc_color,
                        button.context.theme
                    )
                )
            }
        }
    }

    fun getBookIdentifierFromLink(url: String?): String {
        val result = url?.replace("fb2", "")?.filter { it.isDigit() }
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

    fun timestampToDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy hh:mm", Locale.ENGLISH)
        val netDate = Date(timestamp)
        return sdf.format(netDate)
    }

    fun toBase64(context: Context, file: DocumentFile): String {
        return ByteArrayOutputStream().use { outputStream ->
            Base64OutputStream(outputStream, android.util.Base64.DEFAULT).use { base64FilterStream ->
                context.contentResolver.openInputStream(file.uri).use { inputStream ->
                    inputStream?.copyTo(base64FilterStream)
                }
            }
            return@use outputStream.toString()
        }
    }

    fun toBase64(file: File): String {
        return ByteArrayOutputStream().use { outputStream ->
            Base64OutputStream(outputStream, android.util.Base64.DEFAULT).use { base64FilterStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(base64FilterStream)
                }
            }
            return@use outputStream.toString()
        }
    }

    fun explodeFile(encodedFile: String, maxSendViaSocketSize: Int): ArrayList<String> {
        val result: ArrayList<String> = arrayListOf()
        var offset = 0
        while (true){
            if (offset + maxSendViaSocketSize < encodedFile.length){
                result.add(encodedFile.substring(offset, offset + maxSendViaSocketSize))
                offset += maxSendViaSocketSize
            }
            else{
                result.add(encodedFile.substring(offset))
                break
            }
        }
        return result
    }

}