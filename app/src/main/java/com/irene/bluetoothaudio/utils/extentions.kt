package com.irene.bluetoothaudio.utils

import android.bluetooth.BluetoothDevice
import com.irene.bluetoothaudio.models.ScannedDevice

fun List<ScannedDevice>.containsDevice(device: ScannedDevice): Boolean{
    for (d in this){
        if (d.macAddress == device.macAddress)
            return true
    }
    return false
}

fun String.getBluetoothDeviceByMacAddress(devices: List<BluetoothDevice>): BluetoothDevice?{
    for (d in devices){
        if (d.address == this)
            return d
    }
    return null
}