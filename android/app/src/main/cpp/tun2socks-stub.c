/**
 * Stub implementation of tun2socks JNI interface
 *
 * This is a PLACEHOLDER that allows the app to build and run without the real
 * hev-socks5-tunnel library. It does NOT provide actual VPN functionality.
 *
 * Replace this with the real library for production use.
 * See NATIVE_LIBRARY_GUIDE.md for instructions.
 */

#include <jni.h>
#include <android/log.h>

#define LOG_TAG "Tun2SocksStub"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

/**
 * Start tun2socks (STUB IMPLEMENTATION)
 *
 * In real implementation, this would:
 * 1. Read config from configPath
 * 2. Initialize lwIP stack
 * 3. Start routing TUN traffic to SOCKS5
 *
 * Returns:  0 on success, -1 on failure
 */
JNIEXPORT jint JNICALL
Java_com_eraser_recovery_vpn_Tun2SocksManager_nativeStart(
        JNIEnv *env, jobject thiz, jstring config_path, jint tun_fd) {

    const char *path = (*env)->GetStringUTFChars(env, config_path, NULL);

    LOGW("========================================");
    LOGW("STUB IMPLEMENTATION - NOT FUNCTIONAL");
    LOGW("========================================");
    LOGW("nativeStart called with:");
    LOGW("  Config: %s", path);
    LOGW("  TUN FD: %d", tun_fd);
    LOGW("");
    LOGW("This is a PLACEHOLDER implementation.");
    LOGW("VPN will appear to start but will NOT route traffic.");
    LOGW("");
    LOGW("For actual functionality, replace with real library:");
    LOGW("See NATIVE_LIBRARY_GUIDE.md");
    LOGW("========================================");

    (*env)->ReleaseStringUTFChars(env, config_path, path);

    // Return success to allow app to continue
    return 0;
}

/**
 * Stop tun2socks (STUB IMPLEMENTATION)
 */
JNIEXPORT void JNICALL
Java_com_eraser_recovery_vpn_Tun2SocksManager_nativeStop(
        JNIEnv *env, jobject thiz) {

    LOGD("nativeStop called (stub)");
}

/**
 * Get traffic statistics (STUB IMPLEMENTATION)
 *
 * Returns array: [tx_packets, tx_bytes, rx_packets, rx_bytes]
 * All zeros since no actual traffic is being routed
 */
JNIEXPORT jlongArray JNICALL
Java_com_eraser_recovery_vpn_Tun2SocksManager_nativeStats(
        JNIEnv *env, jobject thiz) {

    jlongArray result = (*env)->NewLongArray(env, 4);
    if (result == NULL) {
        return NULL;
    }

    jlong stats[4] = {0, 0, 0, 0};  // All zeros
    (*env)->SetLongArrayRegion(env, result, 0, 4, stats);

    return result;
}
