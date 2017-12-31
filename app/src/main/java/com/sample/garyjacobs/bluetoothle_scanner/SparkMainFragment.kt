package com.sample.garyjacobs.bluetoothle_scanner

import android.app.Fragment
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.content.ComponentName
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.TypedArray
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.sample.garyjacobs.bluetoothle_scanner.utils.BleNamesResolver

/**
 * Created by garyjacobs on 11/3/17.
 */
class SparkMainFragment() : Fragment() {
    val TAG = SparkMainFragment::class.java.simpleName
    lateinit var statusTF: TextView
    lateinit var device: BluetoothDevice
    lateinit var inboundMessenger: Messenger
    lateinit var outboundMessenger: Messenger
    var bound = false

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater?.inflate(R.layout.spark_main_fragment, null)!!
        statusTF = view.findViewById(R.id.spark_status)
        device = arguments.getParcelable<BluetoothDevice>(SparkService.DEVICEADDRESS)

        return view
    }

    override fun onResume() {
        super.onResume()
        var intent = Intent(this.context, SparkService::class.java)
        intent.putExtra(SparkService.DEVICEADDRESS, device)
        this.context.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()
        var intent = Intent(this.context, SparkService::class.java)
        intent.putExtra(SparkService.DEVICEADDRESS, device)
        this.context.unbindService(serviceConnection)
    }

    inner class InboundHandler : Handler() {

        override fun handleMessage(msg: Message?) {
            when (msg!!.what) {
                SparkService.CONNECTING -> statusTF.text = "CONNECTING"
                SparkService.CONNECTIONSTATECHANGED ->
                    if (msg.arg1 == BluetoothGatt.GATT_SUCCESS) when (msg.arg2) {
                        BluetoothGatt.STATE_CONNECTED -> handleConnected()
                        BluetoothGatt.STATE_DISCONNECTED -> handleDisconnected()
                    }
                SparkService.SERVICESDISCOVERED -> handleServices(msg.data.get("services") as Array<BluetoothGattService>)
                SparkService.SERVICEDISCONNECTED -> handleDisconnected()

            }
        }
    }

    fun handleConnected() = {
        statusTF.text = "Device connected...Waiting For Services"
    }

    fun handleDisconnected() = {
        statusTF.text = "Device disconnected..."
    }

    fun handleServices(services: Array<BluetoothGattService>) {

        statusTF.text = "${services.size} Services Found"
        Log.d(TAG, "${services.size} services found")
        services.forEachIndexed { index, bluetoothGattService ->
            Log.d(TAG, "SERVICE $index ${bluetoothGattService.uuid}:${BleNamesResolver.resolveUuid(bluetoothGattService.uuid.toString())}")
            bluetoothGattService.includedServices.forEachIndexed { index, bluetoothGattService ->
                Log.d(TAG, "included service $index UUID: ${bluetoothGattService.uuid}:${BleNamesResolver.resolveUuid(bluetoothGattService.uuid.toString())}")
            }
            bluetoothGattService.characteristics.forEachIndexed { index, bluetoothGattCharacteristic ->
                Log.d(TAG, "characterisitic $index UUID: ${bluetoothGattCharacteristic.uuid}:${BleNamesResolver.resolveUuid(bluetoothGattCharacteristic.uuid.toString())}")
                bluetoothGattCharacteristic.descriptors.forEachIndexed { index, bluetoothGattDescriptor ->
                    Log.d(TAG, "descriptor $index desc: ${bluetoothGattDescriptor.characteristic.uuid}:${BleNamesResolver.resolveUuid(bluetoothGattCharacteristic.uuid.toString())}")
                }
            }
        }
        sendMessage(SparkService.DISCONNECTFROMSERVICE)
    }

    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bound = true
            outboundMessenger = Messenger(service)
            inboundMessenger = Messenger(InboundHandler())
            sendMessage(SparkService.CONNECT)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    fun sendMessage(messageNumber: Int) {
        var message = Message.obtain()
        message.replyTo = inboundMessenger
        message.what = messageNumber
        outboundMessenger.send(message)
    }
}
