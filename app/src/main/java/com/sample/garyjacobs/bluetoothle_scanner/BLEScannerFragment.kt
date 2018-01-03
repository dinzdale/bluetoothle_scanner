package com.sample.garyjacobs.bluetoothle_scanner

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
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
import android.support.v7.widget.LinearLayoutCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.bluetooth_scanned_list.*

import kotlin.collections.ArrayList

/**
 * Created by garyjacobs on 9/5/17.
 */
class BLEScannerFragment : Fragment() {
    val TAG = BLEScannerFragment::class.java.name
    val PERMISSION_REQUEST = 99
    var REQUEST_ENABLE_BT = 100

    var handler: Handler = Handler()

    var myActivity: MainActivity? = null
    var scanning: Boolean = false

    lateinit var bluetoothAdapter: BluetoothAdapter
    var scanRecords: ArrayList<ScanResult> = ArrayList<ScanResult>()
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        myActivity = activity as MainActivity
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.bluetooth_scanned_list, null) as View
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {

        interval_infinity_button.setOnClickListener(intervalInfinityListener)
        scan_interval_seekbar.setOnSeekBarChangeListener(seekBarListener)

        val linearLayoutManager = LinearLayoutManager(this.activity)
        linearLayoutManager.orientation = LinearLayoutCompat.VERTICAL
        scan_results_listview.layoutManager = linearLayoutManager
        scan_results_listview.adapter = MyListAdapter(scanRecords, ListItemClickListener())

        setScanButtonLabel(scanning)

        if (!this.activity.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this.activity, "This device does not support BLE", Toast.LENGTH_LONG).show()
            this.activity.finish()
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
            val bluetoothManager: BluetoothManager = this.activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter

        }

        scan_startstop_button?.setOnClickListener(
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        scanRecords.clear()
                        scan_results_listview.adapter.notifyDataSetChanged()
                        scanning = scanning.not()
                        setScanButtonLabel(scanning)
                        scanBLEDevices(scanning)
                    }
                })


    }

    override fun onStart() {
        super.onStart()
        bluetoothAdapter?.let {
            it.enable().not().apply {
                startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT)
            }
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
            setScanButtonLabel(true)
            scanBLEDevices(true, false)
        }
    }

    val stopScanningTask = object : Runnable {
        override fun run() {
            scanning = false
            setScanButtonLabel(scanning)
            bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallBack)
        }
    }

    fun setScanButtonLabel(onOff: Boolean) = if (onOff) {
        scan_startstop_button.text = "Stop Scan"
    } else {
        scan_startstop_button.text = "Start Scan"
    }

    fun scanBLEDevices(enable: Boolean = false, timed: Boolean = true) {

        handler.removeCallbacks(stopScanningTask)

        if (enable) {
            // start scan
            if (timed) {
                handler.postDelayed(stopScanningTask, minutesMillis(scan_interval_seekbar.progress + 1))
            }
            scanning = true
            bluetoothAdapter.bluetoothLeScanner?.startScan(scanCallBack)
        } else {
            // stop scan
            scanning = false
            bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallBack)
        }
        setScanButtonLabel(scanning)
    }

    fun minutesMillis(noMinutes: Int): Long {
        return noMinutes * 60 * 1000L
    }

    val scanCallBack = object : ScanCallback() {

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "Failed scan $errorCode")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                val index = scanRecords.indexOfFirst({
                    it.scanRecord.deviceName == result.scanRecord.deviceName && it.device.address == result.device.address
                })
                if (index == -1) {
                    scanRecords.add(result)
                    scan_results_listview.adapter.notifyDataSetChanged()
                } else {
                    Log.i(TAG, "Updating ${result.scanRecord.toString()}")
                    scanRecords[index] = result
                    scan_results_listview.adapter.notifyItemChanged(index)
                }
            }
            Log.i(TAG, " ScanCallback: callbackType: $callbackType result: ${result.toString()}")
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }

    }

    fun locationsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this.activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this.activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    }

    inner class ListItemClickListener() : View.OnClickListener {
        override fun onClick(view: View?) {
            view?.let {
                val position = this@BLEScannerFragment.scan_results_listview.getChildAdapterPosition(view)
                val adapter = this@BLEScannerFragment.scan_results_listview.adapter as MyListAdapter
                val scanResult = adapter.scanResultList[position]
                if (scanResult?.device?.address.equals(SparkService.DEVICEADDRESS)) {
                    this@BLEScannerFragment.scan_startstop_button.performClick()
                    var bundle = Bundle()
                    bundle.putParcelable(SparkService.DEVICEADDRESS, scanResult.device)
                    myActivity?.OnBTItemSelected(bundle)
                }
            }
        }
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        lateinit var deviceName: TextView
        lateinit var deviceAddress: TextView
        lateinit var rssi: TextView
        lateinit var connectable: TextView

        init {
            deviceName = itemView.findViewById(R.id.device_name)
            deviceAddress = itemView.findViewById(R.id.device_address)
            rssi = itemView.findViewById(R.id.rssi)
            connectable = itemView.findViewById(R.id.connectable)

        }

    }

    class MyListAdapter(val scanResultList: ArrayList<ScanResult>, val listener: View.OnClickListener) : RecyclerView.Adapter<MyViewHolder>() {

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
                viewHolder.deviceAddress.text = resources.getString(R.string.device_address, scanResult.device.address)
                viewHolder.rssi.text = resources.getString(R.string.power_level, scanResult.rssi)
                viewHolder.connectable.text = resources.getString(R.string.connectable, scanResult.scanRecord.advertiseFlags)
            }
        }

        override fun getItemCount(): Int {
            return scanResultList.size
        }

    }

}

