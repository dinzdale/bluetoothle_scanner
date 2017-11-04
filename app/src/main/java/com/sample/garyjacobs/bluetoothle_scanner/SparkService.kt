package com.sample.garyjacobs.bluetoothle_scanner

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger

/**
 * Created by garyjacobs on 11/3/17.
 */
class SparkService : Service() {

    val CONNNECT = 1
    var device: BluetoothDevice? = null
    var inboundMessenger = Messenger(InBoundHandler)

    companion object Constants {
        val DEVICEADDRESS: String = "D5:42:2E:EA:7B:D7"
        val CONNECT = 1
        val CONNECTING = 2
        val CONNECTED = 3
    }

    override fun onBind(intent: Intent?): IBinder {
        device = intent?.extras?.get(DEVICEADDRESS) as BluetoothDevice
        return inboundMessenger.binder
    }

    object InBoundHandler : Handler() {
        override fun handleMessage(msg: Message?) {
            val message = Message.obtain()
            val outboundMessenger = msg!!.replyTo
            when(msg.what) {
                CONNECT -> {
                    message.what = CONNECTING
                    outboundMessenger.send(message)
                    // try to connect

                }
            }
        }
    }


    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }
}