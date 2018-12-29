package `in`.arunkumarsampath.flickerapp.home

import `in`.arunkumarsampath.flickerapp.data.ImageResult
import `in`.arunkumarsampath.flickerapp.data.ImagesRepository
import `in`.arunkumarsampath.flickerapp.util.Result
import `in`.arunkumarsampath.flickerapp.util.schedulers.SchedulerProvider
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import java.util.concurrent.TimeUnit

/**
 * Home presenter to handle business logic and communicate to data layer. View can start the search process by calling
 * [onQueryChanged] and [loadNextPage] (load further pages within the same query)
 *
 * Currently available callbacks are
 * 1. onImagesLoaded - Returns initial list of images for a given query
 * 2. onNewPageLoaded - Returns list of images loaded for given pages.
 */
class HomePresenter(
    private val imagesRepository: ImagesRepository,
    private val schedulerProvider: SchedulerProvider
) {
    /**
     * Tracks all active subscriptions to dispose them later.
     */
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

    /**
     * Handles search logic and pagination by listening for emissions from [searchQueue] and [pagingQueue]. The [pagingQueue]
     * is dependant on [searchQueue] stream and any active page load is cancelled automatically when a new search term is
     * received using [Flowable.switchMap].
     *
     * In order to avoid excessive network calls, the input stream is debounced to `350` milliseconds
     */
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
                val searchFirstPage = imagesRepository.search(query, page)
                    .subscribeOn(schedulerProvider.io)
                    .map { it to page }
                    .doOnComplete(::incrementPage)

                val loadMorePages = pagingQueue
                    .onBackpressureBuffer()
                    .distinctUntilChanged()
                    .concatMap { (query, page) ->
                        imagesRepository.search(query, page)
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

    /**
     * Performs search for the given [newText] and notifies [onImagesLoaded]. Also any activelly performed network calls
     * are cancelled.
     */
    fun onQueryChanged(newText: String) {
        searchQueue.onNext(newText)
    }

    /**
     * Loads the next page for query previously set by [onQueryChanged].
     */
    fun loadNextPage() {
        pagingQueue.onNext(pageLoadRequest)
    }

    /**
     * Cleans up all active work done by the presenter. Should be called when the view is no longer needed.
     */
    fun cleanup() {
        subs.clear()
    }
}