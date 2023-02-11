// Copyright 2005 Nick Mathewson, Roger Dingledine
// See LICENSE file for copying information
package net.veldor.tor_client.model.tor_utils

/** Interface defining constants used by the Tor controller protocol.
 */
// XXXX Take documentation for these from control-spec.txt
interface TorControlCommands {
    companion object {
        const val CMD_ERROR: Short = 0x0000
        const val CMD_DONE: Short = 0x0001
        const val CMD_SETCONF: Short = 0x0002
        const val CMD_GETCONF: Short = 0x0003
        const val CMD_CONFVALUE: Short = 0x0004
        const val CMD_SETEVENTS: Short = 0x0005
        const val CMD_EVENT: Short = 0x0006
        const val CMD_AUTH: Short = 0x0007
        const val CMD_SAVECONF: Short = 0x0008
        const val CMD_SIGNAL: Short = 0x0009
        const val CMD_MAPADDRESS: Short = 0x000A
        const val CMD_GETINFO: Short = 0x000B
        const val CMD_INFOVALUE: Short = 0x000C
        const val CMD_EXTENDCIRCUIT: Short = 0x000D
        const val CMD_ATTACHSTREAM: Short = 0x000E
        const val CMD_POSTDESCRIPTOR: Short = 0x000F
        const val CMD_FRAGMENTHEADER: Short = 0x0010
        const val CMD_FRAGMENT: Short = 0x0011
        const val CMD_REDIRECTSTREAM: Short = 0x0012
        const val CMD_CLOSESTREAM: Short = 0x0013
        const val CMD_CLOSECIRCUIT: Short = 0x0014
        val CMD_NAMES = arrayOf(
            "ERROR",
            "DONE",
            "SETCONF",
            "GETCONF",
            "CONFVALUE",
            "SETEVENTS",
            "EVENT",
            "AUTH",
            "SAVECONF",
            "SIGNAL",
            "MAPADDRESS",
            "GETINFO",
            "INFOVALUE",
            "EXTENDCIRCUIT",
            "ATTACHSTREAM",
            "POSTDESCRIPTOR",
            "FRAGMENTHEADER",
            "FRAGMENT",
            "REDIRECTSTREAM",
            "CLOSESTREAM",
            "CLOSECIRCUIT"
        )
        const val EVENT_CIRCSTATUS: Short = 0x0001
        const val EVENT_STREAMSTATUS: Short = 0x0002
        const val EVENT_ORCONNSTATUS: Short = 0x0003
        const val EVENT_BANDWIDTH: Short = 0x0004
        const val EVENT_NEWDESCRIPTOR: Short = 0x0006
        const val EVENT_MSG_DEBUG: Short = 0x0007
        const val EVENT_MSG_INFO: Short = 0x0008
        const val EVENT_MSG_NOTICE: Short = 0x0009
        const val EVENT_MSG_WARN: Short = 0x000A
        const val EVENT_MSG_ERROR: Short = 0x000B
        val EVENT_NAMES = arrayOf(
            "(0)",
            "CIRC",
            "STREAM",
            "ORCONN",
            "BW",
            "OLDLOG",
            "NEWDESC",
            "DEBUG",
            "INFO",
            "NOTICE",
            "WARN",
            "ERR"
        )
        const val CIRC_STATUS_LAUNCHED: Byte = 0x01
        const val CIRC_STATUS_BUILT: Byte = 0x02
        const val CIRC_STATUS_EXTENDED: Byte = 0x03
        const val CIRC_STATUS_FAILED: Byte = 0x04
        const val CIRC_STATUS_CLOSED: Byte = 0x05
        val CIRC_STATUS_NAMES = arrayOf(
            "LAUNCHED",
            "BUILT",
            "EXTENDED",
            "FAILED",
            "CLOSED"
        )
        const val STREAM_STATUS_SENT_CONNECT: Byte = 0x00
        const val STREAM_STATUS_SENT_RESOLVE: Byte = 0x01
        const val STREAM_STATUS_SUCCEEDED: Byte = 0x02
        const val STREAM_STATUS_FAILED: Byte = 0x03
        const val STREAM_STATUS_CLOSED: Byte = 0x04
        const val STREAM_STATUS_NEW_CONNECT: Byte = 0x05
        const val STREAM_STATUS_NEW_RESOLVE: Byte = 0x06
        const val STREAM_STATUS_DETACHED: Byte = 0x07
        val STREAM_STATUS_NAMES = arrayOf(
            "SENT_CONNECT",
            "SENT_RESOLVE",
            "SUCCEEDED",
            "FAILED",
            "CLOSED",
            "NEW_CONNECT",
            "NEW_RESOLVE",
            "DETACHED"
        )
        const val OR_CONN_STATUS_LAUNCHED: Byte = 0x00
        const val OR_CONN_STATUS_CONNECTED: Byte = 0x01
        const val OR_CONN_STATUS_FAILED: Byte = 0x02
        const val OR_CONN_STATUS_CLOSED: Byte = 0x03
        val OR_CONN_STATUS_NAMES = arrayOf(
            "LAUNCHED", "CONNECTED", "FAILED", "CLOSED"
        )
        const val SIGNAL_HUP: Byte = 0x01
        const val SIGNAL_INT: Byte = 0x02
        const val SIGNAL_USR1: Byte = 0x0A
        const val SIGNAL_USR2: Byte = 0x0C
        const val SIGNAL_TERM: Byte = 0x0F
        val ERROR_MSGS = arrayOf(
            "Unspecified error",
            "Internal error",
            "Unrecognized message type",
            "Syntax error",
            "Unrecognized configuration key",
            "Invalid configuration value",
            "Unrecognized byte code",
            "Unauthorized",
            "Failed authentication attempt",
            "Resource exhausted",
            "No such stream",
            "No such circuit",
            "No such OR"
        )
    }
}