package net.veldor.flibusta_test.model.handler

import android.content.Context
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_AUTHOR
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_BOOK
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_GENRE
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_SEQUENCE
import net.veldor.flibusta_test.model.selections.FileItem
import net.veldor.flibusta_test.model.selections.SortOption
import net.veldor.flibusta_test.model.selections.opds.FoundEntity

class SortHandler {
    fun getBookSortOptions(context: Context): List<SortOption?> {
        return listOf(
            SortOption(-1, context.getString(R.string.sort_option_default)),
            SortOption(0, context.getString(R.string.sort_option_name)),
            SortOption(11, context.getString(R.string.sort_option_name_reverse)),
            SortOption(1, context.getString(R.string.sort_option_size)),
            SortOption(12, context.getString(R.string.sort_option_size_reverse)),
            SortOption(2, context.getString(R.string.sort_option_downloads)),
            SortOption(13, context.getString(R.string.sort_option_downloads_reverse)),
            SortOption(3, context.getString(R.string.sort_option_sequence)),
            SortOption(14, context.getString(R.string.sort_option_sequence_reverse)),
            SortOption(4, context.getString(R.string.sort_option_genre)),
            SortOption(15, context.getString(R.string.sort_option_genre_reverse)),
            SortOption(5, context.getString(R.string.sort_option_author_name)),
            SortOption(16, context.getString(R.string.sort_option_author_name_reverse)),
            SortOption(6, context.getString(R.string.sort_option_is_loaded)),
            SortOption(17, context.getString(R.string.sort_option_is_loaded_reverse)),
            SortOption(7, context.getString(R.string.sort_option_is_read)),
            SortOption(18, context.getString(R.string.sort_option_is_read_reverse)),
            SortOption(8, context.getString(R.string.sort_option_format)),
            SortOption(19, context.getString(R.string.sort_option_format_reverse)),
            SortOption(9, context.getString(R.string.sort_option_pub_year)),
            SortOption(20, context.getString(R.string.sort_option_pub_year_reverse)),
            SortOption(10, context.getString(R.string.sort_option_add_to_site)),
            SortOption(21, context.getString(R.string.sort_option_add_to_site)),
            SortOption(22, context.getString(R.string.sort_option_translator)),
            SortOption(23, context.getString(R.string.sort_option_translator_reverse)),
        )
    }

    fun getAuthorSortOptions(context: Context): List<SortOption?> {
        return listOf(
            SortOption(-1, context.getString(R.string.sort_option_default)),
            SortOption(0, context.getString(R.string.sort_option_author_name)),
            SortOption(1, context.getString(R.string.sort_option_author_name_reverse)),
            SortOption(2, context.getString(R.string.sort_option_books_count)),
            SortOption(3, context.getString(R.string.sort_option_books_count_reverse)),
        )
    }

    fun getDefaultSortOptions(context: Context): List<SortOption?> {
        return listOf(
            SortOption(-1, context.getString(R.string.sort_option_default)),
            SortOption(0, context.getString(R.string.sort_option_parameter_name)),
            SortOption(1, context.getString(R.string.sort_option_parameter_name_reverse)),
        )
    }

    fun sortItems(list: ArrayList<FoundEntity>) {
        list.sortWith sort@{ lhs: FoundEntity, rhs: FoundEntity ->
            val result: Int = if (lhs.type == TYPE_BOOK && rhs.type == TYPE_BOOK) {
                compareBooks(lhs, rhs)
            } else if (lhs.type == TYPE_AUTHOR && rhs.type == TYPE_AUTHOR) {
                compareAuthors(lhs, rhs)
            } else if (lhs.type == TYPE_GENRE && rhs.type == TYPE_GENRE) {
                compareGenres(lhs, rhs)
            } else if (lhs.type == TYPE_SEQUENCE && rhs.type == TYPE_SEQUENCE) {
                compareSequences(lhs, rhs)
            } else {
                compareByName(lhs, rhs)
            }
            return@sort result
        }
    }

