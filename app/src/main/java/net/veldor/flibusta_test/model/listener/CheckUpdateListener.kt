package net.veldor.flibusta_test.model.listener

import net.veldor.flibusta_test.model.selection.UpdateInfo

interface CheckUpdateListener {
    fun haveUpdate(updateInfo: UpdateInfo?)
    fun checkError(message: String?)
}