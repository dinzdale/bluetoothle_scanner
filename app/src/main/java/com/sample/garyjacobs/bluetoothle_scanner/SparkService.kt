package com.sample.garyjacobs.bluetoothle_scanner

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.os.*
import android.util.Log
import com.sample.garyjacobs.bluetoothle_scanner.utils.sphero.Command
import com.sample.garyjacobs.bluetoothle_scanner.utils.sphero.Response

/**
 * Created by garyjacobs on 11/3/17.
 */
class SparkService : Service() {
    val TAG = SparkService::class.java.simpleName
    val CONNNECT = 1
    lateinit var device: BluetoothDevice
    lateinit var gatt: BluetoothGatt
    lateinit var inboundMessenger: Messenger
    lateinit var outboundMessenger: Messenger
    var command = Command()
    var response = Response()

    companion object Constants {
        val DEVICEADDRESS: String = "D5:42:2E:EA:7B:D7"
        val CONNECT = 1
        val CONNECTING = 2
        val CONNECTIONSTATECHANGED = 3
        val CONNECTED = 4
        val SERVICESDISCOVERED = 5
        val DISCONNECTFROMSERVICE = 11
        val SERVICEDISCONNECTED = -11
        val PING = 20
        val PINGRESULT = -20
        val SETCOLORRGB = 30
        val SETBACKLED = 40
        val SETROLL = 50

        // bundle keys
        val COLORRED = "RED"
        val COLORGREEN = "GREEN"
        val COLORBLUE = "BLUE"
        val BACKLED = "BACKLED"
        val FOUNDSERVICES = "FOUNDSERVICES"
        val SPEED = "SPEED"
        val HEADING_X = "HEADING_X"
        val HEADING_Y = "HEADING_Y"

        // wake up codes
        val ANTIDOS = "011i3".toByteArray()
        val TXPWR = byteArrayOf(0x07)
        val WAKEUP = byteArrayOf(0x01)

        val ROBOT_SERVICE_UUID = "22bb746f-2ba0-7554-2d6f-726568705327"
        val ROBOT_CONTROL_CHAR_UUID = "22bb746f-2ba1-7554-2d6f-726568705327"
        val ROBOT_RESPONSE_CHAR_UUID = "22bb746f-2ba6-7554-2d6f-726568705327"

        val RADIO_SERVICE_UUID = "22bb746f-2bb0-7554-2d6f-726568705327"
        val ANTI_DOS_CHAR_UUID = "22bb746f-2bbd-7554-2d6f-726568705327"
        val TX_PWR_CHAR_UUID = "22bb746f-2bb2-7554-2d6f-726568705327"
        val WAKEUP_CHAR_UUID = "22bb746f-2bbf-7554-2d6f-726568705327"

        lateinit var robot_char_control: BluetoothGattCharacteristic
        lateinit var robot_char_response: BluetoothGattCharacteristic
        lateinit var radio_anti_dos: BluetoothGattCharacteristic
        lateinit var radio_tx_pwr: BluetoothGattCharacteristic
        lateinit var wakeup: BluetoothGattCharacteristic
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
                    sendMessage(CONNECTING)
                    gatt = device.connectGatt(context, true, gattCallBack)
                }
                DISCONNECTFROMSERVICE -> gatt.disconnect()
                PING -> {
                }
                SETCOLORRGB -> {
                    val red = incomingMessage.data.getByte(COLORRED)
                    val green = incomingMessage.data.getByte(COLORGREEN)
                    val blue = incomingMessage.data.getByte(COLORBLUE)
                    val colorrgbcmd = command.setRgbLedCmd(red, green, blue)
                    Log.d(TAG, "Setting RGB...${command.dump(colorrgbcmd)}")
                    robot_char_control.value = colorrgbcmd
                    val status = gatt.writeCharacteristic(robot_char_control)
                }
                SETBACKLED -> {
                    val value = incomingMessage.data.getByte(BACKLED)
                    val cmd = command.setBackLedCmd(value)
                    Log.d(TAG, "Setting BACK LED to $value ...${command.dump(cmd)}")
                    robot_char_control.value = cmd
                    val status = gatt.writeCharacteristic(robot_char_control)
                }
                SETROLL -> {
                    val speed = incomingMessage.data.getByte(SPEED)
                    val heading_x = incomingMessage.data.getByte(HEADING_X)
                    val heading_y = incomingMessage.data.getByte(HEADING_Y)
                    val cmd = command.setRollCmd(heading_x,heading_y,speed)
                    Log.d(TAG, "Setting ROLL to S: $speed Hx: $heading_x Hy: $heading_y ...${command.dump(cmd)}")
                    robot_char_control.value = cmd
                    val status = gatt.writeCharacteristic(robot_char_control)
                }

