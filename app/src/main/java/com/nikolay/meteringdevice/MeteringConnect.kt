package com.nikolay.meteringdevice

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.schedule


@SuppressLint("MissingPermission")
class MeteringConnect(private val context: Context) {
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    var gattConnction: BluetoothGatt? = null
    private var theDevice: BluetoothDevice? = null

    private var the_timer_handler: TimerTask? = null
    private var readPending = false


    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    @SuppressLint("MissingPermission")
    private fun startBleScan()
    {
        val scanFilters: List<ScanFilter> = listOf(
            ScanFilter.Builder()
                .setDeviceName(THE_DEVICE_NAME)
                .build()
        )
        if (!(context as MainActivity).deviceConnected)
        {
            bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
            (context as MainActivity).isScanning = true
        }
        else
        {
            Toast.makeText(context, "The device already connected disconnect first!",
                Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan()
    {
        bluetoothLeScanner?.stopScan(scanCallback)
        (context as MainActivity).isScanning = false
    }

    private val scanCallback = object : ScanCallback()
    {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            theDevice = result.device
            if(theDevice?.name == THE_DEVICE_NAME)
            {
                Log.i("ScanCallback", "Found BLE device! Name: " +
                        "${theDevice?.name ?: "Unnamed"}, " + "address: ${theDevice?.address}")
                stopBleScan()
                connectDevice()
            }
        }
    }

    // GATT callbacks ------------------------------------------------------------------------------
    private val gattCallback = object : BluetoothGattCallback()
    {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int)
        {
            val deviceAddress = gatt.device.address
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                if (newState == BluetoothProfile.STATE_CONNECTED)
                {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                    (context as MainActivity).deviceConnected = true
                    try{
                        Toast.makeText(context, "Device connected!",
                            Toast.LENGTH_SHORT).show()
                    } catch (e: RuntimeException){ }

                    Handler(Looper.getMainLooper()).post {
                        gattConnction?.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED)
                {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from " +
                            deviceAddress
                    )
                    gatt.close()
                    (context as MainActivity).deviceConnected = false
                    Toast.makeText(context, "Device disconnected!",
                        Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.w("BluetoothGattCallback", "Error $status encountered for " +
                        "$deviceAddress! Disconnecting...")
                gatt.close()
                (context as MainActivity).deviceConnected = false
            }
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int)
        {
            with(gatt) {
                Log.w("BluetoothGattCallback", "Discovered ${services.size} services for" +
                        " ${device.address}")
                subscribeToseedMiss()
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        onValueHaveRead(uuid, value)
                        Log.i("BluetoothGattCallback", "Read characteristic " +
                                "$uuid:\n${value.toHexString()}")
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Characteristic read failed for " +
                                "$uuid, error: $status")
                        Toast.makeText(context, "Failed to read ${charMap[uuid]}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
            readPending = false
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        onValueHasWritten(uuid, value)
                        Log.i("BluetoothGattCallback", "Wrote to characteristic " +
                                "$uuid | value: ${value.toHexString()}")
                    }
                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
                        Log.e("BluetoothGattCallback",
                            "Write exceeded connection ATT MTU!")
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback",
                            "Write not permitted for $uuid!")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Characteristic write failed " +
                                "for $uuid, error: $status")
                    }
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            with(descriptor)
            {
                when (status)
                {
                    BluetoothGatt.GATT_SUCCESS -> {
                        onDeviceConnected()
                        Log.i("BluetoothGattCallback", "Wrote to descriptor " +
                                "${this?.uuid} | value: ${this?.value?.toHexString()}")
                    }
                    else -> {
                        if(this?.uuid == seedMisslDescUuid)
                        {
                            Toast.makeText(context, "Failed to sibscribe to ${charMap[seedMissUuid]}",
                                Toast.LENGTH_SHORT).show()
                            subscribeToseedMiss()
                        }
                        Log.e("BluetoothGattCallback", "Descriptor write failed " +
                                "for ${this?.uuid}, error: $status")

                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic) {
                onValueChanged(uuid, value)
                Log.i("BluetoothGattCallback", "Characteristic $uuid changed | value: " +
                        value.toHexString()
                )
            }
        }

    }

    //-----------------------------------------------------------------------------------------------------------

    fun ByteArray.toHexString(): String =
        joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }

    @SuppressLint("MissingPermission")
    private fun subscribeToseedMiss() {

        val seedMissChar = gattConnction?.getService(meteringServiceUuid)?.getCharacteristic(
            seedMissUuid)

        gattConnction?.setCharacteristicNotification(seedMissChar, true)
        var descriptor: BluetoothGattDescriptor? = seedMissChar?.getDescriptor(seedMisslDescUuid)
        descriptor?.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        gattConnction?.writeDescriptor(descriptor)
        gattConnction?.setCharacteristicNotification(seedMissChar, true)
    }

    @SuppressLint("MissingPermission")
    private fun readRPMLevel() {
        val rpmLevelChar = gattConnction?.getService(meteringServiceUuid)?.getCharacteristic(
            speedLevelCharUuid)
        gattConnction?.readCharacteristic(rpmLevelChar)
    }

    @SuppressLint("MissingPermission")
    fun writeRPM(rpmValue: UByte) {
        val rpmChar = gattConnction?.getService(meteringServiceUuid)?.getCharacteristic(
            speedLevelCharUuid)
        if (rpmChar != null) {
            writeCharacteristic(rpmChar, byteArrayOf(rpmValue.toByte()))
        }
    }

    @SuppressLint("MissingPermission")
    private fun readStart() {
        val startChar = gattConnction?.getService(meteringServiceUuid)?.getCharacteristic(
            startCharUuid)
        gattConnction?.readCharacteristic(startChar)
    }

