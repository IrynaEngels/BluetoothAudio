package com.irene.bluetoothaudio.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.media.*
import android.media.AudioRecord.OnRecordPositionUpdateListener
import android.os.Build
import android.os.Process
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class AudioRecorder(private val context: Context) {
    private var LOG_TAG: String? = null


    private var recorder: AudioRecord? = null
    private var player: AudioTrack? = null
    private var audioManager: AudioManager? = null
    private var recorderBufSize: Int
    private var recordingSampleRate: Int
    private val trackBufSize: Int
    private var audioData: ShortArray? = shortArrayOf()
    private var isRecording = false
    private var isPlaying = false
    private var startRecThread: Thread? = null
    private val posUpdateListener: OnRecordPositionUpdateListener

    /**
     * constructor method for initializing the variables
     */
    init {
        LOG_TAG = "Constructor"
        trackBufSize = 0
        recordingSampleRate = trackBufSize
        recorderBufSize = recordingSampleRate

        // init function will initialize all the necessary variables ...
        init()
        if (recorder != null && player != null) {
            log("recorder and player initialized")
            audioData = ShortArray(recorderBufSize / 2) // since we r reading shorts
        } else {
            log("Problem inside init function ")
        }
        posUpdateListener = object : OnRecordPositionUpdateListener {
            var numShortsRead = 0
            override fun onPeriodicNotification(rec: AudioRecord) {
                audioData =
                    ShortArray(recorderBufSize / 2) // divide by 2 since now we are reading shorts
                numShortsRead = rec.read(audioData!!, 0, audioData!!.size)
                player!!.write(audioData!!, 0, numShortsRead)
            }

            override fun onMarkerReached(recorder: AudioRecord) {
                // TODO Auto-generated method stub
                log("Marker Reached")
            }
        }
        // listener will be called every time 160 frames are reached
        recorder!!.positionNotificationPeriod = 160
        recorder!!.setRecordPositionUpdateListener(posUpdateListener)
        log("inside constructor")
    }

    @SuppressLint("MissingPermission")
    private fun init() {
        LOG_TAG = "initFunc"
        // int[] mSampleRates = new int[] { 8000, 11025, 22050, 44100 };
        val audioFormat: Short = AudioFormat.ENCODING_PCM_16BIT.toShort()
        // for (int rate : mSampleRates) {
        recordingSampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
        try {
            // Log.d(LOG_TAG, "Attempting rate " + rate + "Hz, bits: " +
            // audioFormat);
            val bufrSize = AudioRecord.getMinBufferSize(
                recordingSampleRate,
                AudioFormat.CHANNEL_IN_MONO, audioFormat.toInt()
            )

            // lets find out the minimum required size for AudioTrack
            val audioTrackBufSize = AudioTrack.getMinBufferSize(
                recordingSampleRate,
                AudioFormat.CHANNEL_OUT_MONO, audioFormat.toInt()
            )
            if (bufrSize != AudioRecord.ERROR_BAD_VALUE
                && bufrSize != AudioRecord.ERROR
            ) {
                // check if we can instantiate and have a success
                if (audioTrackBufSize >= bufrSize) {
                    recorderBufSize = audioTrackBufSize
                } else {
                    recorderBufSize = bufrSize
                }
                val rec = AudioRecord(
                    MediaRecorder.AudioSource.MIC, recordingSampleRate,
                    AudioFormat.CHANNEL_IN_MONO, audioFormat.toInt(), recorderBufSize
                )
                if (rec != null
                    && rec.state == AudioRecord.STATE_INITIALIZED
                ) {

                    Log.e(
                        LOG_TAG,
                        "Returning..(rate:channelConfig:audioFormat:recorderBufSize)"
                                + recordingSampleRate + ":" + AudioFormat.CHANNEL_IN_MONO
                                + ":" + audioFormat + ":" + recorderBufSize
                    )

                    // Now create an instance of the AudioTrack
//                  int audioTrackBufSize = AudioTrack.getMinBufferSize(rate,
//                          AudioFormat.CHANNEL_OUT_MONO, audioFormat);
                    Log.e(
                        LOG_TAG,
                        "Audio Record / Track / Final buf size :" + bufrSize + "/ " + audioTrackBufSize + "/ " + recorderBufSize
                    )

                    player = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        AudioTrack(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build(),
                            AudioFormat.Builder()
                                .setSampleRate(recordingSampleRate)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                            recorderBufSize,
                            AudioTrack.MODE_STREAM,
                            AudioManager.AUDIO_SESSION_ID_GENERATE)
                    } else {
                        //support for Android KitKat
                        AudioTrack(AudioManager.STREAM_MUSIC,
                            recordingSampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            recorderBufSize,
                            AudioTrack.MODE_STREAM)
                    }
                    recorder = rec
                    player!!.stop()
                    player!!.flush()
                    player!!.playbackRate = recordingSampleRate
                    return
                }
            }
        } catch (e: IllegalArgumentException) {
            Log.d(LOG_TAG, recordingSampleRate.toString() + "Exception, keep trying.", e)
        } catch (e: Exception) {
            Log.e(LOG_TAG, recordingSampleRate.toString() + "Some Exception!!", e)
        }
        // for loop for channel config ended here. . . .
        // for loop for audioFormat ended here. . .
        // }// for loop for sampleRate
        return
    }

    private fun startPlaying() {
        LOG_TAG = "startPlaying"
        Log.e(LOG_TAG, "start Playing")
    }

    private fun stopPlaying() {
        LOG_TAG = "stopPlaying"
        Log.e(LOG_TAG, "stop Playing")
    }

    private fun startRecording(coroutineScope: CoroutineScope) {
        LOG_TAG = "startRecording"

        coroutineScope.launch {

                // TODO Auto-generated method stub
                Process
                    .setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                //              String LOG_TAG = Thread.currentThread().getName();
                if (recorder!!.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    recorder!!.startRecording()
                }
                //              Log.e(LOG_TAG, "running" +recorder.getRecordingState());
                while (recorder!!.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    recorder!!.read(audioData!!, 0, audioData!!.size)
                    try {
                        delay(1000) // sleep for 2s
                    } catch (e: InterruptedException) {
                        // TODO Auto-generated catch block
                        Log.e("run Method", "recorder thread is interrupted")
                        e.printStackTrace()
                    }
                }
            }

        audioManager!!.isSpeakerphoneOn = true
        player!!.flush()
        player!!.play()
        Log.e(LOG_TAG, "start Recording")
    }

    private fun stopRecording() {
        LOG_TAG = "stopRecording"
        recorder!!.stop()
        if (startRecThread != null && startRecThread!!.isAlive) {
            startRecThread!!.destroy()
            startRecThread = null
        }
        player!!.stop()
        player!!.flush()
        Log.e(LOG_TAG, "stop Recording")
    }

    private fun stop() {
        if (isRecording) {
            isRecording = false
            stopRecording()
        }
        if (isPlaying) {
            isPlaying = false
            stopPlaying()
        }
    }

    fun createAudioManager(coroutineScope: CoroutineScope) {
        audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        startRecording(coroutineScope)

    }

    val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
            if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {
                log("bluetooth connected")
                unregisterReceiver()
            } else if (AudioManager.SCO_AUDIO_STATE_DISCONNECTED == state) {
                log("bluetooth disconnected")
            }
        }
    }

    fun registerReceiver(){
        context.registerReceiver(
            broadcastReceiver,
            IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        )
    }

    fun unregisterReceiver(){
        try {
            context.unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            // already unregistered
        }
    }

    fun turnOnBluetooth() {
        registerReceiver()
        try {
            audioManager?.let {
                val mAudioManager = audioManager as AudioManager
                if (mAudioManager.isBluetoothScoAvailableOffCall) {
                    if (mAudioManager.isBluetoothScoOn()) {
                        mAudioManager.stopBluetoothSco()
                        mAudioManager.startBluetoothSco()
                        System.err.println("Bluetooth SCO On!")
                    } else {
                        System.err.println("Bluetooth Sco Off!")
                        mAudioManager.startBluetoothSco()
                    }
                } else {
                    System.err.println("Bluetooth SCO not available")
                }
            }

        } catch (e: java.lang.Exception) {
            System.err.println("sco elsepart startBluetoothSCO $e")
            unregisterReceiver()
        }
    }

    fun onDestroy() {
        if (recorder != null) recorder!!.release()
        if (startRecThread != null && startRecThread!!.isAlive) startRecThread!!.destroy()
        if (recorder != null) recorder!!.release()
        if (player != null) player!!.release()
        startRecThread = null
        recorder = null
        player = null
        audioData = null
        System.gc()
    }
}