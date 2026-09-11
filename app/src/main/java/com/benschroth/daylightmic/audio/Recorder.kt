package com.benschroth.daylightmic.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.min
import kotlin.math.sqrt

const val SAMPLE_RATE = 16_000

/** Captures 16 kHz mono PCM16 from the microphone until [stop] is called or [maxSeconds] elapse. */
class Recorder(
    private val maxSeconds: Int,
    private val level: MutableStateFlow<Float>? = null,
    private val onLimitReached: () -> Unit,
) {
    private val stopFlag = AtomicBoolean(false)
    private val pcm = ByteArrayOutputStream()
    private var thread: Thread? = null
    private var failure: Throwable? = null

    @SuppressLint("MissingPermission") // The caller checks RECORD_AUDIO.
    fun start() {
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        require(minBuf > 0) { "16 kHz mono PCM16 is not supported on this device" }
        val record = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 4)
            .build()
        check(record.state == AudioRecord.STATE_INITIALIZED) { "Microphone could not be opened" }

        val maxBytes = maxSeconds.toLong() * SAMPLE_RATE * 2
        thread = Thread({
            val buf = ShortArray(1024) // 64 ms at 16 kHz, so the meter updates ~15 times a second
            try {
                record.startRecording()
                check(record.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Microphone did not start" }
                while (!stopFlag.get()) {
                    val n = record.read(buf, 0, buf.size)
                    if (n < 0) throw IllegalStateException("Microphone read failed ($n)")
                    if (n > 0) {
                        val bb = ByteBuffer.allocate(n * 2).order(ByteOrder.LITTLE_ENDIAN)
                        bb.asShortBuffer().put(buf, 0, n)
                        synchronized(pcm) { pcm.write(bb.array()) }
                        level?.value = meter(buf, n)
                        if (pcm.size() >= maxBytes) {
                            stopFlag.set(true)
                            onLimitReached()
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "recording failed", t)
                failure = t
            } finally {
                runCatching { record.stop() }
                record.release()
            }
        }, "daylight-mic-recorder").also { it.start() }
    }

    /** Stops capture and returns the samples. Throws if the capture failed. */
    fun stop(): ShortArray {
        stopFlag.set(true)
        thread?.join(2_000)
        failure?.let { throw it }
        val bytes = synchronized(pcm) { pcm.toByteArray() }
        val sb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        return ShortArray(sb.remaining()).also { sb.get(it) }
    }

    companion object {
        private const val TAG = "Recorder"

        /** Speech level in 0..1 for a meter. RMS with gain, so quiet voices still move the bars. */
        fun meter(buf: ShortArray, n: Int): Float {
            if (n <= 0) return 0f
            var acc = 0.0
            for (i in 0 until n) { val f = buf[i] / 32768.0; acc += f * f }
            val rms = sqrt(acc / n).toFloat()
            return min(1f, rms * 6f)
        }

        fun durationSeconds(samples: ShortArray): Float = samples.size / SAMPLE_RATE.toFloat()

        /** Root mean square in 0..1. A muted or blocked microphone returns ~0. */
        fun rms(samples: ShortArray): Float {
            if (samples.isEmpty()) return 0f
            var acc = 0.0
            for (s in samples) { val f = s / 32768.0; acc += f * f }
            return sqrt(acc / samples.size).toFloat()
        }

        fun toFloats(samples: ShortArray): FloatArray = FloatArray(samples.size) { samples[it] / 32768f }
    }
}
