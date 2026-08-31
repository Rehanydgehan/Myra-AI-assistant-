package com.myra.assistant.ai

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.sqrt

class AudioEngine {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var recordJob: Job? = null
    private var playJob: Job? = null

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private val playQueue = ConcurrentLinkedQueue<ByteArray>()
    private var isPlaying = false

    private val _rmsFlow = MutableStateFlow(0f)
    val rmsFlow: StateFlow<Float> = _rmsFlow

    var onAudioData: ((ByteArray) -> Unit)? = null
    var isMuted = false

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (recordJob?.isActive == true) return

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(1024)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord?.startRecording()
        recordJob = scope.launch {
            val buffer = ByteArray(1024)
            while (isActive) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val data = buffer.copyOf(read)
                    if (!isMuted && !isPlaying) {
                        onAudioData?.invoke(data)
                        calculateRms(data)
                    } else if (isPlaying) {
                        // Suppress mic visually too or just keep RMS low
                        _rmsFlow.value = 0f
                    } else {
                        _rmsFlow.value = 0f
                    }
                }
                delay(1)
            }
        }
    }

    fun stopRecording() {
        recordJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    fun startPlayback() {
        if (playJob?.isActive == true) return

        val sampleRate = 24000
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(1024)

        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize,
            AudioTrack.MODE_STREAM
        )

        audioTrack?.play()

        playJob = scope.launch {
            while (isActive) {
                val chunk = playQueue.poll()
                if (chunk != null) {
                    isPlaying = true
                    audioTrack?.write(chunk, 0, chunk.size)
                    calculateRms(chunk)
                } else {
                    isPlaying = false
                    delay(10)
                }
            }
        }
    }

    fun queueAudio(pcm: ByteArray) {
        playQueue.add(pcm)
    }

    fun stopPlayback() {
        playJob?.cancel()
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        playQueue.clear()
        isPlaying = false
    }

    fun interruptPlayback() {
        playQueue.clear()
        audioTrack?.pause()
        audioTrack?.flush()
        audioTrack?.play()
        isPlaying = false
    }

    private fun calculateRms(buffer: ByteArray) {
        var sum = 0.0
        val shorts = ShortArray(buffer.size / 2)
        java.nio.ByteBuffer.wrap(buffer).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
        for (sample in shorts) {
            val normalized = sample / 32768.0
            sum += normalized * normalized
        }
        val rms = if (shorts.isNotEmpty()) sqrt(sum / shorts.size) else 0.0
        _rmsFlow.value = (rms.toFloat() * 5f).coerceIn(0f, 1f)
    }
}
