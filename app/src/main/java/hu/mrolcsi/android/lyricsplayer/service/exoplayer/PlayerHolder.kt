package hu.mrolcsi.android.lyricsplayer.service.exoplayer

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.ext.mediasession.DefaultPlaybackController
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueEditor
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import hu.mrolcsi.android.lyricsplayer.BuildConfig
import hu.mrolcsi.android.lyricsplayer.extensions.media.from
import hu.mrolcsi.android.lyricsplayer.extensions.media.id
import hu.mrolcsi.android.lyricsplayer.extensions.media.mediaPath
import hu.mrolcsi.android.lyricsplayer.extensions.media.mediaUri
import hu.mrolcsi.android.lyricsplayer.extensions.media.toMediaSource
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class PlayerHolder(context: Context, session: MediaSessionCompat) {

  private val mPlayer = ExoPlayerFactory.newSimpleInstance(
    context,
    AudioOnlyRenderersFactory(context),
    DefaultTrackSelector()
  )

  private val mQueue = ConcatenatingMediaSource()

  private val mPlaybackPreparer = object : MediaSessionConnector.PlaybackPreparer {
    override fun getCommands(): Array<String>? = null

    override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?) {}

    override fun getSupportedPrepareActions(): Long = MediaSessionConnector.PlaybackPreparer.ACTIONS

    override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
      // Assuming mediaId is a path
      onPrepareFromUri(Uri.fromFile(File(mediaId)), extras)
    }

    override fun onPrepareFromUri(uri: Uri, extras: Bundle?) {
      // Reset media player before loading
      Log.v(LOG_TAG, "Loading media into Player: $uri")

      // Load metadata
      val retriever = MediaMetadataRetriever().apply {
        setDataSource(context, uri)
      }
      val metadata = MediaMetadataCompat.Builder().from(retriever).apply {
        // Some additional info cannot be retrieved from the retriever.
        id = uri.path!!
        mediaUri = uri.toString()
      }.build()

      // Create MediaSource from description
      val mediaSource = metadata.toMediaSource(
        DefaultDataSourceFactory(
          context,
          Util.getUserAgent(context, BuildConfig.APPLICATION_ID)
        )
      )

      // clear queue and load media
      mQueue.clear()
      mQueue.addMediaSource(mediaSource)
      mPlayer.prepare(mQueue)
    }

    override fun onPrepareFromSearch(query: String?, extras: Bundle?) {}

    override fun onPrepare() {}
  }

  private val mUpdaterActionProvider = object : MediaSessionConnector.CustomActionProvider {
    override fun getCustomAction(): PlaybackStateCompat.CustomAction {
      return if (mUpdaterEnabled.get()) {
        PlaybackStateCompat.CustomAction
          .Builder(ACTION_STOP_UPDATER, "Stop progress updater.", -1)
          .build()
      } else {
        PlaybackStateCompat.CustomAction
          .Builder(ACTION_START_UPDATER, "Start progress updater.", -1)
          .build()
      }
    }

    override fun onCustomAction(action: String?, extras: Bundle?) {
      when (action) {
        ACTION_START_UPDATER -> if (!mUpdaterEnabled.getAndSet(true)) mUpdateHandler.post(mUpdateRunnable)
        ACTION_STOP_UPDATER -> mUpdaterEnabled.set(false)
      }
    }
  }

  private val mPlaybackController = object : DefaultPlaybackController() {
    // override functions as needed

    override fun onPlay(player: Player?) {
      super.onPlay(player)

      // Start updater if it is enabled (Gets cancelled in onStop())
      if (mUpdaterEnabled.get()) {
        mUpdateHandler.post(mUpdateRunnable)
      }
    }

    override fun onStop(player: Player?) {
      super.onStop(player)

      // Rewind track
      mPlayer.seekTo(0)

      // Cancel Handler
      mUpdateHandler.removeCallbacks(mUpdateRunnable)
    }
  }

  private val mQueueNavigator = object : TimelineQueueNavigator(session) {
    private val mWindow = Timeline.Window()
    override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
      return player.currentTimeline.getWindow(windowIndex, mWindow, true).tag as MediaDescriptionCompat
    }
  }

  private val mQueueDataAdapter = object : TimelineQueueEditor.QueueDataAdapter {
    override fun add(position: Int, description: MediaDescriptionCompat?) {
      val source = mMediaSourceFactory.createMediaSource(description)
      mQueue.addMediaSource(position, source)
    }

    override fun remove(position: Int) {
      mQueue.removeMediaSource(position)
    }

    override fun move(from: Int, to: Int) {
      mQueue.moveMediaSource(from, to)
    }
  }

  private val mMediaSourceFactory = TimelineQueueEditor.MediaSourceFactory { description ->
    // Return a MediaSource from the MediaDescription
    val uri = description.mediaUri ?: Uri.fromFile(File(description.mediaPath))
    val dataSourceFactory = DefaultDataSourceFactory(context, Util.getUserAgent(context, BuildConfig.APPLICATION_ID))
    ExtractorMediaSource.Factory(dataSourceFactory).setTag(description).createMediaSource(uri)
  }

  // Player state polling
  private val mUpdaterEnabled = AtomicBoolean(false)
  private val mUpdateHandler = Handler()
  private val mUpdateRunnable = object : Runnable {
    override fun run() {
      // Update session with the current position
      val currentState = session.controller.playbackState
      val newState = PlaybackStateCompat.Builder(currentState)
        .setState(
          currentState.state,
          mPlayer.currentPosition,
          mPlayer.playbackParameters.speed
        ).build()
      session.setPlaybackState(newState)

      // If updater is enabled, schedule next 'tick'
      if (mUpdaterEnabled.get()) {
        mUpdateHandler.postDelayed(this, UPDATE_FREQUENCY)
      }
    }
  }

  init {
    // Connect this holder to the session
    MediaSessionConnector(session, mPlaybackController).also {
      it.setPlayer(mPlayer, mPlaybackPreparer, mUpdaterActionProvider)
      it.setQueueNavigator(mQueueNavigator)
      it.setQueueEditor(
        TimelineQueueEditor(
          session.controller,
          mQueue,
          mQueueDataAdapter,
          mMediaSourceFactory
        )
      )
    }
  }

  companion object {
    @SuppressWarnings("unused")
    private const val LOG_TAG = "PlayerHolder"

    private const val UPDATE_FREQUENCY: Long = 500  // in milliseconds

    const val ACTION_START_UPDATER = "ACTION_START_UPDATER"
    const val ACTION_STOP_UPDATER = "ACTION_STOP_UPDATER"
  }

}