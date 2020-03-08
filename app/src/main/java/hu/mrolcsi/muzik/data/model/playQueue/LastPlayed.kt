package hu.mrolcsi.muzik.data.model.playQueue

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.exoplayer2.Player

@Entity(tableName = "last_played")
data class LastPlayed(
  @ColumnInfo(name = "queue_position") var queuePosition: Int = 0,
  @ColumnInfo(name = "track_position") var trackPosition: Long = 0,
  @ColumnInfo(name = "shuffle_mode") var shuffleMode: Int = 0, // PlaybackStateCompat.SHUFFLE_MODE_NONE
  @ColumnInfo(name = "repeat_mode") @Player.RepeatMode var repeatMode: Int = Player.REPEAT_MODE_ALL,
  @ColumnInfo(name = "shuffle_seed") var shuffleSeed: Long = 0,
  @ColumnInfo(name = "queue_title") var queueTitle: String = ""
) {

  // Always use the same PrimaryKey to ensure
  // we only have this one entry in the database.
  @PrimaryKey var id: Int = 0
}