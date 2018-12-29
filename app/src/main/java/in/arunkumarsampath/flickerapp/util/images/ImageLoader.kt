package `in`.arunkumarsampath.flickerapp.util.images

import android.widget.ImageView

interface ImageLoader {

    fun loadUrl(imageView: ImageView, url: String)

    fun cancel(imageView: ImageView)

    fun cleanup()
}