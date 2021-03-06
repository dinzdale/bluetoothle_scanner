package com.sample.garyjacobs.bluetoothle_scanner


import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.content.ComponentName
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.TypedArray
import android.graphics.Color
import android.os.*
import android.text.method.MovementMethod
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.sample.garyjacobs.bluetoothle_scanner.utils.sphero.Command
import kotlinx.android.synthetic.main.spark_main_fragment.*

/**
 * Created by garyjacobs on 11/3/17.
 */
class SparkMainFragment() : Fragment() {
    val TAG = SparkMainFragment::class.java.simpleName
    val command = Command()
    var connectingDlg: AlertDialog? = null
    lateinit var device: BluetoothDevice
    lateinit var inboundMessenger: Messenger
    lateinit var outboundMessenger: Messenger
    var bound = false
    var connected = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.spark_main_fragment, null)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getParcelable<BluetoothDevice>(SparkService.DEVICEADDRESS)?.let{
            device = it
        }
        color_picker_view.addOnColorChangedListener { color ->
            val red = Color.red(color).toByte()
            val green = Color.green(color).toByte()
            val blue = Color.blue(color).toByte()
            val bundle = Bundle()
            bundle.putByte(SparkService.COLORRED, red)
            bundle.putByte(SparkService.COLORGREEN, green)
            bundle.putByte(SparkService.COLORBLUE, blue)
            sendMessage(SparkService.SETCOLORRGB, bundle)
        }
        color_picker_view.addOnColorSelectedListener { color ->
            val red = Color.red(color).toByte()
            val green = Color.green(color).toByte()
            val blue = Color.blue(color).toByte()
            val bundle = Bundle()
            bundle.putByte(SparkService.COLORRED, red)
            bundle.putByte(SparkService.COLORGREEN, green)
            bundle.putByte(SparkService.COLORBLUE, blue)
            sendMessage(SparkService.SETCOLORRGB, bundle)
        }

        back_led_sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val bundle = Bundle()
                bundle.putByte(SparkService.BACKLED, progress.toByte())
                sendMessage(SparkService.SETBACKLED, bundle)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        spin_sb.setOnSeekBarChangeListener(object : CircularSeekBar.OnCircularSeekBarChangeListener {
            override fun onProgressChanged(circularSeekBar: CircularSeekBar?, progress: Int, fromUser: Boolean) {
                val bundle = Bundle()
                val progB = progress.toByte()
                bundle.putByte(SparkService.SPEED, 50.toByte())
                bundle.putByte(SparkService.HEADING_X, 0.toByte())
                bundle.putByte(SparkService.HEADING_Y, progB)
                sendMessage(SparkService.SETROLL, bundle)
            }

            override fun onStopTrackingTouch(seekBar: CircularSeekBar?) {
            }

            override fun onStartTrackingTouch(seekBar: CircularSeekBar?) {
            }
        })

        services_tf.movementMethod = ScrollingMovementMethod()

        connectingDlg = AlertDialog.Builder(this.context!!)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.connecting_warning)
                .show()

    }


    fun ColorToHex(intColor: Int): Triple<Byte, Byte, Byte> {

        val red = Color.red(intColor).toByte()
        val green = Color.green(intColor).toByte()
        val blue = Color.blue(intColor).toByte()
        return Triple(red, green, blue)
    }

    override fun onResume() {
        super.onResume()
        var intent = Intent(this.context, SparkService::class.java)
        intent.putExtra(SparkService.DEVICEADDRESS, device)
        context?.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()
        connectingDlg?.dismiss()
        var intent = Intent(this.context, SparkService::class.java)
        intent.putExtra(SparkService.DEVICEADDRESS, device)
        this.context?.unbindService(serviceConnection)
    }

    inner class InboundHandler : Handler() {

        override fun handleMessage(msg: Message?) {
            when (msg!!.what) {
                SparkService.CONNECTIONSTATECHANGED -> if (msg.arg1 == BluetoothGatt.GATT_SUCCESS) {
                    when (msg.arg2) {
                        BluetoothGatt.STATE_CONNECTED -> handleConnected()
                        BluetoothGatt.STATE_DISCONNECTED -> handleDisconnected()
                    }
                }
                SparkService.SERVICESDISCOVERED -> services_tf.text = msg.data.getString(SparkService.FOUNDSERVICES)
                SparkService.CONNECTED -> connectingDlg?.hide()
                SparkService.SERVICEDISCONNECTED -> handleDisconnected()

            }
        }
    }

    fun handleConnected() = {
    }

    fun handleDisconnected() = {
    }

    fun handleServices(services: Array<BluetoothGattService>) {
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

    fun sendMessage(messageNumber: Int, bundle: Bundle? = null) {
        var message = Message.obtain()
        message.replyTo = inboundMessenger
        message.what = messageNumber
        bundle?.let { message.data = bundle }
        outboundMessenger.send(message)
    }
}
