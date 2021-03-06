package `in`.arunkumarsampath.flickerapp.data.source

import `in`.arunkumarsampath.flickerapp.data.ImageResult
import `in`.arunkumarsampath.flickerapp.util.Result
import io.reactivex.Flowable

/**
 * Images data source contract that defines structure for searching images.
 */
interface ImagesDataSource {

    fun search(query: String, page: Int): Flowable<Result<List<ImageResult>>>
}