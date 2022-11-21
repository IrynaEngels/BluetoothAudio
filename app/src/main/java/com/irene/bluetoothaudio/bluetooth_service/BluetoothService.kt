package com.irene.bluetoothaudio.bluetooth_service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.irene.bluetoothaudio.utils.log
import com.irene.bluetoothaudio.utils.safeLet
import java.io.IOException
import java.nio.CharBuffer
import java.util.*


// Constants that indicate the current connection state
enum class State {
    STATE_NONE, // we're doing nothing
    STATE_LISTEN,  // now listening for incoming connections
    STATE_CONNECTING, // now initiating an outgoing connection
    STATE_CONNECTED, // now connected to a remote device
    STATE_DISCONNECTING
}

const val BLUETOOTH_NOT_ENABLED = "BLUETOOTH not enabled."

class BluetoothService constructor(private val context: Context) {

    var callback: IBluetoothSDKListener? = null
    private var bluetoothAdapter: BluetoothAdapter?
    private var connectingDevices: HashMap<String, ConnectingThread>
    private var connectedDevices: HashMap<String, ConnectedThread>
    //private var connectedDevice: BluetoothDevice? = null
    // UUID for SPP
    private val SSP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var connectionState: State = State.STATE_NONE
    //private var newState: State = State.STATE_NONE

    // Bluetooth connections
    private var connectingThread: ConnectingThread? = null
    //private var connectedThread: ConnectedThread? = null
    //private var mAcceptThread: AcceptThread? = null

    private val PAIRING_VARIANT_PIN = 0
    private val PAIRING_VARIANT_PASSKEY = 1
    private val PAIRING_VARIANT_PASSKEY_CONFIRMATION = 2
    private val PAIRING_VARIANT_CONSENT = 3
    private val PAIRING_VARIANT_DISPLAY_PASSKEY = 4
    private val PAIRING_VARIANT_DISPLAY_PIN = 5
    private val PAIRING_VARIANT_OOB_CONSENT = 6


    init {
        val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        connectingDevices = HashMap<String, ConnectingThread>()
        connectedDevices = HashMap<String, ConnectedThread>()
        connectionState = State.STATE_NONE
        //newState = connectionState

        val scanCallback: ScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device: BluetoothDevice = result.getDevice()
                log("found device $device")
                // ...do whatever you want with this found device
            }

            override fun onBatchScanResults(results: List<ScanResult?>?) {
                // Ignore for now
            }