    private fun compareBooks(lhs: FoundEntity, rhs: FoundEntity): Int {
        when (SelectedSortTypeHandler.instance.getBookSortOption()) {
            0 -> return compareStrings(lhs.name, rhs.name, false)
            11 -> return compareStrings(lhs.name, rhs.name, true)
            1 -> return compareIntValues(lhs.size, rhs.size, false)
            12 -> return compareIntValues(lhs.size, rhs.size, true)
            2 -> return compareIntValues(lhs.downloadsCount, rhs.downloadsCount, false)
            13 -> return compareIntValues(lhs.downloadsCount, rhs.downloadsCount, true)
            3 -> return compareStrings(lhs.sequencesComplex, rhs.sequencesComplex, false)
            14 -> return compareStrings(lhs.sequencesComplex, rhs.sequencesComplex, true)
            4 -> return compareStrings(lhs.genreComplex, rhs.genreComplex, false)
            15 -> return compareStrings(lhs.genreComplex, rhs.genreComplex, true)
            5 -> return compareStrings(lhs.author, rhs.author, false)
            16 -> return compareStrings(lhs.author, rhs.author, true)
            6 -> return compareBooleans(lhs.downloaded, rhs.downloaded, false)
            17 -> return compareBooleans(lhs.downloaded, rhs.downloaded, true)
            7 -> return compareBooleans(lhs.read, rhs.read, false)
            18 -> return compareBooleans(lhs.read, rhs.read, true)
            8 -> return compareStrings(lhs.format, rhs.format, false)
            19 -> return compareStrings(lhs.format, rhs.format, true)
            9 -> return compareIntValues(lhs.publicationYear, rhs.publicationYear, false)
            20 -> return compareIntValues(lhs.publicationYear, rhs.publicationYear, true)
            10 -> return compareIntValues(lhs.publishTime, rhs.publishTime, false)
            21 -> return compareIntValues(lhs.publishTime, rhs.publishTime, true)
            22 -> return compareStrings(lhs.translate, rhs.translate, false)
            23 -> return compareIntValues(lhs.translate, rhs.translate, true)
        }
        return 1
    }

    private fun compareAuthors(lhs: FoundEntity, rhs: FoundEntity): Int {

        when (SelectedSortTypeHandler.instance.getAuthorSortOption()) {
            0 -> return compareStrings(lhs.name, rhs.name, false)
            1 -> return compareStrings(lhs.name, rhs.name, true)
            2 -> return compareIntValues(lhs.content, rhs.content, false)
            3 -> return compareIntValues(lhs.content, rhs.content, true)
        }
        return 1
    }

    private fun compareGenres(lhs: FoundEntity, rhs: FoundEntity): Int {
        when (SelectedSortTypeHandler.instance.getGenreSortOption()) {
            0 -> compareStrings(lhs.name, rhs.name, false)
            1 -> compareStrings(lhs.name, rhs.name, true)
        }
        return 1
    }

    private fun compareSequences(lhs: FoundEntity, rhs: FoundEntity): Int {
        when (SelectedSortTypeHandler.instance.getSequenceSortOption()) {
            0 -> return compareStrings(lhs.name, rhs.name, false)
            1 -> return compareStrings(lhs.name, rhs.name, true)
        }
        return 1
    }

    private fun compareByName(lhs: FoundEntity, rhs: FoundEntity): Int {
        return compareStrings(lhs.name, rhs.name, false)
    }


    private fun compareBooleans(lhs: Boolean, rhs: Boolean, invert: Boolean): Int {
        if (lhs == rhs) return 0
        if (invert) {
            return if (lhs) 1 else -1
        }
        return if (rhs) 1 else -1
    }

    private fun compareStrings(lhs: String?, rhs: String?, invert: Boolean): Int {
        if (lhs == rhs) return 0
        if (lhs == null) {
            return 1
        }
        if (rhs == null) {
            return -1
        }
        if (invert)
            return rhs.compareTo(lhs)
        return lhs.compareTo(rhs)
    }

