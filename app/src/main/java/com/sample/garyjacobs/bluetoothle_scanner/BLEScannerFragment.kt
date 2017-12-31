package com.sample.garyjacobs.bluetoothle_scanner

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.annotation.RequiresApi
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import com.polidea.rxandroidble.RxBleClient
import com.polidea.rxandroidble.scan.ScanResult
import com.polidea.rxandroidble.scan.ScanSettings
import kotlinx.android.synthetic.main.bluetooth_scanned_list.*
import kotlinx.android.synthetic.main.scanned_item.view.*
import rx.Subscription

/**
 * Created by garyjacobs on 9/5/17.
 */
class BLEScannerFragment : Fragment() {
    val TAG = BLEScannerFragment::class.java.name
    val PERMISSION_REQUEST = 99
    var REQUEST_ENABLE_BT = 100

    var handler: Handler = Handler()

    var scanning: Boolean = false

    lateinit var bluetoothAdapter: BluetoothAdapter
    //var scanResultList: ArrayList<ScanResult> = ArrayList<ScanResult>()
    var scanResultList = emptyArray<ScanResult>().toMutableList()
    var scanSubscription: Subscription? = null

    //RxBle
    lateinit var rxBleClient: RxBleClient

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.bluetooth_scanned_list, null)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {

        rxBleClient = RxBleClient.create(this.context)
        interval_infinity_button.setOnClickListener(intervalInfinityListener)
        scan_interval_seekbar.setOnSeekBarChangeListener(seekBarListener)

        val linearLayoutManager = LinearLayoutManager(this.activity)
        linearLayoutManager.orientation = LinearLayout.VERTICAL
        scan_results_listview.layoutManager = linearLayoutManager
        scan_results_listview.adapter = MyListAdapter(scanResultList, ListItemClickListener(this))

        setScanButtonLabel(scanning)

        if (!this.activity.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this.activity, "This device does not support BLE", Toast.LENGTH_LONG).show()
            activity.finish()
        } else {
            @RequiresApi(Build.VERSION_CODES.M)
            if (locationsGranted().not()) {
                AlertDialog.Builder(this.context)
                        .setTitle("This app needs location access")
                        .setMessage("Please grant location access so this app can detect beacons.")
                        .setPositiveButton(android.R.string.ok, null)
                        .setOnDismissListener(object : DialogInterface.OnDismissListener {
                            override fun onDismiss(dialogInterface: DialogInterface?) {
                                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST)
                            }
                        })
                        .show()
            }

            Toast.makeText(this.context, "BLE Supported!!", Toast.LENGTH_LONG)
            val bluetoothManager = this.activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter

        }

        scan_startstop_button.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                scanResultList.clear()
                scan_results_listview.adapter.notifyDataSetChanged()
                scanning = scanning.not()
                setScanButtonLabel(scanning)
                scanBLEDevices(scanning)
            }
        })

    }

    override fun onStart() {
        super.onStart()
        bluetoothAdapter.enable().not().apply {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT)
        }
    }


    val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            seekBar?.let {
                current_progress_textfield.text = seekBar.resources.getString(R.string.seekbar_label, progress + 1)
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            seekBar?.let {
                Log.i(TAG, "SeekBar started at: ${seekBar.progress}")
            }
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            seekBar?.let {
                current_progress_textfield.text = seekBar.resources.getString(R.string.seekbar_label, seekBar.progress + 1)
            }
        }
    }

    val intervalInfinityListener = object : View.OnClickListener {
        override fun onClick(view: View?) {
            scanBLEDevices(false)
            scan_interval_seekbar.progress = 0
            scanBLEDevices(true, false)
        }
    }


    val stopScanningTask = object : Runnable {
        override fun run() {
            scanning = false
            setScanButtonLabel(scanning)

        }
    }

    fun setScanButtonLabel(onOff: Boolean) = if (onOff) {
        scan_startstop_button.text = "Stop Scan"
    } else {
        scan_startstop_button.text = "Start Scan"
    }

    fun scanBLEDevices(enable: Boolean = false, timed: Boolean = true) {

        if (enable) {
            scanSubscription = rxBleClient.scanBleDevices(ScanSettings.Builder().build(), com.polidea.rxandroidble.scan.ScanFilter.empty())
                    .subscribe({ nxtResult ->
                        val index = scanResultList.indexOfFirst {
                             it.bleDevice.macAddress == nxtResult.bleDevice.macAddress
                        }
                        if (index == -1) {
                            scanResultList.add(nxtResult)
                            scan_results_listview.adapter.notifyDataSetChanged()
                        } else {
                            scanResultList[index] = nxtResult
                            scan_results_listview.adapter.notifyItemChanged(index)
                        }
                    }, { error ->
                        AlertDialog.Builder(this.context)
                                .setMessage(error.message)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                    })
            if (timed) {
                handler.postDelayed(stopScanningTask, minutesMillis(scan_interval_seekbar.progress + 1))
            }
            scanning = true
        } else {
            // stop scan
            scanning = false
            scanSubscription?.let {
                if (!it.isUnsubscribed) {
                    it.unsubscribe()
                }
            }
        }
        setScanButtonLabel(scanning)

    }

    fun minutesMillis(noMinutes: Int): Long {
        return noMinutes * 60 * 1000L
    }


    fun locationsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this.activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this.activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    }

    class ListItemClickListener(val scannerFrag: BLEScannerFragment) : View.OnClickListener {
        override fun onClick(view: View?) {
            view?.let {
                scannerFrag.scanBLEDevices()
                val position = scannerFrag.scan_results_listview.getChildAdapterPosition(view)
                val adapter = scannerFrag.scan_results_listview.adapter as MyListAdapter
                val scanResult = adapter.scanResultList[position]
                Toast.makeText(view.context, "Item Clicked at $position : ${scanResult.scanRecord.deviceName}", Toast.LENGTH_LONG).show()
                if (scanResult.bleDevice.macAddress.equals(SparkService.DEVICEADDRESS)) {
                    var bundle = Bundle()
                    bundle.putParcelable(SparkService.DEVICEADDRESS, scanResult.bleDevice.bluetoothDevice)
                    var frag = SparkMainFragment()
                    frag.arguments = bundle
                    val fragmentManager = (view.context as Activity).fragmentManager
                    fragmentManager.beginTransaction()
                            .replace(R.id.main_container, frag)
                            .commit()
                }
            }
        }
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var deviceName = itemView.device_name
        var deviceAddress = itemView.device_address
        var rssi = itemView.rssi
        var connectable = itemView.connectable
    }

    class MyListAdapter(val scanResultList: List<ScanResult>, val listener: View.OnClickListener) : RecyclerView.Adapter<MyViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(parent!!.context).inflate(R.layout.scanned_item, null)
            view.setOnClickListener(listener)
            return MyViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyViewHolder?, position: Int) {
            holder?.let { viewHolder ->
                val resources = viewHolder.deviceName.resources
                val scanResult = scanResultList[position]
                viewHolder.deviceName.text = resources.getString(R.string.device_name, scanResult.scanRecord.deviceName)
                viewHolder.deviceAddress.text = resources.getString(R.string.device_address, scanResult.bleDevice.macAddress)
                viewHolder.rssi.text = resources.getString(R.string.power_level, scanResult.rssi)
                viewHolder.connectable.text = resources.getString(R.string.connectable, scanResult.scanRecord.advertiseFlags)
            }
        }

        override fun getItemCount(): Int {
            return scanResultList.size
        }

    }

}

