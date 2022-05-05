package com.nikolay.meteringdevice

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children


class MainActivity : AppCompatActivity() {
    // UI
    var btn_connect: Button? = null
    var btn_start_scan: Button? = null
    var btn_start: Button? = null
    var btn_add_rpm: ImageButton? = null
    var txt_target_rpm: TextView? = null
    var txt_single_misses: TextView? = null
    var txt_double_misses: TextView? = null
    var txt_total_misses: TextView? = null

    var lt_rpm_values: LinearLayout? = null

    // bluetooth
    private val BLUETOOTH_PERMISSION_REQUEST_CODE: Int = 1
    private var txt_scan_status: TextView? = null
    private var txt_status: TextView? = null
    var meteringConnection: MeteringConnect? = null

    //device

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initUI()
        initializeBluetoothOrRequestPermission()
        initActions()

        ControlPreferences.loadRPMSettinngs(this)

    }

    private fun initUI()
    {
        //buttons
        btn_connect = findViewById(R.id.btn_connect)
        btn_start_scan = findViewById(R.id.btn_start_scan)
        btn_start = findViewById(R.id.btn_start)
        btn_add_rpm = findViewById(R.id.btn_add_rpm)

        //labels
        txt_scan_status = findViewById(R.id.lbl_scan_status)
        txt_status = findViewById(R.id.txt_status)
        txt_target_rpm = findViewById(R.id.txt_target_rpm)
        txt_single_misses = findViewById(R.id.txt_one_miss)
        txt_double_misses = findViewById(R.id.txt_double_miss)
        txt_total_misses = findViewById(R.id.txt_total_miss)
        // layouts
        lt_rpm_values = findViewById(R.id.all_rpm_values)

    }

    private fun initActions()
    {
        meteringConnection?.setStartButton(btn_start)

        btn_add_rpm?.setOnClickListener {
            val rpm_entry = RPMEntry(this@MainActivity)
            lt_rpm_values?.addView(rpm_entry, lt_rpm_values!!.childCount - 1)
            ControlPreferences.saveRPMsettings(this)
        }
        txt_single_misses?.setOnClickListener {
            meteringConnection?.singleMisses = 0
        }
        txt_double_misses?.setOnClickListener {
            meteringConnection?.doubleMisses = 0
        }
        txt_total_misses?.setOnClickListener {
            meteringConnection?.totalMisses = 0
        }
    }

    // Device related
    var deviceStarted = false
        set(value) {
            field = value
            btn_start?.text = if (value) "Stop device" else "Start device"
        }


    // Bluetooth related ------------------------------------------------------------------
    private fun initializeBluetoothOrRequestPermission() {
        val requiredPermissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        }

        val missingPermissions = requiredPermissions.filter { permission ->
            checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isEmpty()) {
            initializeBluetooth()
        } else {
            requestPermissions(missingPermissions.toTypedArray(), BLUETOOTH_PERMISSION_REQUEST_CODE)
        }
    }

    private fun initializeBluetooth() {
        meteringConnection = MeteringConnect( this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            BLUETOOTH_PERMISSION_REQUEST_CODE -> {
                if (grantResults.none { it != PackageManager.PERMISSION_GRANTED }) {
                    // all permissions are granted
                    initializeBluetooth()
                } else {
                    // some permissions are not granted
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    var isScanning = false
        set(value) {
            field = value
            runOnUiThread {
                btn_start_scan?.text = if (value) "Stop Scan" else "Start Scan"
                txt_scan_status?.text = if (value) "is scanning" else "not scanning"
            }
        }

    var deviceConnected = false
        set(value) {
            field = value
            runOnUiThread {
                btn_connect?.text = if (value) "Disconnect" else "Connect"
                btn_connect?.setEnabled(true)
                txt_status?.text = if (value) "is connected" else "disconnected"
            }
        }

    @SuppressLint("MissingPermission")
    override fun onStop() {
        super.onStop()
        meteringConnection?.gattConnction?.disconnect()
        meteringConnection?.gattConnction?.close()
        deviceConnected = false
        Log.w("Destroy callback", "On stop called")
    }
}