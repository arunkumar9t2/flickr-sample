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

/**
 * Responsible for rendering rendering image grid and loading images by talking to [imageLoader]
 */
class ImagesAdapter(
    private val application: Application,
    private val imageLoader: ImageLoader
) : RecyclerView.Adapter<ImagesAdapter.ImageViewHolder>() {

    /**
     * Cached reusable placeholder drawable used to show loading status when a load request is in progress.
     */
    private val placeholder by lazy { ContextCompat.getDrawable(application, R.drawable.ic_refresh_24dp)!! }

    init {
        setHasStableIds(true)
    }

    /**
     * Mutable list of [ImageResult] that this adapter renders.
     */
    private var images: MutableList<ImageResult> = mutableListOf()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageViewHolder = ImageViewHolder.create(parent, imageLoader, placeholder)

    override fun onBindViewHolder(
        holder: ImageViewHolder,
        position: Int
    ) = holder.bindImageResult(getItem(position))

    override fun getItemCount() = images.size

    override fun getItemId(position: Int) = getItem(position).imageUrl.hashCode().toLong()

    private fun getItem(position: Int) = images[position]

    /**
     * Clears all existing images and sets [images] as the source of truth for this adapter
     */
    fun setImages(images: List<ImageResult>) {
        this.images.run {
            clear()
            addAll(images)
        }
        notifyDataSetChanged()
    }

    /**
     * Appends to already exsiting images and notifies of change to this adapter.
     */
    fun addImages(images: List<ImageResult>) {
        val positionStart = this.images.size
        this.images.addAll(images)
        notifyItemRangeInserted(positionStart, itemCount)
    }

    /**
     * Cleans up any expensive work done by this adapter
     */
    fun cleanup() {
        imageLoader.cleanup()
    }

    override fun onViewRecycled(holder: ImageViewHolder) {
        super.onViewRecycled(holder)
        imageLoader.cancel(holder.containerView.imageView)
    }

    class ImageViewHolder(
        override val containerView: View,
        private val imageLoader: ImageLoader,
        private val placeholder: Drawable
    ) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        /**
         * When the image is to be shown, shows the placeholder drawable initially and talks to [imageLoader] to fetch
         * the image asynchronously.
         */
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