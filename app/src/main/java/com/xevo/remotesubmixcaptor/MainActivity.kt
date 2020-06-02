package com.xevo.remotesubmixcaptor

import android.media.*
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

@RuntimePermissions
class MainActivity : AppCompatActivity() {

    private val samplingRate = 44100
    private val fps = 10
    private val samplingNumPerFrame = samplingRate / fps
    private val frameSizeInBytes = samplingNumPerFrame * 2 * 2
    private var audioRecord: AudioRecord? = null
    private val fileName = "test.pcm"
    private val audioBufferSizeInBytes = max(samplingNumPerFrame * 10,
        AudioRecord.getMinBufferSize(samplingRate, AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT))
    private var file: FileOutputStream? = null
    private var audioTrack: AudioTrack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)


        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        recordButton.setOnClickListener {
            startRecordingWithPermissionCheck()
        }

        stopButton.setOnClickListener {
            stopRecording()
        }
    }

    @NeedsPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAPTURE_AUDIO_OUTPUT)
    fun startRecording() {

        val dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        file = File(dir, fileName).outputStream()

        val bufferSize = AudioTrack.getMinBufferSize(samplingRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    //.setContentType(AudioAttributes.CON)
                    .build()
            )
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(samplingRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()


        val audioDataArray = ByteArray(frameSizeInBytes)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.REMOTE_SUBMIX,
            samplingRate,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            audioBufferSizeInBytes).apply {

            positionNotificationPeriod = samplingNumPerFrame
            setRecordPositionUpdateListener(object: AudioRecord.OnRecordPositionUpdateListener {
                override fun onPeriodicNotification(recorder: AudioRecord?) {

                    val ret = recorder?.read(audioDataArray, 0, frameSizeInBytes)
                    ret?.let {
                        file?.write(audioDataArray, 0, it)
                        audioTrack?.write(audioDataArray, 0 ,audioDataArray.size)
                    }
                    Log.d("mogawa", "read=$ret")
                }

                override fun onMarkerReached(recorder: AudioRecord?) {
                }
            })
            startRecording()
        }

        audioTrack?.play()
    }

    fun stopRecording() {
        audioRecord?.stop()
        audioRecord = null
        audioTrack?.stop()
        audioTrack = null
        file?.close()
        file = null
    }

    override fun onRequestPermissionsResult (
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