            // PING->gatt.writeCharacteristic()
            }
        }

    }

    val gattCallBack = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            sendMessage(CONNECTIONSTATECHANGED, status, newState)
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    gatt?.discoverServices()
                }
                BluetoothGatt.STATE_DISCONNECTED -> sendMessage(SERVICEDISCONNECTED)
            }

        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {

            gatt?.services?.let {
                findAllCharacteristics(it)
                gatt.writeCharacteristic(radio_anti_dos)
                var bundle = Bundle()
                bundle.putParcelableArray(FOUNDSERVICES, gatt.services.toTypedArray())
                sendMessage(SERVICESDISCOVERED)
            }

        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {


            } else {
                Log.d(TAG, "read failed: status: $status")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (characteristic!!.uuid.toString()) {
                    ANTI_DOS_CHAR_UUID -> gatt!!.writeCharacteristic(radio_tx_pwr)
                    TX_PWR_CHAR_UUID -> gatt!!.writeCharacteristic(wakeup)
                    WAKEUP_CHAR_UUID -> {
                        sendMessage(CONNECTED)

//                        robot_char_control.value = cmd
//                        var status = gatt!!.writeCharacteristic(robot_char_control)
//                        Log.d(TAG,"Set back led cmd sent, status:${status}, ${command.dump(cmd)}")

//                        gatt?.let {
//                            sendMessage(CONNECTED)
//                        }
//                        var cmd = command.setBackLedCmd(0x255.toByte())
//                        robot_char_control.value = cmd
//                        var status = gatt!!.writeCharacteristic(robot_char_control)
//                        Log.d(TAG,"Set back led cmd sent, status:${status}, ${command.dump(cmd)}")

//                        val bundle = Bundle()
//                        bundle.putParcelableArray("services", gatt!!.services.toTypedArray())
//                        sendMessage(SERVICESDISCOVERED, bundle = bundle)
                    }
                    else -> {
                        Log.d(TAG, "Successful ${characteristic.uuid}")
                    }
                }
            } else {
                Log.d(TAG, "write failed: status: $status")
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        gatt?.disconnect()
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

    fun findAllCharacteristics(services: List<BluetoothGattService>) {
        val robotService = services
                .find { it.uuid.toString() == ROBOT_SERVICE_UUID }
        (robotService!!.characteristics.find { it.uuid.toString() == ROBOT_CONTROL_CHAR_UUID })?.let {
            robot_char_control = it
            Log.d(TAG, "WRITE CHARACTERISTC FOUND")
        }
        (robotService!!.characteristics.find { it.uuid.toString() == ROBOT_RESPONSE_CHAR_UUID })?.let {
            robot_char_response = it
            Log.d(TAG, "READ CHARACTERISTC FOUND")

        }

        val radioService = services
                .find { it.uuid.toString() == RADIO_SERVICE_UUID }
        (radioService!!.characteristics.find { it.uuid.toString() == ANTI_DOS_CHAR_UUID })?.let {
            radio_anti_dos = it
            radio_anti_dos.value = ANTIDOS
        }
        (radioService!!.characteristics.find { it.uuid.toString() == TX_PWR_CHAR_UUID })?.let {
            radio_tx_pwr = it
            radio_tx_pwr.value = TXPWR
        }
        (radioService!!.characteristics.find { it.uuid.toString() == WAKEUP_CHAR_UUID })?.let {
            wakeup = it
            wakeup.value = WAKEUP
        }

    }

}