package `in`.arunkumarsampath.flickerapp.data.flickr

import `in`.arunkumarsampath.flickerapp.data.ImageResult
import `in`.arunkumarsampath.flickerapp.data.ImagesDataSource
import `in`.arunkumarsampath.flickerapp.data.common.NetworkException
import `in`.arunkumarsampath.flickerapp.util.Result
import io.reactivex.Flowable
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class FlickrImagesDataSource(private val okHttpClient: OkHttpClient) : ImagesDataSource {

    override fun search(query: String, page: Int): Flowable<Result<List<ImageResult>>> {
        return Single
            .create<List<ImageResult>> { emitter ->
                val request = Request.Builder().run {
                    url(String.format(SEARCH_URL_FORMAT, query, page))
                    build()
                }
                try {
                    okHttpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val json = response.body()?.string()
                            if (json != null) {
                                emitter.onSuccess(parseImagesFromJson(json))
                            } else {
                                emitter.tryOnError(NetworkException("Could not parse response body"))
                            }
                        } else {
                            emitter.tryOnError(NetworkException("Request failed with status code : ${response.code()}"))
                        }
                    }
                } catch (e: Exception) {
                    emitter.tryOnError(e)
                }
            }.toFlowable()
            .compose(Result.applyToFlowable())
    }


    /**
     * Parses given json response into [List]ImageResults
     */
    private fun parseImagesFromJson(json: String): List<ImageResult> {
        val photos = JSONObject(json)
            .getJSONObject("photos")
            .getJSONArray("photo")
        return ArrayList<ImageResult>().apply {
            for (i in 0 until photos.length()) {
                photos.getJSONObject(i).run {
                    add(
                        ImageResult(
                            String.format(
                                IMAGE_URL_FORMAT,
                                getString("farm"),
                                getString("server"),
                                getString("id"),
                                getString("secret")
                            )
                        )
                    )
                }
            }
        }
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

        private const val IMAGE_URL_FORMAT = "http://farm%s.static.flickr.com/%s/%s_%s.jpg"
    }
}