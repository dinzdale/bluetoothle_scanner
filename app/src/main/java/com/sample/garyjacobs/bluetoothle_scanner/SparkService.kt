package com.sample.garyjacobs.bluetoothle_scanner

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Context
import android.content.Intent
import android.os.*

/**
 * Created by garyjacobs on 11/3/17.
 */
class SparkService : Service() {

    val CONNNECT = 1
    lateinit var device: BluetoothDevice
    lateinit var inboundMessenger: Messenger
    lateinit var outboundMessenger: Messenger

    companion object Constants {
        val DEVICEADDRESS: String = "D5:42:2E:EA:7B:D7"
        val CONNECT = 1
        val CONNECTING = 2
        val CONNECTIONSTATECHANGED = 3
        val SERVICESFOUND = 5
    }

    override fun onBind(intent: Intent?): IBinder {
        device = intent?.extras?.get(DEVICEADDRESS) as BluetoothDevice
        inboundMessenger = Messenger(InBoundHandler(this, device))
        return inboundMessenger.binder
    }

    inner class InBoundHandler(val context: Context, val device: BluetoothDevice) : Handler() {

        override fun handleMessage(incomingMessage: Message?) {
            val message = Message.obtain()
            outboundMessenger = incomingMessage!!.replyTo
            when (incomingMessage.what) {
                CONNECT -> {
                    message.what = CONNECTING
                    outboundMessenger.send(message)
                    device.connectGatt(context, true, gattCallBack)
                }
            }
        }

    }

    val gattCallBack = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val message = Message.obtain()
            message.what = status
            message.arg1 = newState
            outboundMessenger.send(message)
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt?.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {

            gatt?.services?.let {
                val message = Message.obtain()
                message.what = SERVICESFOUND
                val bundle = Bundle()
                bundle.putParcelableArray("services", it.toTypedArray())
                message.data = bundle
                outboundMessenger.send(message)
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }
}