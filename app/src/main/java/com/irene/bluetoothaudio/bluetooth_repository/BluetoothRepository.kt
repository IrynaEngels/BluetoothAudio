package com.irene.bluetoothaudio.bluetooth_repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.irene.bluetoothaudio.bluetooth_service.BluetoothService
import com.irene.bluetoothaudio.bluetooth_service.IBluetoothSDKListener
import com.irene.bluetoothaudio.models.Device
import com.irene.bluetoothaudio.models.DeviceError
import com.irene.bluetoothaudio.models.ScannedDevice
import com.irene.bluetoothaudio.utils.log
import com.irene.bluetoothaudio.utils.safeOffer
import com.irene.bluetoothaudio.utils.safeOfferCatching
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class BluetoothRepository @Inject constructor(
    private val context: Context,
    private val bluetoothService: BluetoothService,
)  {

    private val list = mutableListOf<ScannedDevice>()
    private var scannedDeviceList: Channel<List<ScannedDevice>> = Channel()
    private var connectedDevices: ConflatedBroadcastChannel<Set<Device>> =
        ConflatedBroadcastChannel(mutableSetOf())
    private var _error = Channel<DeviceError>(Channel.UNLIMITED)
    private var messageSent = Channel<String>(Channel.UNLIMITED)


    private val _errors: MutableStateFlow<String> =
        MutableStateFlow("")

    val errorsStateFlow: StateFlow<String> = _errors.asStateFlow()

    private val bluetoothSDKListener = object : IBluetoothSDKListener {
        override fun onDiscoveryStarted() {
            log("onDiscoveryStarted")
            list.clear()
        }

        override fun onDiscoveryStopped() {
            log("onDiscoveryStopped")
            list.clear()
        }

        @SuppressLint("MissingPermission")
        override fun onDeviceDiscovered(device: BluetoothDevice) {
            log("onDeviceDiscovered")
            list.add(
                ScannedDevice(
                    name = device.name ?: device.address,
                    macAddress = device.address,
                    deviceClass = device.bluetoothClass.toString(),
                    device = device
                )
            )
            scannedDeviceList.safeOffer(list)
        }

        @SuppressLint("MissingPermission")
        override fun onDeviceConnected(device: BluetoothDevice) {
            log("onDeviceConnected ${device.address}")
            connectedDevices.let { stateConnect ->
                if (!stateConnect.isClosedForSend) {
                    val list = stateConnect.value as MutableSet
                    list.add(
                        Device(
                            name = device.name,
                            macAddress = device.address
                        )
                    )
                    stateConnect.safeOfferCatching(list)
                }
            }
        }

        override fun onMessageReceived(device: BluetoothDevice?, message: String?) {
            log("onMessageReceived")
        }

        override fun onMessageSent(device: BluetoothDevice?) {
            log("onMessageSent")
            messageSent.let { sendChannel ->
                if (!sendChannel.isClosedForSend) {
                    device?.let {
                        sendChannel.safeOffer(device.address)
                    }
                }
            }
        }

        override fun onError(macAddress: String?, message: String?) {
            log("onError: $message")
            message?.let{
                _errors.value = it
            }
            _error.let { errorChannel ->
                if (!errorChannel.isClosedForSend) {
                    errorChannel.safeOffer(DeviceError(macAddress, message))
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onDeviceDisconnected(device: BluetoothDevice) {
            log("onDeviceDisconnected ${device.address}")
            connectedDevices.let { stateConnect ->
                if (!stateConnect.isClosedForSend) {
                    val list = stateConnect.value as MutableSet
                    list.remove(Device(
                        name = device.name,
                        macAddress = device.address
                    ))
                    stateConnect.safeOfferCatching(list)
                }
            }
        }
    }

    suspend fun scanningDevices(): Flow<List<ScannedDevice>> {
        scannedDeviceList = Channel()
        bluetoothService.callback = bluetoothSDKListener

        return suspendCancellableCoroutine { continuation ->
            bluetoothService.startDiscovery()

            continuation.resume(flow {
                scannedDeviceList.consumeAsFlow().collect { founded ->
                    log("Result scan -> $founded")
                    emit(founded)
                }
            })
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        bluetoothService.connect(device)
    }

    fun stopScanning() {
        bluetoothService.stopDiscovery()
    }

    fun subscribeToConnectedDevices(): Flow<List<Device>> {
        if (connectedDevices.isClosedForSend) {
            connectedDevices = ConflatedBroadcastChannel()
        }
        return flow {
            connectedDevices.asFlow().collect { devices ->
                emit(devices.toList())
            }
        }
    }

    fun disconnectAllDevices() {
        bluetoothService.closeConnections()
    }

    fun disconnectDevice(macAddress: String) {
        bluetoothService.disconnect(macAddress)
    }

    suspend fun handleErrorState(): Flow<DeviceError> {
        /*_error = Channel()*/
        _error.invokeOnClose {
            log("_error channel is closed: $it")
            _error = Channel()
        }
        return suspendCancellableCoroutine { continuation ->
            continuation.resume(flow {
                _error.consumeAsFlow().collect { error ->
                    emit(error)
                }
            })
        }
    }

    suspend fun sendCommand(macAddress: String, command: String, onCommandSent: () -> Unit) {
        log("$macAddress message: $command")
        bluetoothService.write(macAddress = macAddress, out = command)

        messageSent.consumeAsFlow().collect { _macAddress ->
            if (_macAddress == macAddress) onCommandSent.invoke()
        }
    }
}