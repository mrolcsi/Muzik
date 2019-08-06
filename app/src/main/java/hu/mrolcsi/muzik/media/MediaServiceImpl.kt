package hu.mrolcsi.muzik.media

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.os.bundleOf
import hu.mrolcsi.muzik.service.MuzikPlayerService
import hu.mrolcsi.muzik.service.extensions.media.addQueueItems
import hu.mrolcsi.muzik.service.extensions.media.clearQueue
import hu.mrolcsi.muzik.service.extensions.media.playFromDescriptions
import hu.mrolcsi.muzik.service.extensions.media.setQueueTitle
import hu.mrolcsi.muzik.service.extensions.media.startProgressUpdater
import hu.mrolcsi.muzik.service.extensions.media.stopProgressUpdater
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaServiceImpl @Inject constructor(
  private val app: Application
) : MediaService {

  private val connectionCallbacks: MediaBrowserCompat.ConnectionCallback =
    object : MediaBrowserCompat.ConnectionCallback() {
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
          this@MediaServiceImpl.controller = controller

          // Set initial state, and metadata
          if (!playbackStateSubject.hasValue()) playbackStateSubject.onNext(controller.playbackState)
          if (!metadataSubject.hasValue()) metadataSubject.onNext(controller.metadata)
        }
      }
    }

  override val mediaBrowser = MediaBrowserCompat(
    app,
    ComponentName(app, MuzikPlayerService::class.java),
    connectionCallbacks,
    null // optional Bundle
  ).apply { connect() }

  private var lastMetadata: MediaMetadataCompat? = null

  private val metadataSubject = BehaviorSubject.create<MediaMetadataCompat>()
  private val playbackStateSubject = BehaviorSubject.create<PlaybackStateCompat>()

  override val metadata: Observable<MediaMetadataCompat> = metadataSubject.hide()
  override val playbackState: Observable<PlaybackStateCompat> = playbackStateSubject.hide()
  override var controller: MediaControllerCompat? = null

  override fun observableSubscribe(
    parentId: String,
    options: Bundle?
  ): Observable<List<MediaBrowserCompat.MediaItem>> =
    Observable.create<List<MediaBrowserCompat.MediaItem>> { emitter ->
      mediaBrowser.subscribe(
        parentId,
        options ?: bundleOf(),
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
            emitter.onError(Error("Error while getting items from MediaBrowser! (parentId = $parentId"))
          }

          override fun onError(parentId: String, options: Bundle) {
            emitter.onError(Error("Error while getting items from MediaBrowser! (parentId = $parentId, options = $options)"))
          }
        })
    }
      .takeWhile { mediaBrowser.isConnected }
      .observeOn(AndroidSchedulers.mainThread())

  override fun setQueueTitle(title: CharSequence) {
    controller?.setQueueTitle(title)
  }

  override fun playAll(descriptions: List<MediaDescriptionCompat>, startPosition: Int) {
    controller?.transportControls?.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
    controller?.playFromDescriptions(descriptions, startPosition)
  }

  override fun playAllShuffled(descriptions: List<MediaDescriptionCompat>) {
    controller?.clearQueue()
    controller?.transportControls?.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
    controller?.addQueueItems(descriptions)
    controller?.transportControls?.play()
  }

  override fun seekTo(position: Long) {
    controller?.transportControls?.seekTo(position)
  }

  override fun skipToPrevious() {
    controller?.transportControls?.skipToPrevious()
  }

  override fun playPause() {
    when (controller?.playbackState?.state) {
      PlaybackStateCompat.STATE_PLAYING -> {
        // Pause playback, stop updater
        controller?.transportControls?.pause()
        controller?.transportControls?.startProgressUpdater()
      }
      PlaybackStateCompat.STATE_PAUSED,
      PlaybackStateCompat.STATE_STOPPED -> {
        // Start playback, start updater
        controller?.transportControls?.play()
        controller?.transportControls?.stopProgressUpdater()
      }
    }
  }

  override fun skipToNext() {
    controller?.transportControls?.skipToNext()
  }
}