package `in`.arunkumarsampath.flickerapp.util.images.cache

import `in`.arunkumarsampath.flickerapp.util.images.ImageLoadParams
import android.graphics.Bitmap
import io.reactivex.Completable
import io.reactivex.Maybe

interface ImageCache {

    fun get(loadParams: ImageLoadParams): Maybe<Bitmap>

    fun save(loadParams: ImageLoadParams): Completable

    fun drop()
}