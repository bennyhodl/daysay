package dev.bennyb.daysay.engine

interface TranscriptionEngine {
    /**
     * @param samples 16 kHz mono PCM16
     * @param language ISO-639-1 code, or empty for auto detect
     */
    suspend fun transcribe(samples: ShortArray, language: String): String
}
