package hu.mrolcsi.muzik.player

import android.support.v4.media.session.MediaSessionCompat.QueueItem
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.request.target.Target
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.glide.onLoadFailed
import hu.mrolcsi.muzik.common.glide.onResourceReady
import hu.mrolcsi.muzik.common.view.MVVMViewHolder
import hu.mrolcsi.muzik.databinding.ListItemQueueBinding
import hu.mrolcsi.muzik.service.extensions.media.coverArtUri
import hu.mrolcsi.muzik.theme.Theme
import hu.mrolcsi.muzik.theme.ThemeService
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.list_item_queue.view.*
import kotlin.properties.Delegates

class QueueItemHolder(
  parent: ViewGroup,
  private val themeService: ThemeService,
  private val binding: ListItemQueueBinding =
    ListItemQueueBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
) : MVVMViewHolder<QueueItem>(binding.root) {

  override var model: QueueItem? by Delegates.observable(null) { _, _: QueueItem?, new: QueueItem? ->
    new?.let { bind(it) }
  }

  var usedTheme: Theme? by Delegates.observable(null) { _, _: Theme?, theme: Theme? ->
    theme?.let { binding.theme = it }
  }

  private fun bind(item: QueueItem) {
    itemView.run {
      GlideApp.with(imgCoverArt)
        .asBitmap()
        .load(item.description.coverArtUri)
        .override(Target.SIZE_ORIGINAL)
        .onResourceReady { albumArt ->
          usedTheme = null
          themeService.createTheme(albumArt).subscribeBy { usedTheme = it }
        }
        .onLoadFailed { usedTheme = null; false }
        .into(imgCoverArt)

      tvTitle.text = item.description.title
      tvArtist.text = item.description.subtitle
      tvAlbum.text = item.description.description
    }
  }

  companion object {
    @Suppress("unused")
    private const val LOG_TAG = "QueueItemHolder"
  }

}