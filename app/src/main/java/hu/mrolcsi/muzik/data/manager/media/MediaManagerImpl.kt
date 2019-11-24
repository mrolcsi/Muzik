package hu.mrolcsi.muzik.data.manager.media

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ALL
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_INVALID
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_NONE
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ONE
import android.support.v4.media.session.PlaybackStateCompat.SHUFFLE_MODE_ALL
import android.support.v4.media.session.PlaybackStateCompat.SHUFFLE_MODE_INVALID
import android.support.v4.media.session.PlaybackStateCompat.SHUFFLE_MODE_NONE
import android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED
import android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
import android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED
import android.support.v4.media.session.PlaybackStateCompat.ShuffleMode
import android.util.Log
import androidx.core.os.bundleOf
import hu.mrolcsi.muzik.data.model.media.addQueueItems
import hu.mrolcsi.muzik.data.model.media.clearQueue
import hu.mrolcsi.muzik.data.model.media.playFromDescriptions
import hu.mrolcsi.muzik.data.model.media.setQueueTitle
import hu.mrolcsi.muzik.data.model.media.startProgressUpdater
import hu.mrolcsi.muzik.data.model.media.stopProgressUpdater
import hu.mrolcsi.muzik.data.service.media.MuzikPlayerService
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.koin.core.KoinComponent
import org.koin.core.inject

class MediaManagerImpl : MediaManager, KoinComponent {

  private val app: Application by inject()

  private val connectionCallbacks: MediaBrowserCompat.ConnectionCallback =
    object : MediaBrowserCompat.ConnectionCallback() {
      override fun onConnected() {
        mediaBrowser.sessionToken.also { token ->

          Log.v("MediaManager", "Connected to session: $token")

          // Attach a controller to this MediaSession
          val controller = MediaControllerCompat(app, token).apply {
            // Register callbacks to watch for changes
            registerCallback(object : MediaControllerCompat.Callback() {

              override fun onSessionReady() {
                Log.v("MediaManager", "Session ready. ")

                // Set initial state, and metadata
                if (!playbackStateSubject.hasValue()) playbackStateSubject.onNext(playbackState)
                if (!metadataSubject.hasValue()) metadataSubject.onNext(metadata)
                if (!repeatModeSubject.hasValue()) repeatModeSubject.onNext(repeatMode)
                if (!shuffleModeSubject.hasValue()) shuffleModeSubject.onNext(shuffleMode)
                if (!queueSubject.hasValue()) queue?.let { queueSubject.onNext(it) }
                if (!queueTitleSubject.hasValue()) queueTitle?.let { queueTitleSubject.onNext(it) }
              }

              override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                metadata?.let {
                  Log.v("MediaManager", "onMetadataChanged(${metadata.description})")

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
                playbackStateSubject.onNext(state)
              }

              override fun onRepeatModeChanged(repeatMode: Int) {
                repeatModeSubject.onNext(repeatMode)
              }

              override fun onShuffleModeChanged(shuffleMode: Int) {
                shuffleModeSubject.onNext(shuffleMode)
              }

              override fun onQueueChanged(queue: MutableList<QueueItem>?) {
                queue?.let { queueSubject.onNext(it) }
              }

              override fun onQueueTitleChanged(title: CharSequence?) {
                title?.let { queueTitleSubject.onNext(title) }
              }
            })
          }

          this@MediaManagerImpl.controller = controller
        }
      }
    }

  private val mediaBrowser = MediaBrowserCompat(
    app,
    ComponentName(app, MuzikPlayerService::class.java),
    connectionCallbacks,
    null // optional Bundle
  ).apply { connect() }

  private var lastMetadata: MediaMetadataCompat? = null

  private val metadataSubject = BehaviorSubject.create<MediaMetadataCompat>()
  private val playbackStateSubject = BehaviorSubject.create<PlaybackStateCompat>()
  private val repeatModeSubject = BehaviorSubject.create<@ShuffleMode Int>()
  private val shuffleModeSubject = BehaviorSubject.create<@ShuffleMode Int>()
  private val queueSubject = BehaviorSubject.create<List<QueueItem>>()
  private val queueTitleSubject = BehaviorSubject.create<CharSequence>()

