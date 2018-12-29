package `in`.arunkumarsampath.flickerapp.home

import `in`.arunkumarsampath.flickerapp.R
import `in`.arunkumarsampath.flickerapp.data.ImageResult
import `in`.arunkumarsampath.flickerapp.di.DependencyInjector
import `in`.arunkumarsampath.flickerapp.util.Result
import `in`.arunkumarsampath.flickerapp.util.loge
import android.graphics.Point
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.Menu
import kotlinx.android.synthetic.main.activity_main.*

class HomeActivity : AppCompatActivity() {

    private val homePresenter by lazy { DependencyInjector.provideHomePresenter() }
    private val imagesAdapter by lazy { DependencyInjector.provideImagesAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupToolbar()
        setupImagesList()
        setupPresenter()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.let {
            menuInflater.inflate(R.menu.home_menu, menu)
            val searchItem = menu.findItem(R.id.menuSearchItem)
            setupSearchView(searchItem.actionView as SearchView)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onDestroy() {
        homePresenter.cleanup()
        imagesAdapter.cleanup()
        super.onDestroy()
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

    private fun setupSearchView(searchView: SearchView) {
        searchView.run {
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
    }
}