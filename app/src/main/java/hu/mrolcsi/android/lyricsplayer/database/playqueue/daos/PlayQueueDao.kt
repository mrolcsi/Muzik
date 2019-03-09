package hu.mrolcsi.android.lyricsplayer.database.playqueue.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import hu.mrolcsi.android.lyricsplayer.database.playqueue.entities.LastPlayed
import hu.mrolcsi.android.lyricsplayer.database.playqueue.entities.PlayQueueEntry

@Dao
interface PlayQueueDao {

  //region -- QUEUE --

  // INSERTS

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun addEntries(vararg entries: PlayQueueEntry)

  // DELETES

  @Delete
  fun removeEntries(vararg entries: PlayQueueEntry)

  @Query("DELETE FROM play_queue WHERE queue_position = :position")
  fun removeEntryAtPosition(position: Int)

  @Query("DELETE FROM play_queue WHERE queue_position BETWEEN :from AND :to")
  fun removeEntriesInRange(from: Int, to: Int)

  @Query("DELETE from play_queue")
  fun clearQueue()

  // QUERIES

  @Query("SELECT * FROM play_queue ORDER BY queue_position")
  fun queryQueue(): LiveData<List<PlayQueueEntry>>

  @Query("SELECT * FROM play_queue ORDER BY queue_position")
  fun getQueue(): List<PlayQueueEntry>

  //endregion

  //region -- LAST PLAYED --

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun saveLastPlayed(lastPlayed: LastPlayed)

  @Query("SELECT * FROM last_played")
  fun queryLastPlayed(): LiveData<LastPlayed>

  @Query("SELECT * FROM last_played")
  fun getLastPlayed(): LastPlayed?

  //endregion
}