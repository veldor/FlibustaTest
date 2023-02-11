// Copyright 2005 Nick Mathewson, Roger Dingledine
// See LICENSE file for copying information
package net.veldor.tor_client.model.tor_utils

/**
 * Abstract interface whose methods are invoked when Tor sends us an event.
 *
 * @see TorControlConnection.setEventHandler
 *
 * @see TorControlConnection.setEvents
 */
interface EventHandler {
    /**
     * Invoked when a circuit's status has changed.
     * Possible values for **status** are:
     *
     *  * "LAUNCHED" :  circuit ID assigned to new circuit
     *  * "BUILT"    :  all hops finished, can now accept streams
     *  * "EXTENDED" :  one more hop has been completed
     *  * "FAILED"   :  circuit closed (was not built)
     *  * "CLOSED"   :  circuit closed (was built)
     *
     *
     * **circID** is the alphanumeric identifier of the affected circuit,
     * and **path** is a comma-separated list of alphanumeric ServerIDs.
     */
    fun circuitStatus(status: String?, circID: String?, path: String?)

    /**
     * Invoked when a stream's status has changed.
     * Possible values for **status** are:
     *
     *  * "NEW"         :  New request to connect
     *  * "NEWRESOLVE"  :  New request to resolve an address
     *  * "SENTCONNECT" :  Sent a connect cell along a circuit
     *  * "SENTRESOLVE" :  Sent a resolve cell along a circuit
     *  * "SUCCEEDED"   :  Received a reply; stream established
     *  * "FAILED"      :  Stream failed and not retriable.
     *  * "CLOSED"      :  Stream closed
     *  * "DETACHED"    :  Detached from circuit; still retriable.
     *
     *
     * **streamID** is the alphanumeric identifier of the affected stream,
     * and its **target** is specified as address:port.
     */
    fun streamStatus(status: String?, streamID: String?, target: String?)

    /**
     * Invoked when the status of a connection to an OR has changed.
     * Possible values for **status** are ["LAUNCHED" | "CONNECTED" | "FAILED" | "CLOSED"].
     * **orName** is the alphanumeric identifier of the OR affected.
     */
    fun orConnStatus(status: String?, orName: String?)

    /**
     * Invoked once per second. **read** and **written** are
     * the number of bytes read and written, respectively, in
     * the last second.
     */
    fun bandwidthUsed(read: Long, written: Long)

    /**
     * Invoked whenever Tor learns about new ORs.  The **orList** object
     * contains the alphanumeric ServerIDs associated with the new ORs.
     */
    fun newDescriptors(orList: List<String?>?)

    /**
     * Invoked when Tor logs a message.
     * **severity** is one of ["DEBUG" | "INFO" | "NOTICE" | "WARN" | "ERR"],
     * and **msg** is the message string.
     */
    fun message(severity: String?, msg: String)

    /**
     * Invoked when an unspecified message is received.
     * <type> is the message type, and <msg> is the message string.
    </msg></type> */
    fun unrecognized(type: String?, msg: String)
}