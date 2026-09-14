package dev.bennyb.daysay.engine

/** JNI bridge to the vendored whisper.cpp's Parakeet (TDT) support. See app/src/main/cpp/jni.c. */
object ParakeetLib {
    init {
        System.loadLibrary("daysay")
    }

    @JvmStatic external fun initContext(modelPath: String): Long
    @JvmStatic external fun freeContext(ptr: Long)
    @JvmStatic external fun transcribe(ptr: Long, nThreads: Int, audio: FloatArray): String
    @JvmStatic external fun systemInfo(): String
}
