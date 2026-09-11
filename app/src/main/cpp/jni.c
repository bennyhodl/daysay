#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>
#include "whisper.h"

#define TAG "DaysayJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define UNUSED(x) (void)(x)

static void log_callback(enum ggml_log_level level, const char *text, void *user_data) {
    UNUSED(user_data);
    int prio = level == GGML_LOG_LEVEL_ERROR ? ANDROID_LOG_ERROR
             : level == GGML_LOG_LEVEL_WARN  ? ANDROID_LOG_WARN
             : ANDROID_LOG_DEBUG;
    __android_log_print(prio, "whisper", "%s", text);
}

JNIEXPORT jlong JNICALL
Java_dev_bennyb_daysay_engine_WhisperLib_initContext(JNIEnv *env, jclass clazz, jstring model_path) {
    UNUSED(clazz);
    whisper_log_set(log_callback, NULL);
    const char *path = (*env)->GetStringUTFChars(env, model_path, NULL);
    struct whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;
    struct whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
    if (ctx == NULL) {
        LOGW("Failed to load model from %s", path);
    }
    (*env)->ReleaseStringUTFChars(env, model_path, path);
    return (jlong) ctx;
}

JNIEXPORT void JNICALL
Java_dev_bennyb_daysay_engine_WhisperLib_freeContext(JNIEnv *env, jclass clazz, jlong ptr) {
    UNUSED(env);
    UNUSED(clazz);
    if (ptr != 0) {
        whisper_free((struct whisper_context *) ptr);
    }
}

JNIEXPORT jstring JNICALL
Java_dev_bennyb_daysay_engine_WhisperLib_transcribe(
        JNIEnv *env, jclass clazz, jlong ptr, jint n_threads, jstring language,
        jstring initial_prompt, jfloatArray audio) {
    UNUSED(clazz);
    struct whisper_context *ctx = (struct whisper_context *) ptr;
    if (ctx == NULL) {
        return (*env)->NewStringUTF(env, "");
    }

    const char *lang = language ? (*env)->GetStringUTFChars(env, language, NULL) : NULL;
    const char *prompt = initial_prompt ? (*env)->GetStringUTFChars(env, initial_prompt, NULL) : NULL;

    jfloat *samples = (*env)->GetFloatArrayElements(env, audio, NULL);
    const jsize n_samples = (*env)->GetArrayLength(env, audio);

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.translate = false;
    params.language = (lang && strlen(lang) > 0) ? lang : "auto";
    params.n_threads = n_threads;
    params.no_context = true;
    params.single_segment = false;
    params.suppress_blank = true;
    params.suppress_nst = true;
    params.initial_prompt = (prompt && strlen(prompt) > 0) ? prompt : NULL;

    jstring result;
    if (whisper_full(ctx, params, samples, n_samples) != 0) {
        LOGW("whisper_full failed");
        result = (*env)->NewStringUTF(env, "");
    } else {
        const int n = whisper_full_n_segments(ctx);
        size_t cap = 1024;
        size_t len = 0;
        char *buf = (char *) malloc(cap);
        buf[0] = '\0';
        for (int i = 0; i < n; i++) {
            const char *seg = whisper_full_get_segment_text(ctx, i);
            size_t sl = strlen(seg);
            if (len + sl + 1 > cap) {
                while (len + sl + 1 > cap) cap *= 2;
                buf = (char *) realloc(buf, cap);
            }
            memcpy(buf + len, seg, sl);
            len += sl;
            buf[len] = '\0';
        }
        result = (*env)->NewStringUTF(env, buf);
        free(buf);
    }

    (*env)->ReleaseFloatArrayElements(env, audio, samples, JNI_ABORT);
    if (lang) (*env)->ReleaseStringUTFChars(env, language, lang);
    if (prompt) (*env)->ReleaseStringUTFChars(env, initial_prompt, prompt);
    return result;
}

JNIEXPORT jstring JNICALL
Java_dev_bennyb_daysay_engine_WhisperLib_systemInfo(JNIEnv *env, jclass clazz) {
    UNUSED(clazz);
    return (*env)->NewStringUTF(env, whisper_print_system_info());
}
