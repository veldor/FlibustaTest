package net.veldor.flibusta_test.model.delegate

import net.veldor.flibusta_test.model.selection.FoundEntity
import net.veldor.flibusta_test.model.selection.RequestItem
import net.veldor.tor_client.model.connection.WebResponse


interface OpdsObserverDelegate {
    fun itemInserted(item: FoundEntity)
    fun itemFiltered(item: FoundEntity)
    fun drawBadges()
    fun hasConnectionError(request: RequestItem, response: WebResponse)
    fun hasNoResults()
}