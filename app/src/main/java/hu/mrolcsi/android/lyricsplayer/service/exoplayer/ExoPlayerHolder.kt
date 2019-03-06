package hu.mrolcsi.android.lyricsplayer.service.exoplayer

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.DefaultPlaybackController
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueEditor
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import hu.mrolcsi.android.lyricsplayer.BuildConfig
import hu.mrolcsi.android.lyricsplayer.extensions.media.from
import hu.mrolcsi.android.lyricsplayer.extensions.media.fullDescription
import hu.mrolcsi.android.lyricsplayer.extensions.media.id
import hu.mrolcsi.android.lyricsplayer.extensions.media.mediaPath
import hu.mrolcsi.android.lyricsplayer.extensions.media.mediaUri
import hu.mrolcsi.android.lyricsplayer.service.LastPlayedSetting
import java.io.File
import java.util.concurrent.Executors

class ExoPlayerHolder(context: Context, session: MediaSessionCompat) {

  private val mMainHandler = Handler(Looper.getMainLooper())

  //region -- PLAYBACK --

  private val mPlayer = ExoPlayerFactory.newSimpleInstance(
    context,
    AudioOnlyRenderersFactory(context),
    DefaultTrackSelector()
  ).apply {
    addListener(object : Player.EventListener {
      override fun onPositionDiscontinuity(reason: Int) {
        // Check against saved index if position changed
        val newIndex = this@apply.currentWindowIndex
        if (mLastPlayed.lastPlayedIndex != newIndex) {
          // Save new index to SharedPrefs
          mLastPlayed.lastPlayedIndex = newIndex
        }
      }
    })
    audioAttributes = AudioAttributes.Builder()
      .setUsage(C.USAGE_MEDIA)
      .setContentType(C.CONTENT_TYPE_MUSIC)
      .build()
  }

  private val mPlaybackPreparer = object : MediaSessionConnector.PlaybackPreparer {
    override fun getCommands(): Array<String>? = null

    override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?) {}

    override fun getSupportedPrepareActions(): Long = MediaSessionConnector.PlaybackPreparer.ACTIONS

