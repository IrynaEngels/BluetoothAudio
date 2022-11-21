package com.irene.bluetoothaudio.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import com.irene.bluetoothaudio.bluetooth_repository.BluetoothRepository
import com.irene.bluetoothaudio.bluetooth_service.BLUETOOTH_NOT_ENABLED
import com.irene.bluetoothaudio.ui.theme.BluetoothAudioTheme
import com.irene.bluetoothaudio.utils.OnLifecycleEvent
import com.irene.bluetoothaudio.utils.isPermissionsGranted
import com.irene.bluetoothaudio.utils.log
import com.irene.bluetoothaudio.utils.requestPermissions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

const val REQUEST_CODE = 123
const val ENABLE_BLUETOOTH_REQUEST_CODE = 1

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bluetoothRepository: BluetoothRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isPermissionsGranted())
            requestPermissions(REQUEST_CODE)

        setContent {

            val scope = rememberCoroutineScope()
            StartScan()

            val errors = bluetoothRepository.errorsStateFlow.collectAsState()
            if (errors.value == BLUETOOTH_NOT_ENABLED){
                promptEnableBluetooth()
            }

            OnLifecycleEvent { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME,
                    Lifecycle.Event.ON_START,
                    -> {
                        scope.launch {
                            bluetoothRepository.scanningDevices()
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
                    Greeting("Android")
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