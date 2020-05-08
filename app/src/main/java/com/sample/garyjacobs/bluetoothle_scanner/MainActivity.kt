package com.sample.garyjacobs.bluetoothle_scanner

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.appcompat.app.AppCompatActivity
import android.widget.FrameLayout

/**
 * Created by garyjacobs on 9/2/17.
 */
class MainActivity : AppCompatActivity(), BTItemSelected {

    lateinit var mainContainer: FrameLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_activity)
        mainContainer = findViewById<FrameLayout>(R.id.main_container)
        supportFragmentManager
                .beginTransaction()
                .add(R.id.main_container, BLEScannerFragment())
                .commit()
    }

    override fun OnBTItemSelected(bundle: Bundle) {
        var newFragment = SparkMainFragment()
        newFragment.arguments = bundle
        supportFragmentManager.beginTransaction()
                .replace(R.id.main_container,newFragment)
                .addToBackStack(null)
                .commit()
    }
}

interface BTItemSelected {
    fun OnBTItemSelected(bundle: Bundle)
}