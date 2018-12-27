package `in`.arunkumarsampath.flickerapp.home.adapter

import `in`.arunkumarsampath.flickerapp.R
import `in`.arunkumarsampath.flickerapp.data.ImageResult
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.extensions.LayoutContainer

class ImagesAdapter : RecyclerView.Adapter<ImagesAdapter.ImageViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private var images: MutableList<ImageResult> = mutableListOf()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageViewHolder = ImageViewHolder.create(parent)

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

    class ImageViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bindImageResult(imageResult: ImageResult) {
            // Load image
        }

        companion object {
            fun create(parent: ViewGroup): ImageViewHolder {
                return ImageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(
                            R.layout.layout_flicker_image_item_template,
                            parent,
                            false
                        )
                )
            }
        }
    }
}