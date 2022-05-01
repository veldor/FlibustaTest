package net.veldor.flibusta_test.model.handler

import android.util.Log
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_AUTHOR
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_AUTHORS
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_BOOK
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_GENRE
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_SEQUENCE
import net.veldor.flibusta_test.model.selections.SortOption
import net.veldor.flibusta_test.model.selections.opds.FoundEntity
import java.util.ArrayList

class SelectedSortTypeHandler private constructor() {
    private var selectedBookSortOption: SortOption? =
        SortHandler().getBookSortOptions(App.instance.applicationContext)[0]
    private var selectedAuthorSortOption: SortOption? =
        SortHandler().getAuthorSortOptions(App.instance.applicationContext)[0]
    private var selectedGenreSortOption: SortOption? =
        SortHandler().getDefaultSortOptions(App.instance.applicationContext)[0]
    private var selectedSequenceSortOption: SortOption? =
        SortHandler().getDefaultSortOptions(App.instance.applicationContext)[0]

    fun saveSortType(sortEntityId: Int, selectedOption: Int) {
        Log.d("surprise", "saveSortType: save option $selectedOption")
        // switch selected sort option
        when (sortEntityId) {
            R.id.searchBook -> {
                Log.d("surprise", "saveSortType: of book sort")
                selectedBookSortOption =
                    SortHandler().getBookSortOptions(App.instance.applicationContext)[selectedOption]
            }
            R.id.searchAuthor -> {
                Log.d("surprise", "saveSortType: of author sort")
                selectedAuthorSortOption =
                    SortHandler().getAuthorSortOptions(App.instance.applicationContext)[selectedOption]
            }
            R.id.searchGenre -> {
                Log.d("surprise", "saveSortType: of genre sort")
                selectedGenreSortOption =
                    SortHandler().getDefaultSortOptions(App.instance.applicationContext)[selectedOption]
            }
            R.id.searchSequence -> {
                Log.d("surprise", "saveSortType: of sequence sort")
                selectedSequenceSortOption =
                    SortHandler().getDefaultSortOptions(App.instance.applicationContext)[selectedOption]
            }
        }
    }

    fun sortRequired(values: ArrayList<FoundEntity>): Boolean {
        values.forEach {
            if (it.type == TYPE_BOOK && requireSortBooks()) {
                return true
            }
            if (it.type == TYPE_AUTHOR && requireSortAuthor()) {
                return true
            }
            if (it.type == TYPE_AUTHORS && requireSortAuthor()) {
                return true
            }
            if (it.type == TYPE_GENRE && requireSortGenre()) {
                return true
            }
            if (it.type == TYPE_SEQUENCE && requireSortSequence()) {
                return true
            }
        }
        return false
    }

    private fun requireSortBooks(): Boolean {
        return selectedBookSortOption!!.id != -1
    }

    private fun requireSortAuthor(): Boolean {
        return selectedAuthorSortOption!!.id != -1
    }

    private fun requireSortGenre(): Boolean {
        return selectedGenreSortOption!!.id != -1
    }

    private fun requireSortSequence(): Boolean {
        return selectedSequenceSortOption!!.id != -1
    }

    fun getGenreSortOption(): Int {
        return selectedGenreSortOption!!.id
    }

    fun getSequenceSortOption(): Int {
        return selectedSequenceSortOption!!.id
    }

    fun getAuthorSortOption(): Int {
        return selectedAuthorSortOption!!.id
    }

    fun getBookSortOption(): Int {
        return selectedBookSortOption!!.id
    }

    fun getBookSortOptionIndex(): Int {
        val options =  SortHandler().getBookSortOptions(App.instance)
                options.forEach {
                    if(it?.id == selectedBookSortOption?.id){
                        return options.indexOf(it)
                    }
                }
        return -1
    }
    fun getAuthorSortOptionIndex(): Int {
        val options =  SortHandler().getAuthorSortOptions(App.instance)
                options.forEach {
                    if(it?.id == selectedAuthorSortOption?.id){
                        return options.indexOf(it)
                    }
                }
        return -1
    }
    fun getGenreSortOptionIndex(): Int {
        val options =  SortHandler().getDefaultSortOptions(App.instance)
                options.forEach {
                    if(it?.id == selectedGenreSortOption?.id){
                        return options.indexOf(it)
                    }
                }
        return -1
    }
    fun getSequenceSortOptionIndex(): Int {
        val options =  SortHandler().getDefaultSortOptions(App.instance)
                options.forEach {
                    if(it?.id == selectedSequenceSortOption?.id){
                        return options.indexOf(it)
                    }
                }
        return -1
    }

    companion object {

        @JvmStatic
        var instance: SelectedSortTypeHandler = SelectedSortTypeHandler()
            private set
    }
}