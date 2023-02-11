// Copyright 2005 Nick Mathewson, Roger Dingledine
// See LICENSE file for copying information
package net.veldor.tor_client.model.tor_utils

/** A single key-value pair from Tor's configuration.  */
class ConfigEntry {
    constructor(k: String, v: String) {
        key = k
        value = v
        isDefault = false
    }

    constructor(k: String) {
        key = k
        value = ""
        isDefault = true
    }

    private val key: String
    val value: String
    private val isDefault: Boolean
}