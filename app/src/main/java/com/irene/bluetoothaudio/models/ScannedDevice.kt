package com.irene.bluetoothaudio.models

import android.bluetooth.BluetoothDevice

data class ScannedDevice(
    val name: String = "",
    val macAddress: String,
    val deviceClass: String = "",
    val rssi: String = "",
    val device: BluetoothDevice
)