  override val mediaMetadata: Observable<MediaMetadataCompat> = metadataSubject.hide()
  override val playbackState: Observable<PlaybackStateCompat> = playbackStateSubject.hide()
  override val repeatMode: Observable<Int> = repeatModeSubject.hide()
  override val shuffleMode: Observable<Int> = shuffleModeSubject.hide()
  override val queue: Observable<List<QueueItem>> = queueSubject.hide()
  override val queueTitle: Observable<CharSequence> = queueTitleSubject.hide()

  private var controller: MediaControllerCompat? = null

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
      .subscribeOn(Schedulers.single())
      .takeWhile { mediaBrowser.isConnected }

  override fun getCurrentPlaybackState() = controller?.playbackState ?: playbackStateSubject.value

  override fun getCurrentMediaMetadata() = controller?.metadata ?: metadataSubject.value!!

  override fun setQueueTitle(title: CharSequence) {
    controller?.setQueueTitle(title)
  }

  override fun playAll(descriptions: List<MediaDescriptionCompat>, startPosition: Int) {
    controller?.transportControls?.setShuffleMode(SHUFFLE_MODE_NONE)
    controller?.playFromDescriptions(descriptions, startPosition)
  }

  override fun playAllShuffled(descriptions: List<MediaDescriptionCompat>) {
    controller?.clearQueue()
    controller?.transportControls?.setShuffleMode(SHUFFLE_MODE_ALL)
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
      STATE_PLAYING -> {
        // Pause playback, stop updater
        controller?.transportControls?.pause()
        controller?.transportControls?.stopProgressUpdater()
      }
      STATE_PAUSED,
      STATE_STOPPED -> {
        // Start playback, start updater
        controller?.transportControls?.play()
        controller?.transportControls?.startProgressUpdater()
      }
    }
  }

  override fun skipToNext() {
    controller?.transportControls?.skipToNext()
  }

  override fun rewind() {
    controller?.playbackState?.let {
      controller?.transportControls?.seekTo(it.position - 5000)
    }
  }

  override fun fastForward() {
    controller?.playbackState?.let {
      controller?.transportControls?.seekTo(it.position + 5000)
    }
  }

  override fun getShuffleMode(): Int = controller?.shuffleMode ?: SHUFFLE_MODE_INVALID

  override fun setShuffleMode(shuffleMode: Int) {
    controller?.transportControls?.setShuffleMode(shuffleMode)
  }

  override fun toggleShuffle() {
    controller?.let {
      when (it.shuffleMode) {
        SHUFFLE_MODE_NONE ->
          it.transportControls.setShuffleMode(SHUFFLE_MODE_ALL)
        else ->
          it.transportControls.setShuffleMode(SHUFFLE_MODE_NONE)
      }
    }
  }

  override fun getRepeatMode() = controller?.repeatMode ?: REPEAT_MODE_INVALID

  override fun setRepeatMode(repeatMode: Int) {
    controller?.transportControls?.setRepeatMode(repeatMode)
  }

  override fun toggleRepeat() {
    controller?.let {
      when (it.repeatMode) {
        REPEAT_MODE_NONE ->
          it.transportControls.setRepeatMode(REPEAT_MODE_ONE)
        REPEAT_MODE_ONE ->
          it.transportControls.setRepeatMode(REPEAT_MODE_ALL)
        else ->
          it.transportControls.setRepeatMode(REPEAT_MODE_NONE)
      }
    }
  }

  override fun getActiveQueueItemId(): Long =
    controller?.playbackState?.activeQueueItemId ?: QueueItem.UNKNOWN_ID.toLong()

  override fun skipToQueueItem(id: Long) {
    controller?.transportControls?.skipToQueueItem(id)
  }
}