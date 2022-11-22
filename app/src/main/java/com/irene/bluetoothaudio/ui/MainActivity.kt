package com.irene.bluetoothaudio.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
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
import java.io.IOException
import javax.inject.Inject

const val REQUEST_CODE = 123
const val ENABLE_BLUETOOTH_REQUEST_CODE = 1

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bluetoothRepository: BluetoothRepository

    private var fileName: String = ""

    private var recorder: MediaRecorder? = null

    private var player: MediaPlayer? = null

    val audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @RequiresApi(Build.VERSION_CODES.M)
    fun audioOutputAvailable(type: Int): Boolean {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
            return false
        }
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type == type }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    val audioCallback =  object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            super.onAudioDevicesAdded(addedDevices)
            if (audioOutputAvailable(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)) {
                // a bluetooth headset has just been connected
            }
        }
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            super.onAudioDevicesRemoved(removedDevices)
            if (!audioOutputAvailable(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)) {
                // a bluetooth headset is no longer connected
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isPermissionsGranted())
            requestPermissions(REQUEST_CODE)

        fileName = "${externalCacheDir!!.absolutePath}/audiorecordtest.3gp"
        audioManager.registerAudioDeviceCallback(audioCallback, null)

        setContent {

            val scope = rememberCoroutineScope()
            StartScan()

            var startPlaying by remember{mutableStateOf(true)}
            var startRecording by remember{mutableStateOf(true)}

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
                    Column() {
                        Greeting("Android")
                        Button(
                            onClick = {
                                onPlay(startPlaying)
                                startPlaying = !startPlaying
                            }
                        ) {
                            Text(text = if (startPlaying) "Start playing" else "Stop playing")
                        }
                        Button(
                            onClick = {
                                onRecord(startRecording)
                                startRecording = !startRecording
                            }
                        ) {
                            Text(text = if (startRecording) "Start recording" else "Stop recording")
                        }
                    }

                }
            }
        }
    }

    private fun onRecord(start: Boolean) = if (start) {
        startRecording()
    } else {
        stopRecording()
    }

    private fun onPlay(start: Boolean) = if (start) {
        startPlaying()
    } else {
        stopPlaying()
    }

    private fun startPlaying() {
        log("startPlaying")
        player = MediaPlayer().apply {
            try {
                setDataSource(fileName)
                prepare()
                start()
            } catch (e: IOException) {
                log("prepare() failed")
            }
        }
    }

    private fun stopPlaying() {
        log("stopPlaying")
        player?.release()
        player = null
    }

    private fun startRecording() {
        log("startRecording")
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                log("prepare() failed")
            }

            start()
        }
    }

    private fun stopRecording() {
        log("stopRecording")
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
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