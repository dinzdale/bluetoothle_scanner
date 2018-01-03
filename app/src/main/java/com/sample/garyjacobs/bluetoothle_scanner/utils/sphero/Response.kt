package com.sample.garyjacobs.bluetoothle_scanner.utils.sphero

/**
 * Created by garyjacobs on 1/1/18.
 */
data class Response(val void: Unit? = null)  {
    // constants
    private val NILL = 0x00.toByte()
    private val SOP1 = 0xFF.toByte()
    private val SOP2_ACK = 0xFF.toByte()
    private val SOP2_ASYNCH = 0xFE.toByte()
    private val DID_CORE = 0x00.toByte()
    private val DID_SPHERO = 0x02.toByte()
    private val MRSP_ACK = 0x00.toByte()
    // commands CORE
    private val CMD_CORE_PING = 0x01.toByte()
    private val CMD_CORE_VERSION = 0x02.toByte()
    // commands SPHERO
    private val CMD_SPHERO_SET_RGB_LED = 0x20.toByte()
    private val CMD_SPHERO_SET_BACK_LED = 0x21.toByte()
    private val CMD_SPHERO_GET_RGB_LED = 0x22.toByte()

    fun ackNack(response: ByteArray): Pair<Boolean, Byte> {

        var result = false
        if (response[2] == MRSP_ACK) {
            result = true
        }
        return Pair(result, response[3])
    }
}