            override fun onScanFailed(errorCode: Int) {
                // Ignore for now
            }
        }


        log("init block")
    }

    /**
     * Broadcast Receiver for catching ACTION_FOUND aka new device discovered
     */
    private val discoveryBroadcastReceiver = object : BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            log("onReceive")

            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    log("device found  ${intent.extras.toString()}")
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    safeLet(device, callback) { _device, _callback ->
                        log("name: ${_device.name} address: ${_device.address} type: ${_device.type}")
                        _callback.onDeviceDiscovered(_device)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    log("ACTION_DISCOVERY_FINISHED")
                }
            }
        }
    }

    private val connectionStateBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            log("onReceive")

            when(intent.action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    log("ACTION_ACL_DISCONNECTED")
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        disconnect(it)
                    }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    log("ACTION_ACL_CONNECTED")
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        callback?.onDeviceConnected(device)
                    }
                }
            }
        }
    }

    private val pairingRequestBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            log("pairing request received: ")
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                ?: return

            // Skip other devices
            /*if (!device.address.equals(getAddress(), ignoreCase = true)) return*/
            if (connectingDevices[device.address] == null) return

            val variant =
                intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR)
            log("pairing request received: " + pairingVariantToString(variant) + " (" + variant + ")")
            if (variant == PAIRING_VARIANT_PIN) {
                when(device.address) {
                    "00:22:02:01:1E:BB" -> {
                        val pin: String = "1234"
                        log("setting PIN code for ${device.address} using $pin")
                        device.setPin(pin.toByteArray())
                        abortBroadcast()
                    }
                    "98:D3:21:F7:D2:7E" -> {
                        val pin: String = "6667"
                        log("setting PIN code for ${device.address} using $pin")
                        device.setPin(pin.toByteArray())
                        abortBroadcast()
                    }
                    "98:D3:31:F9:4F:6E" -> {
                        val pin: String = "6766"
                        log("setting PIN code for ${device.address} using $pin")
                        device.setPin(pin.toByteArray())
                        abortBroadcast()
                    }
                }
            }
        }
    }



    private fun pairingVariantToString(variant: Int): String? {
        return when (variant) {
            PAIRING_VARIANT_PIN -> "PAIRING_VARIANT_PIN"
            PAIRING_VARIANT_PASSKEY -> "PAIRING_VARIANT_PASSKEY"
            PAIRING_VARIANT_PASSKEY_CONFIRMATION -> "PAIRING_VARIANT_PASSKEY_CONFIRMATION"
            PAIRING_VARIANT_CONSENT -> "PAIRING_VARIANT_CONSENT"
            PAIRING_VARIANT_DISPLAY_PASSKEY -> "PAIRING_VARIANT_DISPLAY_PASSKEY"
            PAIRING_VARIANT_DISPLAY_PIN -> "PAIRING_VARIANT_DISPLAY_PIN"
            PAIRING_VARIANT_OOB_CONSENT -> "PAIRING_VARIANT_OOB_CONSENT"
            else -> "UNKNOWN"
        }
    }

    private fun registerBondingBroadcastReceivers() {
        /*context.registerReceiver(
            bondStateReceiver,
            IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        )*/
        context.registerReceiver(
            pairingRequestBroadcastReceiver,
            IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST)
        )
    }

    private fun unregisterBondingBroadcastReceivers() {
        try {
            context.unregisterReceiver(pairingRequestBroadcastReceiver)
        } catch (e: Exception) {
            // already unregistered
        }
    }

    private fun registerConnectionBroadcastReceivers() {

        context.registerReceiver(
            connectionStateBroadcastReceiver,
            IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        )
        context.registerReceiver(
            connectionStateBroadcastReceiver,
            IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)
        )
    }

    private fun unregisterConnectionBroadcastReceivers() {
        try {
            context.unregisterReceiver(connectionStateBroadcastReceiver)
        } catch (e: Exception) {
            // already unregistered
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        log("startDiscovery")
        // Register for broadcasts when discovery has finished
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        context.registerReceiver(discoveryBroadcastReceiver, filter)
        if (bluetoothAdapter?.isEnabled == true) {
            bluetoothAdapter?.startDiscovery()
            callback?.onDiscoveryStarted()
        } else {
            callback?.onError(message = BLUETOOTH_NOT_ENABLED)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        log("stopDiscovery")
        try {
            context.unregisterReceiver(discoveryBroadcastReceiver)
        } catch (e: Exception) {
            // already unregistered
        }
        bluetoothAdapter?.cancelDiscovery()
        callback?.onDiscoveryStopped()
    }

    /**
     * Return the current connection state.
     */
    @Synchronized
    fun getState(): State {
        return connectionState
    }

    @Synchronized
    fun connect(found: BluetoothDevice) {
        log("connect to: $found")
        registerConnectionBroadcastReceivers()
        // Cancel any thread attempting to make a connection
        if (connectionState != State.STATE_CONNECTING
            && connectionState != State.STATE_DISCONNECTING) {
            synchronized(this@BluetoothService) {
                connectionState = State.STATE_CONNECTING
            }
            registerBondingBroadcastReceivers()
            // Start the thread to connect with the given device
            val device = bluetoothAdapter?.getRemoteDevice(found.address)
            device?.let {
                connectingThread = ConnectingThread(it)
                connectingThread!!.start()
                connectingDevices[it.address] = connectingThread!!
            }
        } else {
            callback?.onError(macAddress = found.address, message = "Bluetooth service busy.")
        }
    }

    @Synchronized
    private fun startConnectedThread(
        device: BluetoothDevice,
        bluetoothSocket: BluetoothSocket,
    ) {

        val macAddress = device.address
        connectingDevices.remove(macAddress)

        val connectedThread = ConnectedThread(device = device, mmSocket = bluetoothSocket)
        connectedThread.start()
        connectedDevices[macAddress] = connectedThread
        callback?.onDeviceConnected(device)
        unregisterBondingBroadcastReceivers()
    }


    private fun connectionFailed(macAddress: String) {
        connectingDevices[macAddress]?.cancel()
        connectingDevices.remove(macAddress)
        synchronized(this@BluetoothService) {
            connectionState = State.STATE_NONE
        }
        callback?.onError(macAddress = macAddress, message = "Device ${macAddress}  connection fail.")
    }

    /**
     * Write to the ConnectedThread in an asynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread.write
     */
    fun write(macAddress: String, out: String) {
        // Create temporary object
        var r: ConnectedThread?
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            r = connectedDevices[macAddress]
            if (r == null) throw IllegalStateException("$macAddress  device not connected")
        }
        // Perform the write asynchronized
        r?.write(out)
    }

    @Synchronized
    fun closeConnections() {
        synchronized(this@BluetoothService) {
            connectionState = State.STATE_DISCONNECTING
        }
        connectingDevices.forEach {
            it.value?.cancel()
        }
        connectedDevices.forEach {
            it.value?.cancel()
            callback?.onDeviceDisconnected(it.value.device)
        }
        connectingDevices.clear()
        connectedDevices.clear()
        unregisterConnectionBroadcastReceivers()
    }

    fun disconnect(macAddress: String) {
        val device = bluetoothAdapter?.getRemoteDevice(macAddress)
        device?.let {
            disconnect(it)
        }
    }

    @Synchronized
    private fun disconnect(device: BluetoothDevice) {
        synchronized(this@BluetoothService) {
            connectionState = State.STATE_DISCONNECTING
        }
        connectingDevices[device.address]?.cancel()
        connectingDevices.remove(device.address)

        connectedDevices[device.address]?.cancel()
        connectedDevices.remove(device.address)

        callback?.onDeviceDisconnected(device)

        synchronized(this@BluetoothService) {
            connectionState = State.STATE_NONE
        }
    }

    private inner class AcceptThread : Thread() {
        // Body
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    @SuppressLint("MissingPermission")
    private inner class ConnectingThread(private val device: BluetoothDevice) : Thread() {

        private var mmSocket: BluetoothSocket? = null
        private var mThread = this

        init {
            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                mmSocket = device.createInsecureRfcommSocketToServiceRecord(SSP_UUID)
                synchronized(this@BluetoothService) {
                    connectionState =
                        com.irene.bluetoothaudio.bluetooth_service.State.STATE_CONNECTING
                }
            } catch (e: Exception) {
                log("Socket create() failed $e")
                connectionFailed(device.address)
            }

        }

        override fun run() {
            log( "BEGIN mConnectingThread ")
            name = "ConnectThread_${device.address}"

            // Make a connection to the BluetoothSocket
            try {
                // Always cancel discovery because it will slow down a connection
                bluetoothAdapter?.cancelDiscovery()
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket!!.connect()
            } catch (e: Exception) {
                log(e.toString())
                // Close the socket
                try {
                    mmSocket?.close()
                } catch (e2: IOException) {
                    log(
                         "unable to close() " +
                                " socket during connection failure $e2"
                    )
                }
                connectionFailed(device.address)
                return
            }

            // Start the connected thread
            mmSocket?.let {
                startConnectedThread(device = device, bluetoothSocket = it)
            }
        }

        // Call this method to shut down the connection.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: Exception) {
                callback?.onError( macAddress = device.address, message = "Could not close the connect socket")
            }
        }
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     */
    inner class ConnectedThread(
        val device: BluetoothDevice,
        private val mmSocket: BluetoothSocket
        ) : Thread() {

        private val mmInReader = mmSocket.inputStream.bufferedReader()
        private val mmOutWriter = mmSocket.outputStream.bufferedWriter()
        //private val mmBuffer: ByteArray = ByteArray(1024)
        private val mmBuffer = CharBuffer.allocate(1024)

        override fun run() {
            var num: Int // chars returned from read()
            synchronized(this@BluetoothService) {
                connectionState = com.irene.bluetoothaudio.bluetooth_service.State.STATE_CONNECTED
            }
            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                num = try {
                    mmInReader.read(mmBuffer)
                } catch (e: IOException) {
                    log("${device.address} InputStream closed. ${e.toString()}")
                    disconnect(device)
                    break
                }

                /*val message = String(mmBuffer, 0, numBytes)*/
                val message = mmBuffer.toString()
                callback?.onMessageReceived(mmSocket.remoteDevice, message)
            }
        }

        // Call this to send data to the remote device.
        fun write(message: String) {
            try {
                mmOutWriter.write(message)
                mmOutWriter.flush()

                callback?.onMessageSent(mmSocket.remoteDevice)
            } catch (e: IOException) {
                callback?.onError(macAddress = device.address, message = "Error occurred when sending data")
                return
            }
        }

        // Call this method to shut down the connection.
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                callback?.onError( macAddress = device.address, message = "Could not close the connect socket")
            }
        }
    }

}

interface IBluetoothSDKListener {
    /**
     * from action ACTION_DISCOVERY_STARTED
     */
    fun onDiscoveryStarted()

    /**
     * from action ACTION_DISCOVERY_STOPPED
     */
    fun onDiscoveryStopped()

    /**
     * from action ACTION_DEVICE_FOUND
     */
    fun onDeviceDiscovered(device: BluetoothDevice)

    /**
     * from action ACTION_DEVICE_CONNECTED
     */
    fun onDeviceConnected(device: BluetoothDevice)

    /**
     * from action BluetoothUtils.ACTION_MESSAGE_RECEIVED
     */
    fun onMessageReceived(device: BluetoothDevice?, message: String?)

    /**
     * from action BluetoothUtils.ACTION_MESSAGE_SENT
     */
    fun onMessageSent(device: BluetoothDevice?)

    /**
     * from action ACTION_CONNECTION_ERROR
     */
    fun onError(macAddress: String? = null, message: String?)

    /**
     * from action ACTION_DEVICE_DISCONNECTED
     */
    fun onDeviceDisconnected(device: BluetoothDevice)
}