    override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
      // Assuming mediaId is a path
      onPrepareFromUri(Uri.fromFile(File(mediaId)), extras)
    }

    override fun onPrepareFromUri(uri: Uri, extras: Bundle?) {
      Log.v(LOG_TAG, "onPrepareFromUri($uri) called from ${Thread.currentThread()}")

//      mExecutor.submit {
//        // Clear queue and load media
//        mQueue.clear()
//        mQueue.addMediaSource(
//          mQueueMediaSourceFactory.createMediaSource(
//            mQueueMediaSourceFactory.createMediaMetadata(uri).fullDescription
//          ),
//          mMainHandler
//        ) {
//          // Prepare queue when ready (gets called through handler passed as parameter above)
//          onPrepare()
//        }
//      }

      // Clear queue and load media
      mQueue.clear()
      mQueue.addMediaSource(
        mQueueMediaSourceFactory.createMediaSource(
          mQueueMediaSourceFactory.createMediaMetadata(uri).fullDescription
        )
      )
      onPrepare()
    }

    override fun onPrepareFromSearch(query: String?, extras: Bundle?) {}

    override fun onPrepare() {
      Log.v(LOG_TAG, "onPrepare() called from ${Thread.currentThread()}")
      mPlayer.prepare(mQueue)
    }
  }

  private val mUpdaterActionProvider = object : MediaSessionConnector.CustomActionProvider {
    override fun getCustomAction(): PlaybackStateCompat.CustomAction {
      return if (mProgressUpdater.isEnabled) {
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
        ACTION_START_UPDATER -> mProgressUpdater.startUpdater()
        ACTION_STOP_UPDATER -> mProgressUpdater.stopUpdater()
      }
    }
  }

  private val mClearQueueActionProvider = object : MediaSessionConnector.CustomActionProvider {
    override fun getCustomAction(): PlaybackStateCompat.CustomAction {
      return PlaybackStateCompat.CustomAction
        .Builder(ACTION_CLEAR_QUEUE, "Clear queue.", -1)
        .build()
    }

    override fun onCustomAction(action: String?, extras: Bundle?) {
      when (action) {
        ACTION_CLEAR_QUEUE ->
          session.controller.queue.forEach {
            mQueueEditor.onRemoveQueueItem(mPlayer, it.description)
          }
      }
    }
  }

  private val mPlaybackController = object : DefaultPlaybackController() {
    // override functions as needed

    override fun onPlay(player: Player?) {
      super.onPlay(player)

      // Start updater if it is enabled (Gets cancelled in onStop())
      if (mProgressUpdater.isEnabled) {
        mProgressUpdater.startUpdater()
      }
    }

    override fun onPause(player: Player) {
      super.onPause(player)

      mProgressUpdater.stopUpdater()

      // Save current position to SharedPrefs
      mLastPlayed.lastPlayedPosition = player.currentPosition
    }

    override fun onStop(player: Player) {
      super.onStop(player)

      // Rewind track
      player.seekTo(0)

      // Cancel Handler
      mProgressUpdater.stopUpdater()

      // Save current position to SharedPrefs
      mLastPlayed.lastPlayedPosition = player.currentPosition
    }
  }

  //endregion

  //region -- QUEUE --

  private val mExecutor = /*Executors.newCachedThreadPool()*/  Executors.newSingleThreadExecutor()

  private val mQueue = ConcatenatingMediaSource(
    false,
    true,
    ShuffleOrder.DefaultShuffleOrder(0)
  )

  private val mQueueNavigator = object : TimelineQueueNavigator(session, 50) {

    private val mWindow = Timeline.Window()

    override fun onSkipToQueueItem(player: Player?, id: Long) {
      Log.v(LOG_TAG, "onSkipToQueueItem($id) [QueueSize=${mQueue.size}] called from ${Thread.currentThread()}")
      super.onSkipToQueueItem(player, id)
    }

    override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
      return player.currentTimeline.getWindow(windowIndex, mWindow, true).tag as MediaDescriptionCompat
    }
  }

  private val mQueueDataAdapter = object : TimelineQueueEditor.QueueDataAdapter {
    override fun add(position: Int, description: MediaDescriptionCompat?) {
      Log.v(LOG_TAG, "mQueueAdapter.add($position, $description) called from ${Thread.currentThread()}")

      // Call prepare?
//      mMainHandler.post {
//        Log.v(LOG_TAG, "mPlayer.prepare(mQueue, false, false) called from ${Thread.currentThread()}")
//        mPlayer.prepare(mQueue, false, true)
//      }
    }

    override fun remove(position: Int) {}
    override fun move(from: Int, to: Int) {}
  }

  private val mQueueMediaSourceFactory = object : TimelineQueueEditor.MediaSourceFactory {

    private val mDataSourceFactory = DefaultDataSourceFactory(
      context, Util.getUserAgent(context, BuildConfig.APPLICATION_ID)
    )

    fun createMediaMetadata(uri: Uri): MediaMetadataCompat {
      //Log.v(LOG_TAG, "createMediaMetadata($uri) called from ${Thread.currentThread()}")

      // Load metadata
      val retriever = MediaMetadataRetriever().apply {
        setDataSource(context, uri)
      }
      return MediaMetadataCompat.Builder().from(retriever).apply {
        // Some additional info cannot be retrieved from the retriever.
        id = uri.path!!
        mediaUri = uri.toString()
      }.build()
    }

    override fun createMediaSource(description: MediaDescriptionCompat): MediaSource? {
      //Log.v(LOG_TAG, "createMediaSource($description) called from ${Thread.currentThread()}")

      // Create Metadata from Uri
      val uri = description.mediaUri ?: Uri.fromFile(File(description.mediaPath))
      //val metadata = createMediaMetadata(uri)

      // Create MediaSource from Metadata
      //return metadata.toMediaSource(dataSourceFactory)
      // NOTE: ExtractorMediaSource.Factory is not reusable!
      return ExtractorMediaSource.Factory(mDataSourceFactory).setTag(description).createMediaSource(uri)
    }
  }

  private val mQueueEditor = AsyncQueueEditor(
    TimelineQueueEditor(session.controller, mQueue, mQueueDataAdapter, mQueueMediaSourceFactory),
    mExecutor
  )

  //endregion

  // Player state polling
  private val mProgressUpdater = ProgressUpdater {
    // Update session with the current position
    val currentState = session.controller.playbackState
    val newState = PlaybackStateCompat.Builder(currentState)
      .setState(
        currentState.state,
        mPlayer.currentPosition,
        mPlayer.playbackParameters.speed
      ).build()
    session.setPlaybackState(newState)
  }

  // Last Played Settings
  private val mLastPlayed = LastPlayedSetting(context)

  // Connect this holder to the session
  private val mMediaSessionConnector =
    MediaSessionConnector(session, mPlaybackController).also {
      it.setPlayer(mPlayer, mPlaybackPreparer, mUpdaterActionProvider, mClearQueueActionProvider)
      it.setQueueNavigator(mQueueNavigator)
      it.setQueueEditor(mQueueEditor)
    }

  /**
   * Expose the inner player to the outside.
   */
  fun getPlayer(): Player = mPlayer

  fun release() {
    mMediaSessionConnector.setPlayer(null, null)

    // Release player
    mPlayer.release()
  }

  companion object {
    @SuppressWarnings("unused")
    private const val LOG_TAG = "PlayerHolder"

    const val ACTION_START_UPDATER = "ACTION_START_UPDATER"
    const val ACTION_STOP_UPDATER = "ACTION_STOP_UPDATER"
    const val ACTION_CLEAR_QUEUE = "ACTION_CLEAR_QUEUE"
  }

}