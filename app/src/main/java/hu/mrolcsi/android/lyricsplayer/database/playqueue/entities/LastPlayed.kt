package hu.mrolcsi.android.lyricsplayer.database.playqueue.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "last_played")
class LastPlayed {

  // Always use the same PrimaryKey to ensure
  // we only have this one entry in the database.
  @PrimaryKey var id: Int = 0

  @ColumnInfo(name = "queue_position") var queuePosition: Int = 0
  @ColumnInfo(name = "track_position") var trackPosition: Long = 0
  @ColumnInfo(name = "shuffle_mode") var shuffleMode: Int = 0 // PlaybackStateCompat.SHUFFLE_MODE_NONE
  @ColumnInfo(name = "repeat_mode") var repeatMode: Int = 2 // PlaybackStateCompat.REPEAT_MODE_ALL

  override fun toString(): String {
    return "LastPlayed(queuePosition=$queuePosition, trackPosition=$trackPosition, shuffleMode=$shuffleMode, repeatMode=$repeatMode)"
  }

}