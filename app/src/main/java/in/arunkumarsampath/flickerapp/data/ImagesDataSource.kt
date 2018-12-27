package `in`.arunkumarsampath.flickerapp.data

import `in`.arunkumarsampath.flickerapp.util.Result
import io.reactivex.Flowable

interface ImagesDataSource {

    fun search(query: String, page: Int): Flowable<Result<List<ImageResult>>>
}