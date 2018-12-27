package `in`.arunkumarsampath.flickerapp.home

import `in`.arunkumarsampath.flickerapp.data.ImageResult
import `in`.arunkumarsampath.flickerapp.data.ImagesDataSource
import `in`.arunkumarsampath.flickerapp.util.Result
import `in`.arunkumarsampath.flickerapp.util.schedulers.SchedulerProvider
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import java.util.concurrent.TimeUnit

class HomePresenter(
    private val imagesDataSource: ImagesDataSource,
    private val schedulerProvider: SchedulerProvider
) {
    private val subs = CompositeDisposable()

    data class PageLoadRequest(val query: String = "", val page: Int = 1)

    private val searchQueue = PublishProcessor.create<String>()
    private val pagingQueue = PublishProcessor.create<PageLoadRequest>()

    private var pageLoadRequest = PageLoadRequest()

    var onImagesLoaded: (Result<List<ImageResult>>) -> Unit = {}
    var onNewPageLoaded: (Result<List<ImageResult>>) -> Unit = {}

    init {
        initSearchListener()
    }

    private fun initSearchListener() {
        subs.add(searchQueue
            .debounce(350, TimeUnit.MILLISECONDS, schedulerProvider.pool)
            .filter { it.isNotEmpty() }
            .distinctUntilChanged()
            .map {
                // Reset current page to 1
                pageLoadRequest = PageLoadRequest(it, 1)
                return@map pageLoadRequest
            }.switchMap { (query, page) ->
                val searchFirstPage = imagesDataSource.search(query, page)
                    .subscribeOn(schedulerProvider.io)
                    .map { it to page }
                    .doOnComplete(::incrementPage)

                val loadMorePages = pagingQueue
                    .onBackpressureBuffer()
                    .distinctUntilChanged()
                    .concatMap { (query, page) ->
                        imagesDataSource.search(query, page)
                            .subscribeOn(schedulerProvider.io)
                            .map { it to page }
                            .doOnComplete(::incrementPage)
                    }

                // Load first page and then observe for load more pages from UI until current query is changed.
                searchFirstPage
                    .concatWith(loadMorePages)
            }.observeOn(schedulerProvider.ui)
            .subscribe { (images, page) ->
                if (page > 1) {
                    onNewPageLoaded(images)
                } else {
                    onImagesLoaded(images)
                }
            })
    }

    private fun incrementPage() {
        // Increment page for next load.
        pageLoadRequest = pageLoadRequest.copy(page = pageLoadRequest.page + 1)
    }

    fun onQueryChanged(newText: String) {
        searchQueue.onNext(newText)
    }

    fun loadNextPage() {
        pagingQueue.onNext(pageLoadRequest)
    }

    fun cleanup() {
        subs.clear()
    }
}