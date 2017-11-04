package com.sample.garyjacobs.bluetoothle_scanner

import android.app.Fragment
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * Created by garyjacobs on 11/3/17.
 */
class SparkMainFragment() : Fragment() {

    lateinit var statusTF: TextView
    lateinit var device: BluetoothDevice
    lateinit var inboundMessenger: Messenger
    lateinit var outboundMessenger: Messenger
    var bound = false

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater?.inflate(R.layout.spark_main_fragment, null)!!
        statusTF = view.findViewById(R.id.spark_status)
        device = arguments.getParcelable<BluetoothDevice>(SparkService.DEVICEADDRESS)
        var intent = Intent(this.context, SparkService.javaClass)
        intent.putExtra(SparkService.DEVICEADDRESS,device)
        this.context.bindService(intent, serivceConnection, BIND_AUTO_CREATE)
        return view
    }

    class InboundHandler(val frag: SparkMainFragment) : Handler() {

        override fun handleMessage(msg: Message?) {
            when (msg!!.what) {
                SparkService.CONNECTED -> frag.statusTF.text = "CONNECTED"
            }
        }
    }

    val serivceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bound = true
            outboundMessenger = Messenger(service)
            inboundMessenger = Messenger(InboundHandler(this@SparkMainFragment))
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