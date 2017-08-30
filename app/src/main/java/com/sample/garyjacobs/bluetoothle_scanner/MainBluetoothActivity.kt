package com.sample.garyjacobs.bluetoothle_scanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast

class MainBluetoothActivity : AppCompatActivity() {

    val TAG = "MainBluetoothActivitygit"
    val SCANPERIOD = 30000L
    val PERMISSION_REQUEST = 99
    var REQUEST_ENABLE_BT = 100

    var bluetoothAdapter: BluetoothAdapter? = null

    var scanning: Boolean = false;

    var handler = Handler()

    lateinit var scanButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_activity)
        // check if this device in BLE compatible
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "This device does not support BLE", Toast.LENGTH_LONG).show()
            finish()
        } else {
            if (locationsGranted().not()) {
                AlertDialog.Builder(this)
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

            Toast.makeText(this, "BLE Supported!!", Toast.LENGTH_LONG)
            val bluetoothManager: BluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter

        }

        scanButton = findViewById<Button>(R.id.scan_button)

        scanButton?.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                (view as Button).isEnabled = false
                Toast.makeText(view?.context, "Starting scan...", Toast.LENGTH_LONG).show()
                scanBLEDevices(true)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        for (i in permissions.indices) {
            Log.i(TAG, "Permission: ${permissions[i]} is ${grantResults[i]}")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "BLE on result: requestCode: ${requestCode} resulteCode: ${resultCode}")
    }

    fun scanBLEDevices(enable: Boolean = false) {

        if (enable) {
            // start scan
            handler.postDelayed(object : Runnable {
                override fun run() {
                    scanButton.isEnabled = true
                    scanning = false
                    bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallBack)
                }
            }, SCANPERIOD)
            scanning = true
            bluetoothAdapter?.bluetoothLeScanner?.startScan(scanCallBack)

        } else {
            // stop scan
            scanning = false
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallBack)
        }

    }

    val scanCallBack = object : ScanCallback() {

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "Failed scan ${errorCode}")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            Log.i(TAG, result.toString())
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }

    }

    fun locationsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this@MainBluetoothActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this@MainBluetoothActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    }
}
