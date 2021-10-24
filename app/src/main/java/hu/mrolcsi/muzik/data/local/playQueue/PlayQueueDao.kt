package hu.mrolcsi.muzik.data.local.playQueue

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import hu.mrolcsi.muzik.data.model.playQueue.LastPlayed
import hu.mrolcsi.muzik.data.model.playQueue.PlayQueueEntry
import io.reactivex.Observable

@Dao
interface PlayQueueDao {

  //region -- QUEUE --

  // INSERTS

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertEntries(vararg entries: PlayQueueEntry)

  // DELETES

  @Delete
  fun removeEntries(vararg entries: PlayQueueEntry)

  @Query("DELETE FROM play_queue WHERE _id = :id")
  fun removeEntry(id: Long)

  /**
   * Note: range is inclusive.
   */
  @Query("DELETE FROM play_queue WHERE _id BETWEEN :from AND :to")
  fun removeEntriesInRange(from: Int, to: Int)

  @Query("DELETE from play_queue")
  fun clearQueue()

  // QUERIES

  @Query("SELECT * FROM play_queue ORDER BY _id")
  fun observeQueue(): Observable<List<PlayQueueEntry>>

  @Deprecated("Use Observables!")
  @Query("SELECT * FROM play_queue ORDER BY _id")
  fun getQueue(): List<PlayQueueEntry>

  //endregion

  //region -- LAST PLAYED --

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun saveLastPlayed(lastPlayed: LastPlayed)

  @Query("SELECT * FROM last_played LIMIT 1")
  fun observeLastPlayed(): Observable<LastPlayed>

  @Query("SELECT * FROM last_played LIMIT 1")
  fun getLastPlayed(): LastPlayed?

  //endregion
}