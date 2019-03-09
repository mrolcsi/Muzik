package hu.mrolcsi.android.lyricsplayer.database.playqueue.entities

import android.support.v4.media.MediaDescriptionCompat
import androidx.room.ColumnInfo
import androidx.room.Entity
import hu.mrolcsi.android.lyricsplayer.database.entities.Song
import hu.mrolcsi.android.lyricsplayer.extensions.media.album
import hu.mrolcsi.android.lyricsplayer.extensions.media.artist
import hu.mrolcsi.android.lyricsplayer.extensions.media.duration
import hu.mrolcsi.android.lyricsplayer.extensions.media.id

@Entity(tableName = "play_queue")
class PlayQueueEntry(
  @ColumnInfo(name = "queue_position") var queuePosition: Int,
  _id: Long,
  _data: String,
  artist: String?,
  album: String?,
  title: String?,
  duration: Long?
) : Song(_id, _data, artist, album, title, duration) {

  constructor(queuePosition: Int, description: MediaDescriptionCompat) : this(
    queuePosition,
    description.id,
    description.mediaId!!,
    description.artist,
    description.album,
    description.title.toString(),
    description.duration
  )
}