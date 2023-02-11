// Copyright 2005 Nick Mathewson, Roger Dingledine
// See LICENSE file for copying information
package net.veldor.tor_client.model.exceptions

import net.veldor.tor_client.model.tor_utils.TorControlCommands
import java.io.IOException

/**
 * An exception raised when Tor tells us about an error.
 */
class TorControlError(val errorType: Int, s: String?) : IOException(s) {

    constructor(s: String?) : this(-1, s) {}

    val errorMsg: String?
        get() = try {
            if (errorType == -1) null else TorControlCommands.ERROR_MSGS[errorType]
        } catch (ex: ArrayIndexOutOfBoundsException) {
            "Unrecongized error #$errorType"
        }

    companion object {
        const val serialVersionUID: Long = 3
    }
}