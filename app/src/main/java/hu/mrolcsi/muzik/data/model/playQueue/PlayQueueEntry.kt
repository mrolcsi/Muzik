package hu.mrolcsi.muzik.data.model.playQueue

import android.provider.MediaStore
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.os.bundleOf
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import hu.mrolcsi.muzik.data.model.media.album
import hu.mrolcsi.muzik.data.model.media.albumId
import hu.mrolcsi.muzik.data.model.media.artist
import hu.mrolcsi.muzik.data.model.media.artistId
import hu.mrolcsi.muzik.data.model.media.duration
import hu.mrolcsi.muzik.data.model.media.id
import hu.mrolcsi.muzik.data.model.media.mediaPath

@Entity(tableName = "play_queue")
data class PlayQueueEntry(
  @PrimaryKey @ColumnInfo(name = MediaStore.Audio.Media._ID) val _id: Long,
  @ColumnInfo(name = MediaStore.Audio.Media.DATA) val _data: String,
  @ColumnInfo(name = "media_id") val mediaId: Long,
  @ColumnInfo(name = MediaStore.Audio.Media.ARTIST) val artist: String?,
  @ColumnInfo(name = MediaStore.Audio.Media.ARTIST_ID) val artistId: Long,
  @ColumnInfo(name = MediaStore.Audio.Media.ALBUM) val album: String?,
  @ColumnInfo(name = MediaStore.Audio.Media.ALBUM_ID) val albumId: Long,
  @ColumnInfo(name = MediaStore.Audio.Media.TITLE) val title: String?,
  @ColumnInfo(name = MediaStore.Audio.Media.DURATION) val duration: Long?
) {

  constructor(position: Int, description: MediaDescriptionCompat) : this(
    position.toLong(),
    description.mediaPath.toString(),
    description.id,
    description.artist,
    description.artistId ?: 0,
    description.album,
    description.albumId ?: 0,
    description.title.toString(),
    description.duration
  )

  fun toDescription(): MediaDescriptionCompat = MediaDescriptionCompat.Builder()
    .setMediaId(_data)
    .setTitle(title)
    .setSubtitle(artist)
    .setDescription(album)
    .setExtras(
      bundleOf(
        // Store everything as String
        MediaStore.Audio.Media._ID to mediaId.toString(),
        MediaStore.Audio.Media.ARTIST_ID to artistId.toString(),
        MediaStore.Audio.Media.ALBUM_ID to albumId.toString(),
        MediaStore.Audio.Media.DURATION to duration.toString()
      )
    ).build()

}