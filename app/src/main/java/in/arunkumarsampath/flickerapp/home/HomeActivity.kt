package `in`.arunkumarsampath.flickerapp.home

import `in`.arunkumarsampath.flickerapp.R
import `in`.arunkumarsampath.flickerapp.util.logd
import android.graphics.Point
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.view.Menu
import kotlinx.android.synthetic.main.activity_main.*

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupToolbar()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.let {
            menuInflater.inflate(R.menu.home_menu, menu)
            val searchItem = menu.findItem(R.id.menuSearchItem)
            setupSearchView(searchItem.actionView as SearchView)
        }
        return super.onCreateOptionsMenu(menu)
    }


    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    private fun setupSearchView(searchView: SearchView) {
        searchView.run {
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
                    logd(TAG, query)
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    logd(TAG, newText)
                    return true
                }
            })
        }
    }

    companion object {
        private val TAG = HomeActivity::class.java.name
    }
}