package dev.bennyb.daysay.engine

/** JNI bridge to the vendored whisper.cpp. See app/src/main/cpp/jni.c. */
object WhisperLib {
    init {
        System.loadLibrary("daysay")
    }

    @JvmStatic external fun initContext(modelPath: String): Long
    @JvmStatic external fun freeContext(ptr: Long)
    @JvmStatic external fun transcribe(ptr: Long, nThreads: Int, language: String, initialPrompt: String, audio: FloatArray): String
    @JvmStatic external fun systemInfo(): String
}
