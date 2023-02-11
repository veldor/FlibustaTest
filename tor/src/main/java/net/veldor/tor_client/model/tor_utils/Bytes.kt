// Copyright 2005 Nick Mathewson, Roger Dingledine
// See LICENSE file for copying information
package net.veldor.tor_client.model.tor_utils

/**
 * Static class to do bytewise structure manipulation in Java.
 */
/* XXXX There must be a better way to do most of this.
 * XXXX The string logic here uses default encoding, which is stupid.
 */
internal object Bytes {
    /**
     * Read bytes from 'ba' starting at 'pos', dividing them into strings
     * along the character in 'split' and writing them into 'lst'
     */
    fun splitStr(lst: MutableList<String>?, str: String): List<String> {
        // split string on spaces, include trailing/leading
        var l = lst
        val tokenArray = str.split(" ".toRegex()).toTypedArray()
        if(l != null){
            l.addAll(listOf(*tokenArray))
        }
        else{
            l = mutableListOf(*tokenArray)
        }

        return l
    }

    private val NYBBLES = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    )

    fun hex(ba: ByteArray): String {
        val buf = StringBuilder()
        for (value in ba) {
            val b = value.toInt() and 0xff
            buf.append(NYBBLES[b shr 4])
            buf.append(NYBBLES[b and 0x0f])
        }
        return buf.toString()
    }
}