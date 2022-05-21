package net.veldor.flibusta_test.model.handler

import android.util.Log
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_AUTHOR
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_AUTHORS
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_BOOK
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_GENRE
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_SEQUENCE
import net.veldor.flibusta_test.model.selections.blacklist.*
import net.veldor.flibusta_test.model.selections.opds.FoundEntity

object FilterHandler {
    fun check(foundedEntity: FoundEntity): FilteringResult {
        if (PreferencesHandler.instance.isOpdsUseFilter) {
            var list: ArrayList<BlacklistItem>
            var lowerName: String
            if (foundedEntity.type == TYPE_BOOK) {
                // check for all of blacklists
                if (PreferencesHandler.instance.isOnlyRussian && !foundedEntity.language.contains("ru")) {
                    return FilteringResult(false, null, null, "hideNonRussian")
                }
                list = BlacklistBooks.instance.getBlacklist()
                if (list.isNotEmpty() && !foundedEntity.name.isNullOrEmpty()) {
                    lowerName = foundedEntity.name!!.lowercase().trim()
                    list.forEach {
                        if (it.name.startsWith("*")) {
                            if (lowerName.contains(it.name.substring(1))) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name,
                                    "book name"
                                )
                            }
                        } else {
                            if (lowerName == it.name) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name,
                                    "book name strict"
                                )
                            }
                        }
                    }
                }
                list = BlacklistAuthors.instance.getBlacklist()
                if (list.isNotEmpty() && !foundedEntity.author.isNullOrEmpty()) {
                    foundedEntity.authors.forEach { author ->
                        lowerName = author.name!!.lowercase().trim()
                        list.forEach {
                            if (it.name.startsWith("*")) {
                                if (lowerName.contains(it.name.substring(1))) {
                                    return FilteringResult(
                                        false,
                                        lowerName,
                                        it.name,
                                        "book author"
                                    )
                                }
                            } else {
                                if (lowerName == it.name) {
                                    return FilteringResult(
                                        false,
                                        lowerName,
                                        it.name,
                                        "book author strict"
                                    )
                                }
                            }
                        }
                    }
                }
                list = BlacklistGenre.instance.getBlacklist()
                if (list.isNotEmpty() && !foundedEntity.genreComplex.isNullOrEmpty()) {
                    foundedEntity.genres.forEach { genre ->
                        lowerName = genre.name!!.trim().lowercase()
                        list.forEach {
                            if (it.name.startsWith("*")) {
                                if (lowerName.contains(it.name.substring(1))) {
                                    return FilteringResult(
                                        false,
                                        lowerName,
                                        it.name,
                                        "book genre"
                                    )
                                }
                            } else {
                                if (lowerName == it.name) {
                                    return FilteringResult(
                                        false,
                                        lowerName,
                                        it.name,
                                        "book genre strict"
                                    )
                                }
                            }
                        }
                    }
                }
                list = BlacklistSequences.instance.getBlacklist()
                if (list.isNotEmpty() && foundedEntity.sequences.isNotEmpty()) {
                    foundedEntity.sequences.forEach { sequence ->
                        val sequenceLowerName = sequence.name!!.trim().lowercase()
                            .substring(17, sequence.name!!.trim().length - 1)
                        list.forEach {
                            if (it.name.startsWith("*")) {
                                if (sequenceLowerName.contains(it.name.substring(1))) {
                                    return FilteringResult(
                                        false,
                                        sequenceLowerName,
                                        it.name,
                                        "book sequence"
                                    )
                                }
                            } else {
                                if (sequenceLowerName == it.name) {
                                    return FilteringResult(
                                        false,
                                        sequenceLowerName,
                                        it.name,
                                        "book sequence strict"
                                    )
                                }
                            }
                        }
                    }
                }
                list = BlacklistFormat.instance.getBlacklist()
                if (list.isNotEmpty() && foundedEntity.format != null) {
                    list.forEach {
                        if (foundedEntity.format!!.endsWith(it.name)) {
                            return FilteringResult(
                                false,
                                foundedEntity.format,
                                it.name.lowercase(),
                                "format"
                            )
                        }
                    }
                }
            } else if (foundedEntity.type == TYPE_GENRE) {
                list = BlacklistGenre.instance.getBlacklist()
                if (list.isNotEmpty()) {
                    lowerName = foundedEntity.name!!.lowercase().trim()
                    list.forEach {
                        if (it.name.startsWith("*")) {
                            if (lowerName.contains(it.name.substring(1))) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name,
                                    "book genre"
                                )
                            }
                        } else {
                            if (lowerName == it.name) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name,
                                    "book genre strict"
                                )
                            }
                        }
                    }
                }
            } else if (foundedEntity.type == TYPE_SEQUENCE) {
                list = BlacklistSequences.instance.getBlacklist()
                if (list.isNotEmpty()) {
                    lowerName = foundedEntity.name!!.lowercase().trim()
                    list.forEach {
                        if (it.name.startsWith("*")) {
                            if (lowerName.contains(it.name.substring(1))) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name,
                                    "book sequence"
                                )
                            }
                        } else {
                            if (lowerName == it.name) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name,
                                    "book sequence strict"
                                )
                            }
                        }
                    }
                }
            } else if (foundedEntity.type == TYPE_AUTHOR) {
                list = BlacklistAuthors.instance.getBlacklist()
                if (list.isNotEmpty()) {
                    lowerName = foundedEntity.name!!.lowercase().trim()
                    list.forEach {
                        if (it.name.startsWith("*")) {
                            if (lowerName.contains(it.name.substring(1))) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name,
                                    "book author"
                                )
                            }
                        } else {
                            if (lowerName == it.name) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name,
                                    "book author strict"
                                )
                            }
                        }
                    }
                }
            } else if (foundedEntity.type == TYPE_AUTHORS) {
                list = BlacklistAuthors.instance.getBlacklist()
                if (list.isNotEmpty()) {
                    lowerName = foundedEntity.name!!.lowercase().trim()
                    list.forEach {
                        if (it.name.startsWith("*")) {
                            if (lowerName.contains(it.name.substring(1))) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name,
                                    "book author"
                                )
                            }
                        } else {
                            if (lowerName == it.name) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name,
                                    "book author strict"
                                )
                            }
                        }
                    }
                }
            }
            if (PreferencesHandler.instance.isHideRead) {
                if (foundedEntity.read) {
                    return FilteringResult(false, null, null, "hideRead")
                }
            }
            if (PreferencesHandler.instance.isHideDownloaded) {
                if (foundedEntity.downloaded) {
                    return FilteringResult(false, null, null, "hideDownloaded")
                }
            }
            if (PreferencesHandler.instance.isHideDigests) {
                if (foundedEntity.authors.size > 2) {
                    return FilteringResult(false, foundedEntity.author, null, "hideDigests")
                }
            }
        }
        return FilteringResult(true, null, null, null)
    }

    fun addToBlacklist(item: FoundEntity, target: String): List<BlacklistItem> {
        val result = arrayListOf<BlacklistItem>()
        if (target == "name") {
            when (item.type) {
                TYPE_AUTHOR, TYPE_AUTHORS -> {
                        if (item.name != null) {
                            BlacklistAuthors.instance.addValue(item.name!!)
                            result.add(BlacklistItem(item.name!!, "author"))
                        }
                }
                TYPE_GENRE -> {
                        if (item.name != null) {
                            BlacklistGenre.instance.addValue(item.name!!)
                            result.add(BlacklistItem(item.name!!, "genre"))
                    }
                }
                TYPE_SEQUENCE -> {
                            val value = item.name!!.trim().lowercase()
                            BlacklistSequences.instance.addValue(value)
                            result.add(BlacklistItem(item.name!!, "sequence"))
                }
            }
            return result
        }
        when (target) {
            "author" -> {
                item.authors.forEach {
                    if (it.name != null) {
                        BlacklistAuthors.instance.addValue(it.name!!)
                        result.add(BlacklistItem(it.name!!, "author"))
                    }
                }
            }
            "sequence" -> {
                item.sequences.forEach {
                    if (it.name != null) {
                        val value = it.name!!.trim().lowercase()
                            .substring(17, it.name!!.trim().length - 1)
                        BlacklistSequences.instance.addValue(value)
                        result.add(BlacklistItem(it.name!!, "sequence"))
                    }
                }
            }
            "genre" -> {
                item.genres.forEach {
                    if (it.name != null) {
                        BlacklistGenre.instance.addValue(it.name!!)
                        result.add(BlacklistItem(it.name!!, "genre"))
                    }
                }
            }
        }
        return result
    }

    fun filterByRule(item: FoundEntity, rule: BlacklistItem): Boolean {
        when (rule.type) {
            "author" -> {
                item.authors.forEach {
                    if (it.name == rule.name) {
                        return true
                    }
                }
            }
            "genre" -> {
                item.genres.forEach {
                    if (it.name == rule.name) {
                        return true
                    }
                }
            }
            "sequence" -> {
                item.sequences.forEach {
                    if (it.name == rule.name) {
                        return true
                    }
                }
            }
        }
        return false
    }
}