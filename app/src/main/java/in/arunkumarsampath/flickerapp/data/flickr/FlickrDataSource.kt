package `in`.arunkumarsampath.flickerapp.data.flickr

import `in`.arunkumarsampath.flickerapp.data.ImageResult
import `in`.arunkumarsampath.flickerapp.data.ImagesDataSource
import `in`.arunkumarsampath.flickerapp.data.common.NetworkException
import `in`.arunkumarsampath.flickerapp.util.Result
import io.reactivex.Flowable
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.Request

class FlickrImagesDataSource(private val okHttpClient: OkHttpClient) : ImagesDataSource {

    override fun search(query: String, page: Int): Flowable<Result<List<ImageResult>>> {
        return Single
            .create<List<ImageResult>> { emitter ->
                val request = Request.Builder().run {
                    url(String.format(SEARCH_URL_FORMAT, query, page))
                    build()
                }
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = response.body()?.string()
                        emitter.onSuccess(ArrayList<ImageResult>().apply {
                            repeat(10) {
                                add(ImageResult("https://i.imgur.com/PrxPeJJ.jpg"))
                            }
                        })
                    } else {
                        emitter.tryOnError(NetworkException("Request failed with status code : ${response.code()}"))
                    }
                }
            }.toFlowable()
            .compose(Result.applyToFlowable())
    }

    companion object {
        private const val SEARCH_URL_FORMAT =
            "https://api.flickr.com/services/rest/" +
                    "?method=flickr.photos.search" +
                    "&api_key=3e7cc266ae2b0e0d78e279ce8e361736" +
                    "&format=json" +
                    "&nojsoncallback=1" +
                    "&safe_search=1" +
                    "&text=%s" +
                    "&page=%d"
    }
}