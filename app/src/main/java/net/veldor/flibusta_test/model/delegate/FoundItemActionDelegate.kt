package net.veldor.flibusta_test.model.delegate

import android.view.View
import net.veldor.flibusta_test.model.selections.opds.FoundEntity

interface FoundItemActionDelegate {
    fun buttonPressed(item: FoundEntity)
    fun imageClicked(item: FoundEntity)
    fun itemPressed(item: FoundEntity)
    fun buttonLongPressed(item: FoundEntity, target: String)
    fun itemLongPressed(item: FoundEntity)
    fun menuItemPressed(item: FoundEntity, button: View)
    fun loadMoreBtnClicked()
    fun authorClicked(item: FoundEntity)
    fun sequenceClicked(item: FoundEntity)
    fun nameClicked(item: FoundEntity)
    fun rightButtonPressed(item: FoundEntity)
    fun leftButtonPressed(item: FoundEntity)
}