package `in`.arunkumarsampath.flickerapp.di

import `in`.arunkumarsampath.flickerapp.data.ImagesDataSource
import `in`.arunkumarsampath.flickerapp.data.flickr.FlickrImagesDataSource
import `in`.arunkumarsampath.flickerapp.data.mock.MockImagesDataSource
import `in`.arunkumarsampath.flickerapp.di.DependencyInjector.setup
import `in`.arunkumarsampath.flickerapp.home.HomePresenter
import `in`.arunkumarsampath.flickerapp.home.adapter.ImagesAdapter
import `in`.arunkumarsampath.flickerapp.util.images.DefaultImageLoader
import `in`.arunkumarsampath.flickerapp.util.images.cache.ImageCache
import `in`.arunkumarsampath.flickerapp.util.images.cache.MemoryImageCache
import `in`.arunkumarsampath.flickerapp.util.schedulers.AppSchedulerProvider
import `in`.arunkumarsampath.flickerapp.util.schedulers.SchedulerProvider
import `in`.arunkumarsampath.flickerapp.util.schedulers.TestSchedulerProvider
import android.app.Application
import okhttp3.Cache
import okhttp3.OkHttpClient

/**
 * Single source of truth for providing and creating dependent classes.
 *
 * Example usage:
 * ```
 *      DependencyInjector.provideHomePresenter()
 * ```
 *
 * The class provides limited configuration options configured via [setup]. Failing to call [setup] results in a [RuntimeException].
 */
object DependencyInjector {

    /**
     * Types of [ImagesSource]
     */
    enum class ImagesSource {
        MOCK,
        FLICKR
    }

    /**
     * Types of [SchedulerProvider]
     */
    enum class SchedulerType {
        APP,
        TEST
    }

    private lateinit var application: Application
    private val safeApplication
        get() = if (::application.isInitialized) {
            application
        } else throw IllegalStateException("application instance not initialized, please call setup() in Application.onCreate()")

    private var imagesSource = ImagesSource.FLICKR
    private var schedulerType = SchedulerType.APP

    fun setup(
        application: Application,
        imagesSource: ImagesSource = ImagesSource.FLICKR,
        schedulerType: SchedulerType = SchedulerType.APP
    ) {
        this.application = application
        this.imagesSource = imagesSource
        this.schedulerType = schedulerType
    }

    private val okHttpCache by lazy {
        Cache(safeApplication.cacheDir, 10L * 1024 * 1024)
    }

    /**
     * Singleton instance of [OkHttpClient]
     */
    val okHttpClient by lazy {
        OkHttpClient.Builder().run {
            cache(okHttpCache)
            build()
        }
    }

    /**
     * Singleton instance of [SchedulerProvider]
     */
    val schedulerProvider: SchedulerProvider by lazy {
        when (schedulerType) {
            SchedulerType.APP -> AppSchedulerProvider()
            SchedulerType.TEST -> TestSchedulerProvider()
        }
    }

    /**
     * Singleton instance of [ImagesDataSource]
     */
    val imagesDataSource: ImagesDataSource by lazy {
        when (imagesSource) {
            ImagesSource.MOCK -> MockImagesDataSource()
            ImagesSource.FLICKR -> FlickrImagesDataSource(okHttpClient)
        }
    }

    /**
     * Singleton instance of [MemoryImageCache]
     */
    val imageCache: ImageCache by lazy { MemoryImageCache() }

    fun provideHomePresenter() = HomePresenter(imagesDataSource, schedulerProvider)

    fun provideImageLoader() = DefaultImageLoader(schedulerProvider, imageCache, okHttpClient)

    fun provideImagesAdapter() = ImagesAdapter(application, provideImageLoader())
}