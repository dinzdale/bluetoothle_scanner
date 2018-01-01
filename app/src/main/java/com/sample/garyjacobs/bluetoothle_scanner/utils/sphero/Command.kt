package com.sample.garyjacobs.bluetoothle_scanner.utils.sphero

import kotlin.experimental.or

/**
 * Created by garyjacobs on 12/31/17.
 */
class Command() {
    // constants
    private val NILL = 0x00.toByte()
    private val SOP1 = 0xFF.toByte()
    private val SOP2_BASE = 0xFC.toByte()
    private val SOP2_ANSWER_REPLY = 0x01.toByte()
    private val SOP2_ANSWER_TIME_OUT = 0x02.toByte()
    private val DID_CORE = 0x00.toByte()
    private val DID_SPHERO = 0x02.toByte()
    // commands CORE
    private val CMD_CORE_PING = 0x01.toByte()
    private val CMD_CORE_VERSION = 0x02.toByte()
    // commands SPHERO
    private val CMD_SPHERO_SET_RGB_LED = 0x20.toByte()
    private val CMD_SPHERO_SET_BACK_LED = 0x21.toByte()
    private val CMD_SPHERO_GET_RGB_LED = 0x22.toByte()

    private val buffer: Array<Byte>? = null

    private fun getSOP2NoTOReply() = SOP2_BASE.or(SOP2_ANSWER_REPLY).toByte()

    private fun getChk(bytes: Array<Byte>) = (bytes.sum() % 256).inv().toByte()

    fun pingCMD(seq: Byte = NILL): Array<Byte> {
        val buffer = arrayOf(SOP1, getSOP2NoTOReply(), DID_CORE, CMD_CORE_PING, seq, 0x01, NILL, NILL)
        buffer[buffer.size - 1] = getChk(buffer.sliceArray(2..6))
        return buffer
    }

    fun setRgbLedCmd(red: Byte = NILL, green: Byte = NILL, blue: Byte = NILL, seq: Byte = NILL, userColor: Boolean = false): Array<Byte> {
        val buffer = arrayOf(SOP1, getSOP2NoTOReply(), DID_SPHERO, CMD_SPHERO_SET_RGB_LED, seq, 0x05, red, green, blue, NILL, NILL)
        if (userColor) {
            buffer[buffer.size-2] = 0x01
        }
        buffer[buffer.size - 1] = getChk(buffer.sliceArray(2..buffer.size - 2))
        return buffer
    }

    fun getRgbLedCmd(seq: Byte = NILL): Array<Byte> {
        val buffer = arrayOf(SOP1, getSOP2NoTOReply(), DID_SPHERO, CMD_SPHERO_GET_RGB_LED, seq, 0x01, NILL, NILL)
        buffer[buffer.size - 1] = getChk(buffer.sliceArray(2..buffer.size - 2))
        return buffer
    }

    fun setBackLedCmd(alpha: Byte = NILL, seq: Byte = NILL): Array<Byte> {
        val buffer = arrayOf(SOP1, getSOP2NoTOReply(), DID_SPHERO, CMD_SPHERO_SET_BACK_LED, seq, 0x02, NILL, NILL)
        buffer[buffer.size - 1] = getChk(buffer.sliceArray(2..buffer.size - 2))
        return buffer
    }
}