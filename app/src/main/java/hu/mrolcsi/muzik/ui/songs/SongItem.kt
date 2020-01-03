package hu.mrolcsi.muzik.ui.songs

import android.net.Uri
import hu.mrolcsi.muzik.ui.albumDetails.AlbumDetailItem

data class SongItem(
  override val id: Long,
  val coverArtUri: Uri?,
  val trackNumberText: CharSequence?,
  val isPlaying: Boolean,
  val artistText: CharSequence,
  val titleText: CharSequence,
  val durationText: CharSequence
) : AlbumDetailItem