package net.veldor.flibusta_test.model.delegate

import net.veldor.flibusta_test.model.selections.opds.FoundEntity

interface OpdsObserverDelegate {
    fun itemInserted(item: FoundEntity)
    fun itemFiltered(item: FoundEntity)
    fun drawBadges()
}