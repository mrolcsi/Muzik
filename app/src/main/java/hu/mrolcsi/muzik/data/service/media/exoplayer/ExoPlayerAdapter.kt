package hu.mrolcsi.muzik.data.service.media.exoplayer

import android.app.Notification
import android.support.v4.media.MediaDescriptionCompat
import com.google.android.exoplayer2.ExoPlaybackException
import hu.mrolcsi.muzik.data.model.playQueue.LastPlayed
import io.reactivex.Completable
import io.reactivex.Observable

interface ExoPlayerAdapter {

  fun loadQueue(descriptions: List<MediaDescriptionCompat>): Completable
  fun loadLastPlayed(lastPlayed: LastPlayed): Completable

  fun observePlayerEvents(): Observable<PlayerEvent>
  fun observeNotificationEvents(): Observable<NotificationEvent>

  fun isPlaying(): Boolean

  fun release()
}

sealed class PlayerEvent {

  data class PlayerStateChanged(
    val playWhenReady: Boolean,
    val playbackState: Int
  ) : PlayerEvent()

  data class PlayerError(
    val error: ExoPlaybackException
  ) : PlayerEvent()
}

sealed class NotificationEvent {

  data class NotificationPosted(
    val notificationId: Int,
    val notification: Notification,
    val ongoing: Boolean
  ) : NotificationEvent()

  data class NotificationCanceled(
    val notificationId: Int,
    val dismissedByUser: Boolean
  ) : NotificationEvent()
}