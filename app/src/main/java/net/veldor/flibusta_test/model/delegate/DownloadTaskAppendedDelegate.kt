package net.veldor.flibusta_test.model.delegate

import net.veldor.flibusta_test.model.selections.DownloadLink
import net.veldor.flibusta_test.model.selections.opds.FoundEntity
import java.util.ArrayList

interface DownloadTaskAppendedDelegate {
    fun taskAppended(link: DownloadLink)
    fun booksParsed(result: ArrayList<FoundEntity>)
    fun taskAppendFailed()
}