    private fun compareIntValues(lhs: String?, rhs: String?, invert: Boolean): Int {
        var lhsValue = lhs?.filter { it.isDigit() }?.toIntOrNull()
        var rhsValue = rhs?.filter { it.isDigit() }?.toIntOrNull()
        if (lhsValue == null) {
            lhsValue = 0
        }
        if (rhsValue == null) {
            rhsValue = 0
        }
        if (lhsValue == rhsValue) return 0
        if (invert) {
            return if (rhsValue < lhsValue) 1 else -1
        }
        return if (lhsValue < rhsValue) 1 else -1
    }

    fun sortFiles(arrayList: ArrayList<FileItem>, which: Int) {
        when (which) {
            0 -> {
                // name
                arrayList.sortWith sort@{ lhs: FileItem, rhs: FileItem ->
                    if (lhs.file.isDirectory && rhs.file.isFile) {
                        return@sort -1
                    }
                    if (rhs.file.isDirectory && lhs.file.isFile) {
                        return@sort 1
                    }
                    return@sort lhs.name.compareTo(rhs.name)
                }
            }
            1 -> {

                // name reverse
                arrayList.sortWith sort@{ lhs: FileItem, rhs: FileItem ->
                    if (lhs.file.isDirectory && rhs.file.isFile) {
                        return@sort -1
                    }
                    if (rhs.file.isDirectory && lhs.file.isFile) {
                        return@sort 1
                    }
                    return@sort rhs.name.compareTo(lhs.name)
                }
            }
            2 -> {
                //size
                arrayList.sortWith sort@{ lhs: FileItem, rhs: FileItem ->
                    if (lhs.file.isDirectory && rhs.file.isFile) {
                        return@sort -1
                    }
                    if (rhs.file.isDirectory && lhs.file.isFile) {
                        return@sort 1
                    }
                    if (rhs.file.length() == lhs.file.length()) {
                        return@sort 0
                    }
                    if (rhs.file.length() > lhs.file.length()) {
                        return@sort 1
                    }
                    return@sort -1
                }
            }

            3 -> {
                //size reverse
                arrayList.sortWith sort@{ lhs: FileItem, rhs: FileItem ->
                    if (lhs.file.isDirectory && rhs.file.isFile) {
                        return@sort -1
                    }
                    if (rhs.file.isDirectory && lhs.file.isFile) {
                        return@sort 1
                    }
                    if (rhs.file.length() == lhs.file.length()) {
                        return@sort 0
                    }
                    if (rhs.file.length() < lhs.file.length()) {
                        return@sort 1
                    }
                    return@sort -1
                }
            }
            4 -> {
                //type
                arrayList.sortWith sort@{ lhs: FileItem, rhs: FileItem ->
                    if (lhs.file.isDirectory && rhs.file.isFile) {
                        return@sort -1
                    }
                    if (rhs.file.isDirectory && lhs.file.isFile) {
                        return@sort 1
                    }
                    return@sort lhs.type.compareTo(rhs.type)
                }
            }
            5 -> {
                //type
                arrayList.sortWith sort@{ lhs: FileItem, rhs: FileItem ->
                    if (lhs.file.isDirectory && rhs.file.isFile) {
                        return@sort -1
                    }
                    if (rhs.file.isDirectory && lhs.file.isFile) {
                        return@sort 1
                    }
                    return@sort rhs.type.compareTo(lhs.type)
                }
            }
            6 -> {
                arrayList.sortWith sort@{ lhs: FileItem, rhs: FileItem ->
                    if (lhs.file.lastModified() > rhs.file.lastModified()) {
                        return@sort -1
                    }
                    if (lhs.file.lastModified() < rhs.file.lastModified()) {
                        return@sort 1
                    }
                    return@sort 0
                }
            }
            7 -> {
                arrayList.sortWith sort@{ lhs: FileItem, rhs: FileItem ->
                    if (lhs.file.lastModified() < rhs.file.lastModified()) {
                        return@sort -1
                    }
                    if (lhs.file.lastModified() > rhs.file.lastModified()) {
                        return@sort 1
                    }
                    return@sort 0
                }
            }
        }
    }
}