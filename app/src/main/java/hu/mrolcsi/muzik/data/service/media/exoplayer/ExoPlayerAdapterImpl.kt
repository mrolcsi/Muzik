package hu.mrolcsi.muzik.data.service.media.exoplayer

import android.support.v4.media.MediaDescriptionCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import hu.mrolcsi.muzik.data.model.playQueue.LastPlayed
import io.reactivex.Completable
import io.reactivex.Observable
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber

class ExoPlayerAdapterImpl : ExoPlayerAdapter, KoinComponent {

  private val exoPlayer: Player by inject()
  private val mediaSessionConnector: MediaSessionConnector by inject()
  private val metadataProvider: ExoMetadataProvider by inject()

  override val playerEvents: Observable<PlayerEvent> = Observable.empty()
  override val notificationEvents: Observable<NotificationEvent> = Observable.empty()

  init {
    // Initialize MediaSessionConnector
    mediaSessionConnector.apply {
      setPlayer(exoPlayer)
      setMediaMetadataProvider(metadataProvider)

//      setPlaybackPreparer(mPlaybackPreparer)
//      setCustomActionProviders(
//        mPrepareFromDescriptionActionProvider,
//        mSetShuffleModeActionProvider
//      )
//      setControlDispatcher(mPlaybackController)
//      setQueueNavigator(mQueueNavigator)
//      setQueueEditor(mQueueEditor)
//      setErrorMessageProvider(mErrorMessageProvider)
//      registerCustomCommandReceiver(mCustomCommandReceiver)
    }
  }

  override fun loadQueue(descriptions: List<MediaDescriptionCompat>): Completable {
    Timber.d("loadQueue($descriptions)")

    // Prepare from descriptions
    // Clear Queue
    // Get the desired position of the item to be moved to.
    // Add Description to Queue
    // Start playback when ready

    return Completable.complete()
  }

  override fun loadLastPlayed(lastPlayed: LastPlayed): Completable {
    Timber.d("loadLastPlayed($lastPlayed)")

//    mPlaybackController.dispatchSetShuffleModeEnabled(
//      player,
//      shuffleMode != PlaybackStateCompat.SHUFFLE_MODE_NONE,
//      shuffleSeed ?: Random.nextLong()
//    )

    return Completable.complete()
  }

  override fun isPlaying(): Boolean = exoPlayer.playWhenReady

  override fun release() {
    Timber.d("release()")

    mediaSessionConnector.setPlayer(null)
    exoPlayer.release()
  }
}