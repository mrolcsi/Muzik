package hu.mrolcsi.muzik.media

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import hu.mrolcsi.muzik.service.MuzikPlayerService
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaServiceImpl @Inject constructor(
  private val app: Application
) : MediaService {

  override val mediaBrowser: MediaBrowserCompat by lazy {
    MediaBrowserCompat(
      app,
      ComponentName(app, MuzikPlayerService::class.java),
      connectionCallbacks,
      null // optional Bundle
    )
  }

  private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
    override fun onConnected() {
      mediaBrowser.sessionToken.also { token ->

        Log.v("MediaService", "Connected to session: $token")

        // Attach a controller to this MediaSession
        val controller = MediaControllerCompat(app, token).apply {
          // Register callbacks to watch for changes
          registerCallback(object : MediaControllerCompat.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
              metadata?.let {
                Log.v("MediaService", "onMetadataChanged(${metadata.description})")

                // Check if metadata has actually changed
                // Only post metadata whn it has an albumArt
                if (metadata.description?.mediaId != lastMetadata?.description?.mediaId) {
                  metadataSubject.onNext(metadata)
                  // Save as last received metadata
                  lastMetadata = metadata
                }
              }
            }

            override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
              //Log.v(getLogTag(), "onPlaybackStateChanged($state)")/
              playbackStateSubject.onNext(state)
            }
          })
        }
        mediaControllerSubject.onNext(controller)

        // Set initial state, and metadata
        if (!playbackStateSubject.hasValue()) playbackStateSubject.onNext(controller.playbackState)
        if (!metadataSubject.hasValue()) metadataSubject.onNext(controller.metadata)
      }
    }
  }

  private var lastMetadata: MediaMetadataCompat? = null

  private val metadataSubject = BehaviorSubject.create<MediaMetadataCompat>()
  private val playbackStateSubject = BehaviorSubject.create<PlaybackStateCompat>()

  private val mediaControllerSubject = BehaviorSubject.create<MediaControllerCompat>()

  override val currentMetadata: Observable<MediaMetadataCompat> = metadataSubject.hide()
  override val currentPlaybackState: Observable<PlaybackStateCompat> = playbackStateSubject.hide()
  override val mediaController: Observable<MediaControllerCompat> = mediaControllerSubject.hide()

  override fun subscribeWithObservable(parentId: String): Observable<List<MediaBrowserCompat.MediaItem>> =
    Observable.create<List<MediaBrowserCompat.MediaItem>> { emitter ->
      mediaBrowser.subscribe(
        parentId,
        object : MediaBrowserCompat.SubscriptionCallback() {
          override fun onChildrenLoaded(
            parentId: String,
            children: MutableList<MediaBrowserCompat.MediaItem>
          ) {
            emitter.onNext(children)
          }

          override fun onChildrenLoaded(
            parentId: String,
            children: MutableList<MediaBrowserCompat.MediaItem>,
            options: Bundle
          ) {
            emitter.onNext(children)
          }

          override fun onError(parentId: String) {
            emitter.onError(Error())
          }

          override fun onError(parentId: String, options: Bundle) {
            emitter.onError(Error())
          }
        })
    }
      .doOnSubscribe { if (!mediaBrowser.isConnected) mediaBrowser.connect() }
      .doOnComplete { mediaBrowser.disconnect() }
      .takeUntil { !mediaBrowser.isConnected }
      .observeOn(AndroidSchedulers.mainThread())
}