package hu.mrolcsi.muzik.player

import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.request.target.Target
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.view.MVVMViewHolder
import hu.mrolcsi.muzik.databinding.ListItemQueueBinding
import hu.mrolcsi.muzik.service.extensions.media.coverArtUri
import kotlinx.android.synthetic.main.list_item_queue.view.*
import kotlin.properties.Delegates

class QueueItemHolder(
  parent: ViewGroup,
  private val binding: ListItemQueueBinding =
    ListItemQueueBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
) : MVVMViewHolder<ThemedQueueItem>(binding.root) {

  override var model: ThemedQueueItem? by Delegates.observable(null) { _, old: ThemedQueueItem?, new: ThemedQueueItem? ->
    if (old != new) {
      new?.let { bind(it) }
    }
  }

  private fun bind(item: ThemedQueueItem) {
    binding.theme = item.theme

    itemView.run {
      GlideApp.with(imgCoverArt)
        .asBitmap()
        .load(item.queueItem.description.coverArtUri)
        .override(Target.SIZE_ORIGINAL)
        .into(imgCoverArt)

      tvTitle.text = item.queueItem.description.title
      tvArtist.text = item.queueItem.description.subtitle
      tvAlbum.text = item.queueItem.description.description
    }
  }

  companion object {
    @Suppress("unused")
    private const val LOG_TAG = "QueueItemHolder"
  }

}