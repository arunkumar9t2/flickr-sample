package `in`.arunkumarsampath.flickerapp.data

import android.support.v7.util.DiffUtil

data class ImageResult(val imageUrl: String) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ImageResult>() {
            override fun areItemsTheSame(
                oldItem: ImageResult,
                newItem: ImageResult
            ) = oldItem == newItem

            override fun areContentsTheSame(
                oldItem: ImageResult,
                newItem: ImageResult
            ) = oldItem == newItem
        }
    }
}