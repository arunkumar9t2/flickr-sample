package `in`.arunkumarsampath.flickerapp.data

import `in`.arunkumarsampath.flickerapp.util.Result
import io.reactivex.Flowable

/**
 * Repository for loading images from multiple data sources
 */
interface ImagesRepository {

    fun search(query: String, page: Int): Flowable<Result<List<ImageResult>>>
}