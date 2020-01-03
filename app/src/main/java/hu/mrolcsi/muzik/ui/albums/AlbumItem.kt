package hu.mrolcsi.muzik.ui.albums

import android.net.Uri

data class AlbumItem(
  val id: Long,
  val albumText: CharSequence,
  val artistText: CharSequence,
  val albumArtUri: Uri
) {

  val transitionName = "coverArt$id"
}