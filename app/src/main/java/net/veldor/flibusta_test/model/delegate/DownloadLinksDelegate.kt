package net.veldor.flibusta_test.model.delegate

import java.io.InputStream

interface DownloadLinksDelegate {
    fun linkClicked(link: String)
    fun textReceived(textStream: InputStream)
}