package com.sample.garyjacobs.bluetoothle_scanner

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.activity_main.*
/**
 * Created by garyjacobs on 9/2/17.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_activity)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.main_container, BLEScannerFragment())
                .commit()
    }
}