    @SuppressLint("MissingPermission")
    fun writeStart(startValue: Boolean) {
        val startChar = gattConnction?.getService(meteringServiceUuid)?.getCharacteristic(
            startCharUuid)
        if (startChar != null) {
            writeCharacteristic(startChar, byteArrayOf(startValue.toByte()))
        }
    }

    @SuppressLint("MissingPermission")
    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, payload: ByteArray) {
        gattConnction?.let { gatt ->
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.value = payload
            gatt.writeCharacteristic(characteristic)
        } ?: error("Not connected to a BLE device!")
    }

    @SuppressLint("MissingPermission")
    fun connectDevice()
    {
        if(theDevice != null)
        {
            gattConnction = theDevice?.connectGatt(this@MeteringConnect.context,
                false, gattCallback)
            (context as MainActivity).btn_connect?.setEnabled(false)
            the_timer_handler?.cancel()
            the_timer_handler = Timer().schedule(1000) {
                (context as MainActivity).btn_connect?.setEnabled(true)
            }
        }
        else
        {
            Toast.makeText(context, "No device saved, please scan!!",
                Toast.LENGTH_SHORT).show()
        }

    }

    companion object
    {
        private val THE_DEVICE_NAME: String = "MeteringDevice"
        private val meteringServiceUuid: UUID = UUID.fromString("00009950-0000-1000-8000-00805f9b34fb")

        private val speedLevelCharUuid: UUID = UUID.fromString("00001a00-0000-1000-8000-00805f9b34fb")

        private val startCharUuid: UUID = UUID.fromString("00001a01-0000-1000-8000-00805f9b34fb")

        private val seedMissUuid: UUID = UUID.fromString("00001a02-0000-1000-8000-00805f9b34fb")
        private val seedMisslDescUuid: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private  val charMap = hashMapOf(
            speedLevelCharUuid to "rpm value",
            startCharUuid to "start state value",
            seedMissUuid to "seed miss value"
        )
    }



//    private fun BluetoothGatt.printGattTable() {
//        if (services.isEmpty()) {
//            Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?")
//            return
//        }
//        services.forEach { service ->
//            val characteristicsTable = service.characteristics.joinToString(
//                separator = "\n|--",
//                prefix = "|--"
//            ) { it.uuid.toString() }
//            Log.i("printGattTable", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
//            )
//        }
//    }
    init {
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter =  bluetoothManager?.getAdapter()
        if (bluetoothAdapter != null) {
            bluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner
            if(bluetoothLeScanner == null)
            {
                Log.i("MDC", "bluetooth is not enabled")
            }
        }
        else
        {
            Log.i("MDC", "no support for bluetooth")
        }
        (context as MainActivity).btn_start_scan?.setOnClickListener {
            if (context.isScanning) {
                stopBleScan()
            } else {
                startBleScan()
            }
        }
        (context as MainActivity).btn_connect?.setOnClickListener {
            if (context.deviceConnected) {
                gattConnction?.disconnect()
                gattConnction?.close()
                (context as MainActivity).deviceConnected = false
            } else {
                connectDevice()
            }
        }
        startBleScan()
    }

    fun setStartButton(btn_start: Button?)
    {
        btn_start?.setOnClickListener {
            if(!(context as MainActivity).deviceConnected)
            {
                Toast.makeText(context, "The device is not connected!",
                    Toast.LENGTH_SHORT).show()
            }
            else
            {
                writeStart(!deviceStarted)
            }
        }
    }

    //------------------------------------CLEAN FUNCTIONS ------------------------------------------------
    var deviceStarted = false
    set(value) {
        field = value
        val ma = context as MainActivity
        ma.runOnUiThread {
            ma.btn_start?.text = if (value) "Stop" else "Start"
        }
    }
    var currentRPM: UByte = 0u
        set(value) {
            field = value
            val ma = context as MainActivity
            ma.runOnUiThread {
                ma.txt_target_rpm?.text = value.toString()
            }
        }
    var singleMisses = 0
        set(value) {
            field = value
            val ma = context as MainActivity
            ma.txt_single_misses?.text = value.toString()
            if (value != 0) totalMisses ++
        }
    var doubleMisses = 0
        set(value) {
            field = value
            val ma = context as MainActivity
            ma.txt_double_misses?.text = value.toString()
            if (value != 0) totalMisses += 2
        }
    var totalMisses = 0
        set(value) {
            field = value
            val ma = context as MainActivity
            ma.txt_total_misses?.text = value.toString()
        }


    fun onDeviceConnected()
    {
        //read the state of the device
        readStart()
        readPending = true
        GlobalScope.launch() {
            var i = 0
            while (i <20)
            {
                if (!readPending)
                {
                    readRPMLevel()
                    break
                }
                delay(50)
                i++
            }
        }
    }

    private fun onValueHaveRead(valId: UUID, value: ByteArray)
    {
        updateUistate(valId, value)
    }

    private fun onValueHasWritten(valId: UUID, value: ByteArray)
    {
        updateUistate(valId, value)
    }

    private  fun onValueChanged(valId: UUID, value: ByteArray)
    {
        updateUistate(valId, value)
    }

    private fun updateUistate(valId: UUID, value: ByteArray)
    {
        when(valId)
        {
            startCharUuid -> {
                deviceStarted = value[0].toInt() > 0
            }
            speedLevelCharUuid -> {
                currentRPM = value[0].toUByte()
            }
            seedMissUuid -> {
                var valInt = value[0].toInt()
                when(valInt)
                {
                    1 -> { singleMisses++ }
                    2 -> { doubleMisses++ }
                    else -> { totalMisses += valInt }
                }
            }
        }
    }

}


// Utility functions ---------------------------------------------------------------------
private fun Boolean.toByte(): Byte {
    return (if (this) 1 else 0).toByte()
}
