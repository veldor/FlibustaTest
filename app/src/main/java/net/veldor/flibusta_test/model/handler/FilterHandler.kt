package net.veldor.flibusta_test.model.handler

import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_AUTHOR
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_AUTHORS
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_BOOK
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_GENRE
import net.veldor.flibusta_test.model.parser.OpdsParser.Companion.TYPE_SEQUENCE
import net.veldor.flibusta_test.model.selection.FoundEntity
import net.veldor.flibusta_test.model.selection.filter.*

object FilterHandler {
    fun check(foundedEntity: FoundEntity): FilteringResult {
        if (PreferencesHandler.isOpdsUseFilter) {
            var list: ArrayList<BlacklistItem>
            var lowerName: String
            if (foundedEntity.type == TYPE_BOOK) {
                if (PreferencesHandler.isHideRead) {
                    if (foundedEntity.read) {
                        return FilteringResult(false, null, null, "hideRead")
                    }
                }
                if (PreferencesHandler.isHideDownloaded) {
                    if (foundedEntity.downloaded) {
                        return FilteringResult(false, null, null, "hideDownloaded")
                    }
                }
                if (PreferencesHandler.isHideDigests) {
                    if (foundedEntity.authors.size > 2) {
                        return FilteringResult(false, foundedEntity.author, null, "hideDigests")
                    }
                }
                // check for all of blacklists
                if (PreferencesHandler.isOnlyRussian && !foundedEntity.language.contains("ru")) {
                    return FilteringResult(false, null, null, "hideNonRussian")
                }
                list = BlacklistBooks.getBlacklist()
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
                list = BlacklistAuthors.getBlacklist()
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
                list = BlacklistGenre.getBlacklist()
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
                list = BlacklistSequences.getBlacklist()
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
                list = BlacklistFormat.getBlacklist()
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
                list = BlacklistGenre.getBlacklist()
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
                list = BlacklistSequences.getBlacklist()
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
                list = BlacklistAuthors.getBlacklist()
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
                list = BlacklistAuthors.getBlacklist()
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
        }
        return FilteringResult(true, null, null, null)
    }

}