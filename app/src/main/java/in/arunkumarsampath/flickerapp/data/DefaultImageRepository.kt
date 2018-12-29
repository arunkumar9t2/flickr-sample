package `in`.arunkumarsampath.flickerapp.data

import `in`.arunkumarsampath.flickerapp.data.source.ImagesDataSource
import `in`.arunkumarsampath.flickerapp.util.Result
import io.reactivex.Flowable

class DefaultImageRepository(private val remoteDataSource: ImagesDataSource) : ImagesRepository {

    override fun search(query: String, page: Int): Flowable<Result<List<ImageResult>>> {
        return remoteDataSource.search(query, page)
    }
}