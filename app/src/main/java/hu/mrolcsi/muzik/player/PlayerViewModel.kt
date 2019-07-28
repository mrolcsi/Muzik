package hu.mrolcsi.muzik.player

import android.support.v4.media.session.MediaSessionCompat
import androidx.lifecycle.LiveData
import hu.mrolcsi.muzik.library.SessionViewModel

interface PlayerViewModel : SessionViewModel {

  val currentQueue: LiveData<List<MediaSessionCompat.QueueItem>>

}