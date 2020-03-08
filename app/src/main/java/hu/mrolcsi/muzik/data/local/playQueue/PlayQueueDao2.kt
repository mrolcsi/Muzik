package hu.mrolcsi.muzik.data.local.playQueue

import hu.mrolcsi.muzik.data.model.playQueue.LastPlayed
import hu.mrolcsi.muzik.data.model.playQueue.PlayQueueEntry
import io.reactivex.Single

interface PlayQueueDao2 {

  fun getPlayQueue(): Single<List<PlayQueueEntry>>
  fun getLastPlayed(): Single<LastPlayed>

}