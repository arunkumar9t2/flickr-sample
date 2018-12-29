package `in`.arunkumarsampath.flickerapp.util.images

import android.graphics.Bitmap
import android.widget.ImageView
import java.lang.ref.WeakReference

data class ImageLoadParams(
    val imageViewReference: WeakReference<ImageView>,
    val url: String,
    val bitmap: Bitmap? = null
)