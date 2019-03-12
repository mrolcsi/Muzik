package hu.mrolcsi.android.lyricsplayer.database.playqueue.entities

import android.support.v4.media.MediaDescriptionCompat
import androidx.room.Entity
import hu.mrolcsi.android.lyricsplayer.database.entities.Song
import hu.mrolcsi.android.lyricsplayer.extensions.media.album
import hu.mrolcsi.android.lyricsplayer.extensions.media.artist
import hu.mrolcsi.android.lyricsplayer.extensions.media.duration

@Entity(tableName = "play_queue")
class PlayQueueEntry(
  _id: Long,
  _data: String,
  artist: String?,
  album: String?,
  title: String?,
  duration: Long?
) : Song(_id, _data, artist, album, title, duration) {

  constructor(queuePosition: Int, description: MediaDescriptionCompat) : this(
    queuePosition.toLong(),
    description.mediaId!!,
    description.artist,
    description.album,
    description.title.toString(),
    description.duration
  )
}