package com.xevo.remotesubmixcaptor

import android.media.*
import kotlin.math.max

class RemoteSubmixCaptor {

    class BoxedByteArray(ba: ByteArray) {
        var bytearray: ByteArray = ba
    }
    interface Callback {
        fun onReceive(boxedByteArray: BoxedByteArray)
    }

    var callback: Callback? = null
    private val samplingRate = 44100
    private val fps = 105
    private val samplingNumPerFrame = samplingRate / fps
    private val frameSizeInBytes = samplingNumPerFrame * 2 * 2
    private var audioRecord: AudioRecord? = null
    private val audioBufferSizeInBytes = max(samplingNumPerFrame * 10,
        AudioRecord.getMinBufferSize(samplingRate, AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT))
    private var audioTrack: AudioTrack? = null

    fun startRecording() {
        val bufferSize = AudioTrack.getMinBufferSize(samplingRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(samplingRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()
            )
            .setBufferSizeInBytes(bufferSize*20)
            .build()

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.REMOTE_SUBMIX,
            samplingRate,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            audioBufferSizeInBytes).apply {

            positionNotificationPeriod = samplingNumPerFrame

            setRecordPositionUpdateListener(object: AudioRecord.OnRecordPositionUpdateListener {

                override fun onPeriodicNotification(recorder: AudioRecord?) {
                    val audioDataArray = ByteArray(frameSizeInBytes)
                    val ret = recorder?.read(audioDataArray, 0, frameSizeInBytes)
                    ret?.let {

                        audioTrack?.write(
                            audioDataArray,
                            0,
                            audioDataArray.size,
                            AudioTrack.WRITE_NON_BLOCKING)
                        callback?.onReceive(BoxedByteArray(audioDataArray))
                    }
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
    }
}