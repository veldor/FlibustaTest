package net.veldor.flibusta_test.model.listener

interface ActionListener {
    fun actionStateUpdated(message: String)
    fun actionFinished(isSuccess: Boolean, message: String)
    fun actionLaunched()
}