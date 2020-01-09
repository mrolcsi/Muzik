package hu.mrolcsi.muzik

import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import androidx.core.os.bundleOf
import hu.mrolcsi.muzik.ui.common.extensions.toKeyString
import io.mockk.every
import io.mockk.mockk

object TestData {

  fun createAlbumMediaItem(
    id: Long,
    title: String? = null,
    artist: String? = null
  ): MediaBrowserCompat.MediaItem = mockk {
    every { description } returns mockk {
      every { extras } returns bundleOf(
        MediaStore.Audio.Media._ID to id.toString()
      )
      every { description } returns title
      every { subtitle } returns artist
    }
  }

  fun createSongMediaItem(
    id: Long,
    mediaId: String,
    title: String,
    artist: String,
    discNumber: Int = -1,
    trackNumber: Int = -1,
    durationMs: Long = -1,
    dateAddedSeconds: Long = -1,
    isPlayable: Boolean = true
  ): MediaBrowserCompat.MediaItem {
    return mockk {
      every { this@mockk.isPlayable } returns isPlayable
      every { this@mockk.mediaId } returns mediaId
      every { description } returns mockk desc@{
        every { extras } returns bundleOf(
          MediaStore.Audio.Media._ID to id.toString(),
          MediaStore.Audio.Media.TITLE to title,
          MediaStore.Audio.Media.TITLE_KEY to title.toKeyString(),
          MediaStore.Audio.Media.ARTIST to artist,
          MediaStore.Audio.Media.ARTIST_KEY to artist.toKeyString(),
          MediaStore.Audio.Media.DURATION to durationMs.toString(),
          MediaStore.Audio.Media.DATE_ADDED to dateAddedSeconds.toString(),
          MediaStore.Audio.Media.TRACK to run {
            if (trackNumber > 0)
              if (discNumber > 0) discNumber * 1000 + trackNumber
              else trackNumber
            else -1
          }.toString()
        )
        every { subtitle } returns artist
        every { this@desc.title } returns title
      }
    }
  }

}