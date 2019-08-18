package hu.mrolcsi.muzik.database.playqueue.entities

import android.provider.MediaStore
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.os.bundleOf
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import hu.mrolcsi.muzik.service.extensions.media.album
import hu.mrolcsi.muzik.service.extensions.media.artist
import hu.mrolcsi.muzik.service.extensions.media.duration
import hu.mrolcsi.muzik.service.extensions.media.id
import hu.mrolcsi.muzik.service.extensions.media.mediaPath

@Entity(tableName = "play_queue")
data class PlayQueueEntry(
  @PrimaryKey @ColumnInfo(name = MediaStore.Audio.Media._ID) val _id: Long,
  @ColumnInfo(name = MediaStore.Audio.Media.DATA) val _data: String,
  @ColumnInfo(name = "media_id") val mediaId: Long,
  @ColumnInfo(name = MediaStore.Audio.Media.ARTIST) val artist: String?,
  @ColumnInfo(name = MediaStore.Audio.Media.ALBUM) val album: String?,
  @ColumnInfo(name = MediaStore.Audio.Media.TITLE) val title: String?,
  @ColumnInfo(name = MediaStore.Audio.Media.DURATION) val duration: Long?
) {

  constructor(position: Int, description: MediaDescriptionCompat) : this(
    position.toLong(),
    description.mediaPath.toString(),
    description.id,
    description.artist,
    description.album,
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
        MediaStore.Audio.AudioColumns._ID to mediaId.toString(),
        MediaStore.Audio.AudioColumns.DURATION to duration.toString()
      )
    ).build()

}

