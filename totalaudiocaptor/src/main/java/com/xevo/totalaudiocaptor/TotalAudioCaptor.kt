package com.xevo.totalaudiocaptor

import android.media.*
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

class TotalAudioCaptor : CoroutineScope{

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job


    class BoxedByteArray(ba: ByteArray) {
        var bytearray: ByteArray = ba
    }
    interface Callback {
        fun onReceive(boxedByteArray: BoxedByteArray)
    }

    var callback: Callback? = null
    private val samplingRate = 44100
    //private val fps = 3
    //private val fps = 147
    //private val fps = 245
    private val fps = 105
    //private val fps = 7
    //private val fps = 21
    //private val fps = 441
    private val samplingNumPerFrame = samplingRate / fps
    //private val frameSizeInBytes = samplingNumPerFrame
    //private val frameSizeInBytes = samplingNumPerFrame * 2
    private val frameSizeInBytes = samplingNumPerFrame * 2 * 2
    private var audioRecord: AudioRecord? = null
    private val audioBufferSizeInBytes = max(samplingNumPerFrame * 10,
        AudioRecord.getMinBufferSize(samplingRate, AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT))
//    private val audioBufferSizeInBytes =
//        AudioRecord.getMinBufferSize(samplingRate, AudioFormat.CHANNEL_IN_STEREO,
//            AudioFormat.ENCODING_PCM_16BIT)
    private var audioTrack: AudioTrack? = null

    private val operationQueue = SerializedOperationQueue()
    fun startRecording() {


        intArrayOf(8000, 11025, 16000, 22050, 44100, 48000, 90000, 90001).forEach {
            val ret = AudioRecord.getMinBufferSize(it, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT)
            Log.d("mogawa", "$it:$ret")

        }

        launch {
            delay(10)
        }

        listOf(0,1,2,3,4).forEach {
            operationQueue.push {
                val ms = (5 - it) * 1000L
                delay(ms)
                println("Job[$it]@${Thread.currentThread().name} $ms")
            }
            println("Push[$it]@${Thread.currentThread().name}")
        }


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


        //val audioDataArray = ByteArray(frameSizeInBytes)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.REMOTE_SUBMIX,
            //MediaRecorder.AudioSource.MIC,
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
                            AudioTrack.WRITE_NON_BLOCKING
                        )
                        //                        operationQueue.push {
//                        }
                        //Log.d("mo", "frameSizeInBytes:$frameSizeInBytes, ret:$ret")
                        //launch(Dispatchers.Default) {
                            callback?.onReceive(BoxedByteArray(audioDataArray))
                        //}
                        //audioTrack?.write(audioDataArray, 0 ,audioDataArray.size, AudioTrack.WRITE_BLOCKING)
                        //launch(Dispatchers.Default) {






//                        audioTrack?.write(
//                                audioDataArray,
//                                0,
//                                audioDataArray.size,
//                                AudioTrack.WRITE_NON_BLOCKING
//                            )
                        //}
                    }
                    //Log.d("mogawa", if (Looper.getMainLooper().isCurrentThread) "This is UI thread" else "This is UI NOT thread")
                    //Log.d("mogawa", "read=$ret")
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

    class SerializedOperationQueue(name: String = "EventLoop", capacity: Int = 0) : CoroutineScope {
        private val job = Job()
        override val coroutineContext: CoroutineContext
            get() = Dispatchers.Main + job
        private val singleThreadContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        private val queue = Channel<suspend () -> Unit>().also {
            launch(singleThreadContext) {
                for (op in it) {
                    op.invoke()
                }
            }
        }

        fun push(operation: suspend () -> Unit) = launch(Dispatchers.Default) {
            queue.send(operation)
        }
    }


}