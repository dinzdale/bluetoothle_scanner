package com.sample.garyjacobs.bluetoothle_scanner

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.content.Intent
import android.os.*
import android.support.v4.content.ParallelExecutorCompat

/**
 * Created by garyjacobs on 11/3/17.
 */
class SparkService : Service() {

    val CONNNECT = 1
    lateinit var device: BluetoothDevice
    lateinit var gatt : BluetoothGatt
    lateinit var inboundMessenger: Messenger
    lateinit var outboundMessenger: Messenger

    companion object Constants {
        val DEVICEADDRESS: String = "D5:42:2E:EA:7B:D7"
        val CONNECT = 1
        val CONNECTING = 2
        val CONNECTIONSTATECHANGED = 3
        val SERVICESDISCOVERED = 5
        val DISCONNECTFROMSERVICE = 11
        val SERVICEDISCONNECTED = -11
        val PING = 20
        val PINGRESULT = -20
    }

    override fun onBind(intent: Intent?): IBinder {
        device = intent?.extras?.get(DEVICEADDRESS) as BluetoothDevice
        inboundMessenger = Messenger(InBoundHandler(this, device))
        return inboundMessenger.binder
    }

    inner class InBoundHandler(val context: Context, val device: BluetoothDevice) : Handler() {

        override fun handleMessage(incomingMessage: Message?) {
            outboundMessenger = incomingMessage!!.replyTo

            when (incomingMessage.what) {
                CONNECT -> {
                    sendMessage(CONNNECT)
                    gatt = device.connectGatt(context, true, gattCallBack)
                }
                DISCONNECTFROMSERVICE -> gatt.disconnect()
                PING->gatt.readCharacteristic()
                PING->gatt.writeCharacteristic()
            }
        }

    }

    val gattCallBack = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            sendMessage(CONNECTIONSTATECHANGED, status, newState)
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> gatt?.discoverServices()
                BluetoothGatt.STATE_DISCONNECTED -> sendMessage(SERVICEDISCONNECTED)
            }

        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {

            gatt?.services?.let {
                val bundle = Bundle()
                bundle.putParcelableArray("services", it.toTypedArray())
                sendMessage(SERVICESDISCOVERED, bundle = bundle)
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    fun sendMessage(what: Int, arg1: Int? = null, arg2: Int? = null, bundle: Bundle? = null) {
        val message = Message.obtain()
        message.what = what
        bundle?.let {
            message.data = bundle
        }
        arg1?.let {
            message.arg1 = arg1
        }
        arg2?.let {
            message.arg2 = arg2
        }
        outboundMessenger.send(message)
    }
}