package hu.mrolcsi.android.lyricsplayer.database.entities

import android.provider.MediaStore
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.os.bundleOf
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import hu.mrolcsi.android.lyricsplayer.extensions.media.album
import hu.mrolcsi.android.lyricsplayer.extensions.media.artist
import hu.mrolcsi.android.lyricsplayer.extensions.media.duration
import hu.mrolcsi.android.lyricsplayer.extensions.media.id

open class Song(
  @PrimaryKey @ColumnInfo(name = MediaStore.Audio.Media._ID) var _id: Long,
  @ColumnInfo(name = MediaStore.Audio.Media.DATA) var _data: String,
  @ColumnInfo(name = MediaStore.Audio.Media.ARTIST) var artist: String?,
  @ColumnInfo(name = MediaStore.Audio.Media.ALBUM) var album: String?,
  @ColumnInfo(name = MediaStore.Audio.Media.TITLE) var title: String?,
  @ColumnInfo(name = MediaStore.Audio.Media.DURATION) var duration: Long?
) {

  constructor(description: MediaDescriptionCompat) : this(
    description.id,
    description.mediaId!!,
    description.artist,
    description.album,
    description.title.toString(),
    description.duration
  )

  fun createDescription(): MediaDescriptionCompat =
    MediaDescriptionCompat.Builder()
      .setMediaId(_data)
      .setTitle(title)
      .setSubtitle(artist)
      .setDescription(album)
      .setExtras(
        bundleOf(
          MediaStore.Audio.AudioColumns._ID to _id,
          MediaStore.Audio.AudioColumns.DURATION to duration
        )
      ).build()

}