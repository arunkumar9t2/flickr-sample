package `in`.arunkumarsampath.flickerapp.di

import `in`.arunkumarsampath.flickerapp.data.ImagesDataSource
import `in`.arunkumarsampath.flickerapp.data.flickr.FlickrImagesDataSource
import `in`.arunkumarsampath.flickerapp.home.HomePresenter
import `in`.arunkumarsampath.flickerapp.util.schedulers.AppSchedulerProvider
import android.app.Application
import okhttp3.Cache
import okhttp3.OkHttpClient

object DependencyInjector {

    enum class ImagesSource {
        MOCK,
        FLICKR
    }

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
        val cacheSize = 10L * 1024 * 1024 // 10 Mb
        Cache(safeApplication.cacheDir, cacheSize)
    }

    val okHttpClient by lazy {
        OkHttpClient.Builder().run {
            cache(okHttpCache)
            build()
        }
    }

    val schedulerProvider by lazy {
        when (schedulerType) {
            SchedulerType.APP -> AppSchedulerProvider()
            SchedulerType.TEST -> AppSchedulerProvider()
        }
    }

    val imagesDataSource: ImagesDataSource by lazy {
        when (imagesSource) {
            ImagesSource.MOCK -> FlickrImagesDataSource(okHttpClient)
            ImagesSource.FLICKR -> FlickrImagesDataSource(okHttpClient)
        }
    }

    fun provideHomePresenter(): HomePresenter {
        return HomePresenter(imagesDataSource, schedulerProvider)
    }
}