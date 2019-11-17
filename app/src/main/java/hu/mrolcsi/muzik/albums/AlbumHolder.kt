package hu.mrolcsi.muzik.albums

import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.glide.onResourceReady
import hu.mrolcsi.muzik.common.view.MVVMViewHolder
import hu.mrolcsi.muzik.databinding.ListItemAlbumHorizontalBinding
import hu.mrolcsi.muzik.databinding.ListItemAlbumVerticalBinding
import hu.mrolcsi.muzik.service.extensions.media.albumArtUri
import hu.mrolcsi.muzik.service.extensions.media.id
import hu.mrolcsi.muzik.theme.Theme
import hu.mrolcsi.muzik.theme.ThemeService
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.list_item_album_content.view.*
import kotlin.properties.Delegates

class AlbumHolder(
  parent: ViewGroup,
  val viewLifecycleOwner: LifecycleOwner,
  @RecyclerView.Orientation orientation: Int,
  private val themeService: ThemeService,
  private val albumTheme: MutableLiveData<Theme> = MutableLiveData()
) : MVVMViewHolder<MediaBrowserCompat.MediaItem>(
  if (orientation == RecyclerView.VERTICAL) {
    ListItemAlbumVerticalBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
      theme = albumTheme
      lifecycleOwner = viewLifecycleOwner
    }.root
  } else {
    ListItemAlbumHorizontalBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
      theme = albumTheme
      lifecycleOwner = viewLifecycleOwner
    }.root
  }
) {

  override var model: MediaBrowserCompat.MediaItem? by Delegates.observable(null) { _, _: MediaBrowserCompat.MediaItem?, new: MediaBrowserCompat.MediaItem? ->
    new?.let { bind(it) }
  }

  private fun bind(item: MediaBrowserCompat.MediaItem) {
    itemView.run {
      // Set texts
      tvAlbumTitle.text = item.description.title
      tvAlbumArtist.text = item.description.subtitle

      ViewCompat.setTransitionName(imgCoverArt, "coverArt${item.description.id}")

      // Load album art
      GlideApp.with(imgCoverArt)
        .asBitmap()
        .load(item.description.albumArtUri)
        .onResourceReady { albumArt ->
          themeService.createTheme(albumArt).subscribeBy { albumTheme.value = it }
        }
        .into(imgCoverArt)
    }
  }


}