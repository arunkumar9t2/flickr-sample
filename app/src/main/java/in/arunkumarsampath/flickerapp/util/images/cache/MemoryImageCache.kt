package `in`.arunkumarsampath.flickerapp.util.images.cache

import `in`.arunkumarsampath.flickerapp.util.images.ImageLoadParams
import `in`.arunkumarsampath.flickerapp.util.logv
import android.graphics.Bitmap
import android.support.v4.util.LruCache
import io.reactivex.Completable
import io.reactivex.Maybe

class MemoryImageCache : ImageCache {

    private val memoryCache by lazy {
        // Use a memory cache upto 1/8 of available memory
        val cacheMemory = (Runtime.getRuntime().maxMemory() / 1024) / 8
        object : LruCache<String, Bitmap>(cacheMemory.toInt()) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return value.byteCount / 1024
            }
        }
    }

    override fun get(loadParams: ImageLoadParams): Maybe<Bitmap> =
        Maybe.fromCallable { memoryCache.get(loadParams.url) }

    override fun save(loadParams: ImageLoadParams) = Completable.fromAction {
        loadParams.bitmap?.let {
            memoryCache.put(loadParams.url, loadParams.bitmap)
            logv(TAG, "Saved ${loadParams.url} to cache")
        }
    }

    override fun drop() {
        memoryCache.evictAll()
    }

    companion object {
        private val TAG = MemoryImageCache::class.java.name
    }
}