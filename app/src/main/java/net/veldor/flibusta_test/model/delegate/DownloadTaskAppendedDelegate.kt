package net.veldor.flibusta_test.model.delegate

import net.veldor.flibusta_test.model.selection.DownloadLink
import net.veldor.flibusta_test.model.selection.FoundEntity
import java.util.ArrayList

interface DownloadTaskAppendedDelegate {
    fun taskAppended(link: DownloadLink)
    fun booksParsed(result: HashMap<String, FoundEntity>)
    fun taskAppendFailed()
}