package hu.mrolcsi.muzik.data.service.media

import android.app.Service
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.VisibleForTesting
import androidx.media.session.MediaButtonReceiver
import androidx.navigation.NavDeepLinkBuilder
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.data.local.playQueue.PlayQueueDao2
import hu.mrolcsi.muzik.data.model.playQueue.LastPlayed
import hu.mrolcsi.muzik.data.service.media.exoplayer.ExoPlayerAdapter
import hu.mrolcsi.muzik.data.service.media.exoplayer.NotificationEvent
import hu.mrolcsi.muzik.data.service.media.exoplayer.PlayerEvent
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject
import timber.log.Timber

class MuzikPlayerService : MuzikBrowserService() {

  private val mediaSession: MediaSessionCompat by inject()
  private val playQueueDao: PlayQueueDao2 by inject()
  private val exoPlayerAdapter: ExoPlayerAdapter by inject()

  private val disposables = CompositeDisposable()

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal var isForeground = false

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    MediaButtonReceiver.handleIntent(mediaSession, intent)
    return Service.START_STICKY
  }

  override fun onCreate() {
    super.onCreate()

    Timber.i("onCreate()")

    // Set up MediaSession
    mediaSession.let { session ->
      // Set the session's token so that client activities can communicate with it.
      sessionToken = session.sessionToken

      // Enable callbacks from MediaButtons and TransportControls and QueueCommands
      session.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)

      // Prepare Pending Intent to Player
      val playerPendingIntent =
        NavDeepLinkBuilder(this)
          .setGraph(R.navigation.main_navigation)
          .setDestination(R.id.navPlayer)
          .createPendingIntent()
      session.setSessionActivity(playerPendingIntent)
    }

    // Set up ExoPlayerAdapter
    exoPlayerAdapter.observePlayerEvents()
      .doOnNext { Timber.v(it.toString()) }
      .subscribeBy(
        onNext = { event ->
          when (event) {
            is PlayerEvent.PlayerStateChanged -> {
              // Remove service from the foreground (makes notification dismissible)
              if (!event.playWhenReady && isForeground) {
                stopForeground(false)
                isForeground = false
              }

              //if (event.playbackState == Player.STATE_READY){
              //  // Might need to load last played queue here
              //}
            }
            is PlayerEvent.PlayerError -> {
              Timber.e(event.error)
            }
          }
        },
        onError = { Timber.e(it, "Error while observing Player Events") }
      )
      .addTo(disposables)

    exoPlayerAdapter.observeNotificationEvents()
      .doOnNext { Timber.v(it.toString()) }
      .subscribeBy(
        onNext = { event ->
          when (event) {
            is NotificationEvent.NotificationPosted -> {
              if (exoPlayerAdapter.isPlaying() && !isForeground) {
                startService(Intent(applicationContext, MuzikPlayerService::class.java))
                startForeground(event.notificationId, event.notification)
                isForeground = true
              }

              if (!exoPlayerAdapter.isPlaying() && isForeground) {
                stopForeground(false)
                isForeground = false
              }
            }
            is NotificationEvent.NotificationCanceled -> {
              stopForeground(true)
              stopSelf()
              isForeground = false
            }
          }
        },
        onError = { Timber.e(it, "Error while observing Notification Events") }
      )
      .addTo(disposables)

    // Load last played queue
    Singles.zip(
        playQueueDao.getPlayQueue()
          .map { entries -> entries.map { it.toDescription() } },
        playQueueDao.getLastPlayed()
          .onErrorReturnItem(LastPlayed())
      )
      .subscribeOn(Schedulers.io())
      .flatMapCompletable { (queue, lastPlayed) ->
        exoPlayerAdapter.loadQueue(queue)
          .andThen(exoPlayerAdapter.loadLastPlayed(lastPlayed))
      }
      .subscribeBy(
        onError = { Timber.e(it, "Failed to load Last Played Queue") }
      )
      .addTo(disposables)
  }

  override fun onDestroy() {
    super.onDestroy()

    Timber.i("onDestroy()")

    mediaSession.run {
      isActive = false
      release()
    }

    exoPlayerAdapter.release()
    disposables.dispose()
  }
}