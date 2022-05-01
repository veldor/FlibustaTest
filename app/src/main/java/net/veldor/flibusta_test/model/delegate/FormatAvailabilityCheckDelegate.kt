package net.veldor.flibusta_test.model.delegate

import android.view.View
import net.veldor.flibusta_test.model.selections.opds.FoundEntity

interface FormatAvailabilityCheckDelegate {
    fun formatAvailable(size: String?)
    fun formatUnavailable()
}