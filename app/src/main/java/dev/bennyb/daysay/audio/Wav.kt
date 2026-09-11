package dev.bennyb.daysay.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

object Wav {
    /** Wraps PCM16 mono samples in a RIFF/WAVE container. */
    fun encode(samples: ShortArray, sampleRate: Int = SAMPLE_RATE): ByteArray {
        val dataBytes = samples.size * 2
        val buf = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray(Charsets.US_ASCII))
        buf.putInt(36 + dataBytes)
        buf.put("WAVE".toByteArray(Charsets.US_ASCII))
        buf.put("fmt ".toByteArray(Charsets.US_ASCII))
        buf.putInt(16)                 // PCM chunk size
        buf.putShort(1)                // PCM format
        buf.putShort(1)                // channels
        buf.putInt(sampleRate)
        buf.putInt(sampleRate * 2)     // byte rate
        buf.putShort(2)                // block align
        buf.putShort(16)               // bits per sample
        buf.put("data".toByteArray(Charsets.US_ASCII))
        buf.putInt(dataBytes)
        for (s in samples) buf.putShort(s)
        return buf.array()
    }
}
