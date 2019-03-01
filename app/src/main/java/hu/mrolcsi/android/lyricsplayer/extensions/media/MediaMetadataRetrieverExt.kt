package hu.mrolcsi.android.lyricsplayer.extensions.media

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.support.v4.media.MediaMetadataCompat

fun MediaMetadataRetriever.build(): MediaMetadataCompat = MediaMetadataCompat.Builder().apply {
  artist = this@build.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
  album = this@build.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
  title = this@build.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
  duration = this@build.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
  this@build.embeddedPicture?.let { albumArt = BitmapFactory.decodeByteArray(it, 0, it.size) }
  // TODO: other metadata?
}.build()