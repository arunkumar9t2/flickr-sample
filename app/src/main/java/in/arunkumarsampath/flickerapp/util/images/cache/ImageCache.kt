package `in`.arunkumarsampath.flickerapp.util.images.cache

import `in`.arunkumarsampath.flickerapp.util.images.ImageLoadParams
import android.graphics.Bitmap
import io.reactivex.Completable
import io.reactivex.Maybe

/**
 * Interface contract of a image cache store capable of saving and getting cached images.
 */
interface ImageCache {
    /**
     * Attempts to get cached images from this store.
     *
     * @return [Maybe] that invokes onComplete directly when the cache does not have the given [loadParams]'s image
     */
    fun get(loadParams: ImageLoadParams): Maybe<Bitmap>

    /**
     * Saves the given [ImageLoadParams.bitmap] identified by [ImageLoadParams.url] as key
     */
    fun save(loadParams: ImageLoadParams): Completable

    /**
     * Drop all cached items from this store.
     */
    fun drop()
}