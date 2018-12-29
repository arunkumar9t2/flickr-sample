package `in`.arunkumarsampath.flickerapp.data.mock

import `in`.arunkumarsampath.flickerapp.data.ImageResult
import `in`.arunkumarsampath.flickerapp.data.ImagesDataSource
import `in`.arunkumarsampath.flickerapp.util.Result
import io.reactivex.Flowable

class MockImagesDataSource : ImagesDataSource {

    override fun search(query: String, page: Int): Flowable<Result<List<ImageResult>>> {
        return Flowable.fromCallable {
            MOCK_IMAGES_LIST
        }.compose(Result.applyToFlowable())
    }

    companion object {

        val MOCK_IMAGES_LIST by lazy {
            ArrayList<ImageResult>().apply {
                repeat(10) {
                    ImageResult("url")
                }
            }
        }
    }
}