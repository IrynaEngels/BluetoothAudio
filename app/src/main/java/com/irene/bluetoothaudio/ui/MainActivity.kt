package com.irene.bluetoothaudio.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.media.*
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.irene.bluetoothaudio.bluetooth_repository.BluetoothRepository
import com.irene.bluetoothaudio.ui.theme.BluetoothAudioTheme
import com.irene.bluetoothaudio.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

const val REQUEST_CODE = 123
const val ENABLE_BLUETOOTH_REQUEST_CODE = 1

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bluetoothRepository: BluetoothRepository

    private var recorder: MediaRecorder? = null

    private var player: MediaPlayer? = null

//name: MAJOR III BLUETOOTH address: 2C:4D:79:DA:0E:0A type: 1

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isPermissionsGranted())
            requestPermissions(REQUEST_CODE)

        setContent {

            val scope = rememberCoroutineScope()
//            StartScan()

            /*var startPlaying by remember{mutableStateOf(true)}
            var startRecording by remember{mutableStateOf(true)}*/

            val scannedDevices = bluetoothRepository.scannedDeviceStateFlow.collectAsState()

            OnLifecycleEvent { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME,
                    Lifecycle.Event.ON_START,
                    -> {
//                        promptEnableBluetooth()
                        scope.launch {
//                            bluetoothRepository.scanningDevices()
                        }
                    }
                    Lifecycle.Event.ON_DESTROY -> {}
                    else -> Unit
                }
            }

            BluetoothAudioTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column() {
                        Greeting("Android")
                        Button(
                            onClick = {
                                if (bluetoothRepository.isBluetoothOn)
                                    scope.launch {
                                        bluetoothRepository.scanningDevices()
                                    }
                                else promptEnableBluetooth()
                            }
                        ) {
                            Text(text = "Start device scan")
                        }
                        Button(
                            onClick = {
                                val audioRecorder = AudioRecorder(this@MainActivity)
                                audioRecorder.turnOnBluetooth()
                                audioRecorder.createAudioManager(scope)
                            }
                        ) {
                            Text(text = "Record and play")
                        }
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            for (device in scannedDevices.value) {
                                ScannedDeviceItem(device.name, device.macAddress) { macAddress ->
                                    bluetoothRepository.connectToDevice(macAddress)
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    @Composable
    fun StartScan() {
        LaunchedEffect(key1 = Unit, block = {
            bluetoothRepository.scanningDevices()
        })
    }

    @SuppressLint("MissingPermission")
    private fun promptEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
    }
}


@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BluetoothAudioTheme {
        Greeting("Android")
    }
}