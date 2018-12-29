@file:Suppress("NOTHING_TO_INLINE")

package `in`.arunkumarsampath.flickerapp.util

import `in`.arunkumarsampath.flickerapp.BuildConfig
import android.util.Log

inline fun logd(tag: String, msg: String?, include: Boolean = BuildConfig.DEBUG) {
    if (include) {
        Log.d(tag, msg)
    }
}

inline fun logv(tag: String, msg: String?, include: Boolean = BuildConfig.DEBUG) {
    if (include) {
        Log.v(tag, msg)
    }
}

inline fun loge(tag: String, msg: String?, error: Throwable, include: Boolean = BuildConfig.DEBUG) {
    if (include) {
        Log.e(tag, msg, error)
    }
}