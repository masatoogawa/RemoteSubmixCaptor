package com.xevo.totalaudiocaptor

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import java.io.FileOutputStream
import kotlin.math.max

class TotalAudioCaptor {

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




}