package net.veldor.flibusta_test.model.delegate

import net.veldor.flibusta_test.model.selection.FoundEntity


interface BookInfoAddedDelegate {
    fun infoAdded(book: FoundEntity)
    fun checkProgress(linksChecked: Int,currentProgress: Int, size: Int?)
}