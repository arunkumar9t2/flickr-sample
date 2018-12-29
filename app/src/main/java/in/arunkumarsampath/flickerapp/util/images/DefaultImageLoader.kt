package `in`.arunkumarsampath.flickerapp.util.images

import `in`.arunkumarsampath.flickerapp.util.doOnLayout
import `in`.arunkumarsampath.flickerapp.util.images.cache.ImageCache
import `in`.arunkumarsampath.flickerapp.util.logd
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

class DefaultImageLoader(
    private val schedulerProvider: SchedulerProvider,
    private val imageCache: ImageCache,
    private val okHttpClient: OkHttpClient
) : ImageLoader {

    private val loadQueue = PublishProcessor.create<ImageLoadParams>()
    private val cancelProcessor = PublishProcessor.create<ImageView>()

    private val subs = CompositeDisposable()

    init {
        initLoadQueueProcessor()
    }

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
            .concatMapEager(imageLoader, MAX_CONCURRENT_REQUESTS, Flowable.bufferSize())
            .observeOn(schedulerProvider.ui)
            .doOnError { loge(TAG, "initLoadQueueProcessor", it) }
            .subscribe { imageLoadParams ->
                imageLoadParams.run {
                    imageViewReference.get()?.setImageBitmap(bitmap)
                }
            }.let { subs.add(it) }
    }


    override fun loadUrl(imageView: ImageView, url: String) {
        loadQueue.onNext(ImageLoadParams(WeakReference(imageView), url))
    }

    override fun cancel(imageView: ImageView) {
        cancelProcessor.onNext(imageView)
    }


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
                    emitter.setCancellable {
                        logd(TAG, "Cancelled ${imageLoadParams.url}")
                        call.cancel()
                    }
                } catch (e: Exception) {
                    // Return source as-is
                    emitter.onSuccess(imageLoadParams)
                    // emitter.tryOnError(e)
                }
            }.takeUntil(cancelProcessor.filter { imageLoadParams.imageViewReference.get() == it })
            .onErrorReturnItem(imageLoadParams)
            .flatMap { params -> imageCache.save(params).toSingleDefault(params) }
            .subscribeOn(schedulerProvider.io)

        return imageCache.get(imageLoadParams)
            .map { bitmap -> imageLoadParams.copy(bitmap = bitmap) }
            .switchIfEmpty(networkLoader)
    }


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