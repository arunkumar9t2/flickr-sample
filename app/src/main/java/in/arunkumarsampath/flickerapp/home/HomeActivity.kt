package `in`.arunkumarsampath.flickerapp.home

import `in`.arunkumarsampath.flickerapp.R
import `in`.arunkumarsampath.flickerapp.data.ImageResult
import `in`.arunkumarsampath.flickerapp.di.DependencyInjector
import `in`.arunkumarsampath.flickerapp.util.Result
import `in`.arunkumarsampath.flickerapp.util.loge
import android.graphics.Point
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.Menu
import kotlinx.android.synthetic.main.activity_main.*

class HomeActivity : AppCompatActivity() {
    /**
     * Presenter to lift data layer and logical code out of the activity.
     */
    private val homePresenter by lazy { DependencyInjector.provideHomePresenter() }
    private val imagesAdapter by lazy { DependencyInjector.provideImagesAdapter() }

    /**
     * State variable to track query across configuration changes.
     *
     * @see onSaveInstanceState
     * @see restoreState
     */
    private var initialQuery = ""

    /**
     * Reference to [SearchView] inflated via [getMenuInflater]. Should be accessed after [onCreateOptionsMenu], otherwise
     * not initialized exception will be thrown.
     */
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupToolbar()
        setupImagesList()
        setupPresenter()
        restoreState(savedInstanceState)
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        initialQuery = savedInstanceState?.getString(EXTRA_QUERY, "") ?: ""
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.let {
            menuInflater.inflate(R.menu.home_menu, menu)
            val searchItem = menu.findItem(R.id.menuSearchItem)
            searchView = searchItem.actionView as SearchView
            setupSearchView()
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onDestroy() {
        homePresenter.cleanup()
        imagesAdapter.cleanup()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString(EXTRA_QUERY, searchView.query.toString())
    }

    private fun setupPresenter() {
        homePresenter.run {
            onImagesLoaded = {
                handleImagesResult(it, imagesAdapter::setImages)
                imagesListView.scrollToPosition(0)
            }
            onNewPageLoaded = {
                handleImagesResult(it, imagesAdapter::addImages)
            }
        }
    }

    /**
     * Handles common logic when a new list of images are received for either new search or new page.
     * After the logic(loading indicators) is performed the list can be accessed in [images] callback
     */
    private fun handleImagesResult(result: Result<List<ImageResult>>, images: (List<ImageResult>) -> Unit) {
        when (result) {
            is Result.Loading -> {
                loading(true)
            }
            is Result.Success -> {
                loading(false)
                images(result.data)
            }
            is Result.Failure -> {
                loading(false)
                loge(TAG, "Loading failed", result.throwable)
                Snackbar.make(coordinatorLayout, R.string.error_occured, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun setupImagesList() {
        imagesListView.run {
            adapter = imagesAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (!canScrollVertically(1)) {
                        homePresenter.loadNextPage()
                    }
                }
            })
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    /**
     * Binds [HomePresenter] with [SearchView] actions and restores state if this activity instance was recreated part
     * of config change.
     */
    private fun setupSearchView() {
        searchView.run {
            // Set initial query as part of state restoration
            setQuery(initialQuery, true)
            homePresenter.onQueryChanged(initialQuery)
            isIconified = false
            queryHint = getString(R.string.search_flickr)
            // Let the search view take full width of the screen.
            // The default behavior does not look good in landscape mode since width is constrained.
            maxWidth = windowManager.defaultDisplay.run {
                Point().let { size ->
                    getSize(size)
                    return@let size.x // X is the width of the screen.
                }
            }
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let {
                        homePresenter.onQueryChanged(query.trim())
                    }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let {
                        homePresenter.onQueryChanged(newText.trim())
                    }
                    return true
                }
            })
        }
    }

    private fun loading(loading: Boolean) {
        swipeRefreshLayout.run {
            isRefreshing = loading
            isEnabled = loading
        }
    }

    companion object {
        private val TAG = HomeActivity::class.java.name

        private const val EXTRA_QUERY = "EXTRA_QUERY"
    }
}