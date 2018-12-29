package `in`.arunkumarsampath.flickerapp

import `in`.arunkumarsampath.flickerapp.data.ImageResult
import `in`.arunkumarsampath.flickerapp.data.source.mock.MockImagesDataSource
import `in`.arunkumarsampath.flickerapp.di.DependencyInjector
import `in`.arunkumarsampath.flickerapp.home.HomePresenter
import `in`.arunkumarsampath.flickerapp.util.Result
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [19, 21, 24])
class HomePresenterTest {

    lateinit var homePresenter: HomePresenter

    @Suppress("DEPRECATION")
    @Before
    fun setup() {
        DependencyInjector.setup(
            application = RuntimeEnvironment.application,
            imagesSource = DependencyInjector.ImagesSource.MOCK,
            schedulerType = DependencyInjector.SchedulerType.TEST
        )
        homePresenter = DependencyInjector.provideHomePresenter()
    }

    @Test
    fun `test search query returns images`() {
        val newPagesObsTester = Observable.create<Result<List<ImageResult>>> { emitter ->
            homePresenter.onImagesLoaded = {
                emitter.onNext(it)
            }
        }.test()

        homePresenter.onQueryChanged("test")

        newPagesObsTester
            .assertNotComplete()
            .assertNoErrors()
            .assertValueAt(0) { it is Result.Loading }
            .assertValueAt(1) { it is Result.Success && it.data == MockImagesDataSource.MOCK_IMAGES_LIST }
    }


    @Test
    fun `test new page loaded on scroll`() {
        val newPagesObsTester = Observable.create<Result<List<ImageResult>>> { emitter ->
            homePresenter.onNewPageLoaded = {
                emitter.onNext(it)
            }
        }.test()

        homePresenter.loadNextPage()

        newPagesObsTester
            .assertNotComplete()
            .assertNoErrors()
            .assertValueAt(0) { it is Result.Loading }
            .assertValueAt(1) { it is Result.Success && it.data == MockImagesDataSource.MOCK_IMAGES_LIST }
    }

    @After
    fun tearDown() {
        homePresenter.cleanup()
    }
}
