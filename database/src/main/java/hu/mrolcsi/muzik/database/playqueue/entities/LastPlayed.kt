package hu.mrolcsi.muzik.database.playqueue.entities

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
  @ColumnInfo(name = "shuffle_seed") var shuffleSeed: Long = 0

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is LastPlayed) return false

    if (id != other.id) return false
    if (queuePosition != other.queuePosition) return false
    if (trackPosition != other.trackPosition) return false
    if (shuffleMode != other.shuffleMode) return false
    if (repeatMode != other.repeatMode) return false
    if (shuffleSeed != other.shuffleSeed) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id
    result = 31 * result + queuePosition
    result = 31 * result + trackPosition.hashCode()
    result = 31 * result + shuffleMode
    result = 31 * result + repeatMode
    result = 31 * result + shuffleSeed.hashCode()
    return result
  }

  override fun toString(): String {
    return "LastPlayed(queuePosition=$queuePosition, trackPosition=$trackPosition, shuffleMode=$shuffleMode, repeatMode=$repeatMode)"
  }

}