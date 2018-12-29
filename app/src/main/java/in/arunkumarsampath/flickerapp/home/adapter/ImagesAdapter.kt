package `in`.arunkumarsampath.flickerapp.home.adapter

import `in`.arunkumarsampath.flickerapp.R
import `in`.arunkumarsampath.flickerapp.data.ImageResult
import `in`.arunkumarsampath.flickerapp.util.images.ImageLoader
import android.app.Application
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.layout_flicker_image_item_template.*
import kotlinx.android.synthetic.main.layout_flicker_image_item_template.view.*

class ImagesAdapter(
    private val application: Application,
    private val imageLoader: ImageLoader
) : RecyclerView.Adapter<ImagesAdapter.ImageViewHolder>() {

    private val placeholder by lazy { ContextCompat.getDrawable(application, R.drawable.ic_refresh_24dp)!! }

    init {
        setHasStableIds(true)
    }

    private var images: MutableList<ImageResult> = mutableListOf()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageViewHolder = ImageViewHolder.create(parent, imageLoader, placeholder)

    override fun onBindViewHolder(
        holder: ImageViewHolder,
        position: Int
    ) {
        holder.bindImageResult(getItem(position))
    }

    override fun getItemCount() = images.size

    override fun getItemId(position: Int) = getItem(position).imageUrl.hashCode().toLong()

    private fun getItem(position: Int) = images[position]

    fun setImages(images: List<ImageResult>) {
        this.images.run {
            clear()
            addAll(images)
        }
        notifyDataSetChanged()
    }

    fun addImages(images: List<ImageResult>) {
        val positionStart = this.images.size
        this.images.addAll(images)
        notifyItemRangeInserted(positionStart, itemCount)
    }

    fun cleanup() {
        imageLoader.cleanup()
    }

    override fun onViewDetachedFromWindow(holder: ImageViewHolder) {
        super.onViewDetachedFromWindow(holder)
        imageLoader.cancel(holder.containerView.imageView)
    }

    class ImageViewHolder(
        override val containerView: View,
        private val imageLoader: ImageLoader,
        private val placeholder: Drawable
    ) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bindImageResult(imageResult: ImageResult) {
            imageView.setImageDrawable(placeholder)
            imageLoader.loadUrl(
                imageView = imageView,
                url = imageResult.imageUrl
            )
        }

        companion object {
            fun create(
                parent: ViewGroup,
                imageLoader: ImageLoader,
                placeholder: Drawable
            ) = ImageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.layout_flicker_image_item_template,
                        parent,
                        false
                    ),
                imageLoader,
                placeholder
            )
        }
    }
}