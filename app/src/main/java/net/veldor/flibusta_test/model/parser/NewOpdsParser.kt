package net.veldor.flibusta_test.model.parser

import android.util.Log
import kotlinx.coroutines.Job
import net.veldor.flibusta_test.model.db.DatabaseInstance
import net.veldor.flibusta_test.model.handler.FilterHandler
import net.veldor.flibusta_test.model.handler.GrammarHandler
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.selections.DownloadLink
import net.veldor.flibusta_test.model.selections.OpdsStatement
import net.veldor.flibusta_test.model.selections.opds.FoundEntity
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

class NewOpdsParser(private val text: String) {
    fun parse(currentWork: Job?) {
        var contentType: String? = null
        var downloadLink: DownloadLink
        var entity: FoundEntity
        var author: FoundEntity? = null
        val authorStringBuilder = StringBuilder()
        val genreStringBuilder = StringBuilder()
        val simpleStringBuilder = StringBuilder()
        var nextPageLinkFound = false
        var idFound = false
        var nameFound = false
        var issued = false
        var updated = false
        var authorContainerFound = false
        var authorFound = false
        var authorUriFound = false
        var contentFound = false
        var textValue: String?
        var attributeIndex: Int
        var attributeValue: String


        val db = DatabaseInstance.instance.mDatabase
        try {
            var foundedEntity: FoundEntity? = null

            val factory: SAXParserFactory = SAXParserFactory.newInstance()
            val saxParser: SAXParser = factory.newSAXParser()
            val handler: DefaultHandler = object : DefaultHandler() {
                // Метод вызывается когда SAXParser "натыкается" на начало тэга
                @Throws(SAXException::class)
                override fun startElement(
                    uri: String?,
                    localName: String?,
                    qName: String,
                    attributes: Attributes?
                ) {
                    if (currentWork?.isCancelled == true) {
                        return
                    }
                    if (qName.equals("entry", ignoreCase = true)) {
                        foundedEntity = FoundEntity()
                    } else if (qName == "id" && foundedEntity != null) {
                        idFound = true
                    } else if (qName == "title" && foundedEntity != null) {
                        nameFound = true
                        foundedEntity!!.name = ""
                    } else if (qName == "author" && foundedEntity != null) {
                        authorContainerFound = true
                    } else if (qName == "category" && foundedEntity != null) {
                        // add a category
                        attributeIndex = attributes!!.getIndex("label")
                        entity = FoundEntity()
                        entity.type = TYPE_GENRE
                        entity.name = attributes.getValue(attributeIndex)
                        foundedEntity!!.genres.add(entity)
                        genreStringBuilder.append(entity.name)
                        genreStringBuilder.append("\n")
                    } else if (qName == "name" && foundedEntity != null && authorContainerFound) {
                        authorFound = true
                        author = FoundEntity()
                        author!!.name = ""
                        author!!.type = TYPE_AUTHOR
                    } else if (qName == "uri" && foundedEntity != null && authorContainerFound) {
                        authorUriFound = true
                        authorContainerFound = false
                    } else if (qName == "content" && foundedEntity != null) {
                        contentFound = true
                    } else if (qName == "link") {
                        if (foundedEntity == null) {
                            // check it is a link on next results page
                            attributeIndex = attributes!!.getIndex("rel")
                            if (attributeIndex >= 0) {
                                attributeValue = attributes.getValue(attributeIndex)
                                if (attributeValue == "next") {
                                    nextPageLinkFound = true
                                    // found link on next page
                                    attributeIndex = attributes.getIndex("href")
                                    if (attributeIndex >= 0) {
                                        OpdsStatement.instance.setNextPageLink(
                                            attributes.getValue(
                                                attributeIndex
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            // check if it is a download link
                            attributeIndex = attributes!!.getIndex("rel")
                            if (attributeIndex >= 0) {
                                attributeValue = attributes.getValue(attributeIndex)
                                if (attributeValue == "http://opds-spec.org/acquisition/open-access") {
                                    // link found, append it
                                    attributeIndex = attributes.getIndex("href")
                                    downloadLink = DownloadLink()
                                    downloadLink.url = attributes.getValue(attributeIndex)
                                    if (foundedEntity != null && foundedEntity?.id == null) {
                                        foundedEntity?.id = downloadLink.url!!.replace("fb2", "")
                                            .filter { it.isDigit() }
                                    }
                                    attributeIndex = attributes.getIndex("type")
                                    downloadLink.mime = attributes.getValue(attributeIndex)
                                    foundedEntity!!.downloadLinks.add(downloadLink)
                                } else if (attributeValue == "http://opds-spec.org/acquisition/disabled") {
                                    // disabled link found
                                    Log.d("surprise", "OpdsParser.kt 119: disabled link found")
                                } else if (attributeValue == "http://opds-spec.org/image") {
                                    // найдена обложка
                                    foundedEntity!!.coverUrl =
                                        attributes.getValue(attributes.getIndex("href"))
                                } else if (attributeValue == "alternate") {
                                    // найдена обложка
                                    foundedEntity!!.link =
                                        attributes.getValue(attributes.getIndex("href"))
                                } else {
                                    attributeIndex = attributes.getIndex("href")
                                    attributeValue = attributes.getValue(attributeIndex)
                                    if (attributeValue.startsWith("/opds/sequencebooks/")) {
                                        // found sequence
                                        entity = FoundEntity()
                                        entity.type = TYPE_SEQUENCE
                                        entity.link = attributeValue
                                        attributeIndex = attributes.getIndex("title")
                                        entity.name = attributes.getValue(attributeIndex)
                                        foundedEntity!!.sequences.add(entity)

                                    }
                                }
                            } else {
                                // if it is a sequence selector- save link
                                if (foundedEntity!!.type == TYPE_SEQUENCE || foundedEntity!!.type == TYPE_GENRE || foundedEntity!!.type == TYPE_AUTHORS || foundedEntity!!.type == TYPE_AUTHOR) {
                                    attributeIndex = attributes.getIndex("href")
                                    foundedEntity!!.link = attributes.getValue(attributeIndex)
                                }
                            }
                        }
                    } else if (qName == "dc:issued" && foundedEntity != null) {
                        issued = true
                        foundedEntity!!.publicationYear = ""
                    } else if (qName == "updated" && foundedEntity != null) {
                        updated = true
                        foundedEntity!!.publicationYear = ""
                    }
                }

                override fun endElement(uri: String?, localName: String?, qName: String?) {
                    if (currentWork?.isCancelled == true) {
                        return
                    }
                    if (qName.equals("entry", ignoreCase = true)) {
                        foundedEntity!!.author = authorStringBuilder.removeSuffix("\n").toString()
                        foundedEntity!!.genreComplex =
                            genreStringBuilder.removeSuffix("\n").toString()
                        genreStringBuilder.clear()
                        authorStringBuilder.clear()

                        foundedEntity!!.read =
                            db.readBooksDao()
                                .getBookById(foundedEntity!!.id) != null || db.readBooksDao()
                                .getBookById(foundedEntity!!.systemId) != null
                        foundedEntity!!.downloaded =
                            db.downloadedBooksDao()
                                .getBookById(foundedEntity!!.id) != null || db.downloadedBooksDao()
                                .getBookById(foundedEntity!!.systemId) != null

                        var authorDirName: String = when (foundedEntity!!.authors.size) {
                            0 -> {
                                "Без автора"
                            }
                            1 -> {
                                // создам название папки
                                GrammarHandler.createAuthorDirName(foundedEntity!!.authors[0])
                            }
                            2 -> {
                                GrammarHandler.createAuthorDirName(foundedEntity!!.authors[0]) + " " + GrammarHandler.createAuthorDirName(
                                    foundedEntity!!.authors[1]
                                )
                            }
                            else -> {
                                "Антологии"
                            }
                        }
                        authorDirName = GrammarHandler.clearDirName(authorDirName).trim()

                        foundedEntity?.downloadLinks?.forEach { link ->
                            link.author = foundedEntity!!.author
                            link.id = foundedEntity!!.id
                            link.name = foundedEntity!!.name
                            link.size = foundedEntity!!.size ?: "0"
                            link.authorDirName = authorDirName
                            // так, как книга может входить в несколько серий- совмещу назначения
                            if (foundedEntity!!.sequences.size > 0) {
                                simpleStringBuilder.clear()
                                var prefix = ""
                                foundedEntity!!.sequences.forEach {
                                    simpleStringBuilder.append(prefix)
                                    prefix = "$|$"
                                    simpleStringBuilder.append(
                                        Regex("[^\\d\\w ]").replace(
                                            it.name!!.replace(
                                                "Все книги серии",
                                                ""
                                            ), ""
                                        )
                                    )
                                }
                                link.sequenceDirName = simpleStringBuilder.toString().trim()
                                link.nameInSequence =
                                    foundedEntity!!.sequencesComplex.trim().replace("Серия: ", "")
                            } else {
                                link.sequenceDirName = ""
                            }
                        }
                        if (PreferencesHandler.instance.isOpdsUseFilter) {
                            val filterResult = FilterHandler.check(foundedEntity!!)
                            if (filterResult.result) {
                                OpdsStatement.instance.addParsedResult(foundedEntity)
                            } else {
                                OpdsStatement.instance.addFilteredResult(foundedEntity!!)
                            }
                        } else {
                            OpdsStatement.instance.addParsedResult(foundedEntity)
                        }
                    } else if (qName.equals("content")) {
                        contentFound = false
                    } else if (qName.equals("name")) {
                        authorStringBuilder.append(author!!.name)
                        authorStringBuilder.append("\n")
                        authorFound = false
                    } else if (qName.equals("title")) {
                        nameFound = false
                    } else if (qName.equals("dc:issued")) {
                        issued = false
                    } else if (qName.equals("dc:updated")) {
                        updated = false
                    }
                }

                // Метод вызывается когда SAXParser считывает текст между тэгами
                @Throws(SAXException::class)
                override fun characters(ch: CharArray?, start: Int, length: Int) {
                    // Если перед этим мы отметили, что имя тэга NAME - значит нам надо текст использовать.
                    if (idFound) {
                        idFound = false
                        textValue = String(ch!!, start, length)
                        foundedEntity?.systemId = textValue
                        when {
                            textValue!!.contains(TYPE_BOOK) -> {
                                contentType = TYPE_BOOK
                            }
                            textValue!!.contains(TYPE_SEQUENCE) -> {
                                contentType = TYPE_SEQUENCE
                            }
                            textValue!!.contains(TYPE_AUTHORS) -> {
                                contentType = TYPE_AUTHORS
                            }
                            textValue!!.contains(TYPE_AUTHOR) -> {
                                contentType = TYPE_AUTHOR
                            }
                            textValue!!.contains(TYPE_GENRE) -> {
                                contentType = TYPE_GENRE
                            }
                        }
                        foundedEntity!!.type = contentType!!
                    }
                    if (nameFound) {
                        if (ch != null) {
                            foundedEntity!!.name += String(ch, start, length)
                        }
                    }
                    if (contentFound) {
                        parseContent(foundedEntity, String(ch!!, start, length))
                    }
                    if (authorFound) {
                        if (ch != null) {
                            author!!.name += String(ch, start, length)
                        }
                    }
                    if (issued) {
                        if (ch != null) {
                            foundedEntity!!.publicationYear += String(ch, start, length)
                        }
                    }
                    if (updated) {
                        if (ch != null) {
                            foundedEntity!!.publishTime += String(ch, start, length)
                        }
                    }
                    if (authorUriFound) {
                        author!!.link = String(ch!!, start, length)
                        author!!.id = author!!.link
                        foundedEntity!!.authors.add(author!!)
                        authorUriFound = false
                    }
                }
                // Стартуем разбор методом parse, которому передаем наследника от DefaultHandler, который будет вызываться в нужные моменты

            }
            saxParser.parse(text.byteInputStream(), handler)
            if(!nextPageLinkFound){
                OpdsStatement.instance.setNextPageLink(null)
            }
        } catch (e: Exception) {
            Log.d("surprise", "parse: parse error")
            e.printStackTrace()
        }
    }

    private fun parseContent(foundedEntity: FoundEntity?, content: String) {
        foundedEntity!!.content = foundedEntity.content + content
        when {
            content.startsWith("Скачиваний") -> {
                foundedEntity.downloadsCount = content
            }
            content.startsWith("Размер") -> {
                foundedEntity.size = content
            }
            content.startsWith("Формат") -> {
                foundedEntity.format = content
            }
            content.startsWith("Перевод") -> {
                foundedEntity.translate = content
            }
            content.startsWith("Серия") -> {
                foundedEntity.sequencesComplex = content
            }
            content.startsWith("Язык") -> {
                foundedEntity.language = content
            }
            content.contains("сери") -> {
                foundedEntity.description = content.substring(0, content.indexOf("сери") + 5)
            }
            content.contains("автора на") -> {
                foundedEntity.description = content
            }
        }
    }

    companion object {
        const val TYPE_BOOK = "book"
        const val TYPE_AUTHOR = "author"
        const val TYPE_AUTHORS = "authors"
        const val TYPE_GENRE = "genre"
        const val TYPE_SEQUENCE = "sequence"
    }
}