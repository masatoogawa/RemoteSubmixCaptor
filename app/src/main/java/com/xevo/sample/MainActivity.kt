package com.xevo.sample

import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.xevo.remotesubmixcaptor.RemoteSubmixCaptor

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File
import java.io.FileOutputStream

@RuntimePermissions
class MainActivity : AppCompatActivity() {

    private val fileName = "test.pcm"
    private var file: FileOutputStream? = null
    private var remoteSubmixCaptor: RemoteSubmixCaptor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        remoteSubmixCaptor = RemoteSubmixCaptor()

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
        remoteSubmixCaptor?.callback = object : RemoteSubmixCaptor.Callback {
            override fun onReceive(boxedByteArray: RemoteSubmixCaptor.BoxedByteArray) {
                file?.write(boxedByteArray.bytearray, 0, boxedByteArray.bytearray.size)
            }
        }
        remoteSubmixCaptor?.startRecording()
    }

    fun stopRecording() {
        remoteSubmixCaptor?.stopRecording()
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
