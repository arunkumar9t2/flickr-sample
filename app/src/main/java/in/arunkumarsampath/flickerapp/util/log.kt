@file:Suppress("NOTHING_TO_INLINE")

package `in`.arunkumarsampath.flickerapp.util

import `in`.arunkumarsampath.flickerapp.BuildConfig
import android.util.Log

public inline fun logd(tag: String, msg: String?, include: Boolean = BuildConfig.DEBUG) {
    if (include) {
        Log.d(tag, msg)
    }
}

public inline fun loge(tag: String, msg: String?, error: Throwable, include: Boolean = BuildConfig.DEBUG) {
    if (include) {
        Log.e(tag, msg, error)
    }
}