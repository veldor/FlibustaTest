package net.veldor.flibusta_test.model.selection

data class SocketMultiFile(
    val command: String,
    val payload: String,
    val transferId: String,
    val value: String,
    val currentFileIndex: Int,
    val size: Int,
)