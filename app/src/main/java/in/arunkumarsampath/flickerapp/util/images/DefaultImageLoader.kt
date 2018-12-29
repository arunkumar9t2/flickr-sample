package `in`.arunkumarsampath.flickerapp.util.images

import `in`.arunkumarsampath.flickerapp.util.doOnLayout
import `in`.arunkumarsampath.flickerapp.util.images.DefaultImageLoader.Companion.MAX_CONCURRENT_REQUESTS
import `in`.arunkumarsampath.flickerapp.util.images.cache.ImageCache
import `in`.arunkumarsampath.flickerapp.util.loge
import `in`.arunkumarsampath.flickerapp.util.schedulers.SchedulerProvider
import android.graphics.BitmapFactory
import android.widget.ImageView
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.ref.WeakReference

/**
 * Imageloader to asynchronously load images onto a [ImageView] instance. The loader is capable of concurrently performing
 * multiple requests, max of which is defined by [MAX_CONCURRENT_REQUESTS].
 *
 * The loaded image is also downsized to the given [ImageView]'s dimensions and does not load the full image into
 * memory
 *
 * Once a [Bitmap] is loaded from network, it is cached with the provided [imageCache] instance.
 */
class DefaultImageLoader(
    private val schedulerProvider: SchedulerProvider,
    private val imageCache: ImageCache,
    private val okHttpClient: OkHttpClient
) : ImageLoader {

    /**
     * Subject to convert load requests to a data stream
     */
    private val loadQueue = PublishProcessor.create<ImageLoadParams>()
    /**
     * Subject to convert cancel requests for a particular [ImageView] into a data sream.
     */
    private val cancelProcessor = PublishProcessor.create<ImageView>()

    private val subs = CompositeDisposable()

    init {
        initLoadQueueProcessor()
    }

    /**
     * The connecting logic to receive and manage multiple load requests. Responsible for handling incoming requests with
     * backpressure and performing them concurrently.
     *
     * @see loadImage
     * @see waitForLayout
     */
    private fun initLoadQueueProcessor() {
        val imageLoader: (ImageLoadParams) -> Flowable<ImageLoadParams> = { imageLoadParams ->
            waitForLayout(imageLoadParams)
                .flatMap(::loadImage)
                .toFlowable()
                .subscribeOn(schedulerProvider.io)
        }

        loadQueue
            .observeOn(schedulerProvider.io)
            .onBackpressureBuffer()
            .flatMap(imageLoader, MAX_CONCURRENT_REQUESTS)
            .observeOn(schedulerProvider.ui)
            .doOnError { loge(TAG, "initLoadQueueProcessor", it) }
            .subscribe { imageLoadParams ->
                imageLoadParams.run {
                    imageViewReference.get()?.setImageBitmap(bitmap)
                }
            }.let { subs.add(it) }
    }


    /**
     * Asynchronously loads the given images specified by [url] into [imageView]
     */
    override fun loadUrl(imageView: ImageView, url: String) {
        loadQueue.onNext(ImageLoadParams(WeakReference(imageView), url))
    }

    /**
     * Cancels any pending load for [imageView]
     */
    override fun cancel(imageView: ImageView) {
        cancelProcessor.onNext(imageView)
    }


    /**
     * A [Single] that completes immediately if the given [ImageLoadParams.imageViewReference].imageView is
     * already laid out or waits until the [ImageView] is fully laid out before completing.
     *
     * This is required to know the ImageView's dimensions for the next step which is decoding.
     *
     * @see loadImage
     */
    private fun waitForLayout(loadData: ImageLoadParams) = Single
        .create<ImageLoadParams> { emitter ->
            loadData.imageViewReference
                .get()?.doOnLayout {
                    // Safe to access width and height now
                    emitter.onSuccess(loadData)
                } ?: emitter.tryOnError(IllegalStateException("ImageView has been garbage collected"))
        }.doOnError {
            loge(TAG, "waitForLayout", it)
        }.onErrorReturnItem(loadData)


    /**
     * Tries to load image from [imageCache], if it fails then load the image over the network and downscales it to
     * fit the current [ImageView]'s dimensions.
     *
     * During this network load, if the [ImageView] received a cancel request via [cancel] then load and decode process
     * is terminated.
     */
    private fun loadImage(imageLoadParams: ImageLoadParams): Single<ImageLoadParams> {
        val networkLoader = Single
            .create<ImageLoadParams> { emitter ->
                try {
                    val imageView = imageLoadParams.imageViewReference.get()
                        ?: throw IllegalStateException("ImageView garbage collected")

                    // Get the stream from URL
                    val request = Request.Builder().url(imageLoadParams.url).build()

                    val call = okHttpClient.newCall(request).apply {
                        execute().use { response ->
                            // Create BitmapOptions to ask decoder to decode just the bounds first
                            val options = BitmapFactory.Options().apply {
                                inJustDecodeBounds = true
                            }
                            // Calculate sample size based on imageView's dimensions
                            calculateInSampleSize(options, imageView.width, imageView.height)
                            // Safe to decode
                            val bitmap = BitmapFactory.decodeStream(response.body()?.byteStream())
                            emitter.onSuccess(imageLoadParams.copy(bitmap = bitmap))
                        }
                    }
                    emitter.setCancellable { call.cancel() }
                } catch (e: Exception) {
                    // Return source as-is
                    emitter.onSuccess(imageLoadParams)
                    // emitter.tryOnError(e)
                }
            }.takeUntil(cancelProcessor.filter { imageLoadParams.imageViewReference.get() == it })
            .onErrorReturnItem(imageLoadParams)
            .flatMap { params -> imageCache.save(params).toSingleDefault(params) }
            .subscribeOn(schedulerProvider.io)

        // Try to load from cache, failing which fallback to network loading.
        return imageCache.get(imageLoadParams)
            .map { bitmap -> imageLoadParams.copy(bitmap = bitmap) }
            .switchIfEmpty(networkLoader)
    }

    /**
     * Given an already decodes [options] with the resources' dimensions, calculates the [BitmapFactory.Options.inSampleSize]
     * to match the [reqWidth] and [reqHeight]
     *
     * @return Calculated [inSampleSize]
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val (height, width) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    override fun cleanup() {
        imageCache.drop()
        subs.clear()
    }

    companion object {
        private const val MAX_CONCURRENT_REQUESTS = 6
        private val TAG = DefaultImageLoader::class.java.name
    }
}