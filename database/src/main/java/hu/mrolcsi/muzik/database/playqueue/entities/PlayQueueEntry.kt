package hu.mrolcsi.muzik.database.playqueue.entities

import androidx.room.Entity
import hu.mrolcsi.muzik.database.entities.Song

@Entity(tableName = "play_queue")
class PlayQueueEntry(
  _id: Long,
  _data: String,
  artist: String?,
  album: String?,
  title: String?,
  duration: Long?
) : Song(_id, _data, artist, album, title, duration) {

  companion object
}