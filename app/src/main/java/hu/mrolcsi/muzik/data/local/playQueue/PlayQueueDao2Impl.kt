package hu.mrolcsi.muzik.data.local.playQueue

import androidx.room.Dao
import androidx.room.Query
import hu.mrolcsi.muzik.data.model.playQueue.LastPlayed
import hu.mrolcsi.muzik.data.model.playQueue.PlayQueueEntry
import io.reactivex.Single

@Dao
abstract class PlayQueueDao2Impl : PlayQueueDao2 {

  @Query("SELECT * FROM last_played LIMIT 1")
  abstract override fun getLastPlayed(): Single<LastPlayed>

  @Query("SELECT * FROM play_queue ORDER BY _id")
  abstract override fun getPlayQueue(): Single<List<PlayQueueEntry>>

}