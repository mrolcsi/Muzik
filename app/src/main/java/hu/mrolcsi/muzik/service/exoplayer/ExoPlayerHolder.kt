package hu.mrolcsi.muzik.service.exoplayer

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.ResultReceiver
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.util.Pair
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.DefaultControlDispatcher
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlaybackException.TYPE_OUT_OF_MEMORY
import com.google.android.exoplayer2.ExoPlaybackException.TYPE_REMOTE
import com.google.android.exoplayer2.ExoPlaybackException.TYPE_RENDERER
import com.google.android.exoplayer2.ExoPlaybackException.TYPE_SOURCE
import com.google.android.exoplayer2.ExoPlaybackException.TYPE_UNEXPECTED
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueEditor
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.google.android.exoplayer2.util.Util
import hu.mrolcsi.muzik.BuildConfig
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.database.playqueue.PlayQueueDatabase
import hu.mrolcsi.muzik.database.playqueue.entities.LastPlayed
import hu.mrolcsi.muzik.database.playqueue.entities.PlayQueueEntry
import hu.mrolcsi.muzik.service.BecomingNoisyReceiver
import hu.mrolcsi.muzik.service.extensions.database.fromDescription
import hu.mrolcsi.muzik.service.extensions.media.mediaPath
import java.io.File
import java.util.concurrent.Executors
import kotlin.random.Random

class ExoPlayerHolder(private val context: Context, session: MediaSessionCompat) {

  //region  -- HANDLERS & THREADS --

  private val mMainHandler = Handler(Looper.getMainLooper())
  private val mWorkerThread = HandlerThread("PlayerWorker").apply { start() }
  private val mBackgroundHandler = Handler(mWorkerThread.looper)
  private val mDatabaseWorker = Executors.newSingleThreadExecutor()

  //endregion

  //region -- PLAYBACK CONTROLLER --

  private val mPlaybackController = object : DefaultControlDispatcher() {
    override fun dispatchSetPlayWhenReady(player: Player, playWhenReady: Boolean): Boolean {
      if (playWhenReady) {
        Log.v(LOG_TAG, "onPlay()")

        // Prepare player if needed (like after an error)
        if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
          mPlaybackPreparer.onPrepare()
        }

        // Start updater if it is enabled (Gets cancelled in onStop())
        if (mProgressUpdater.isEnabled) {
          mProgressUpdater.startUpdater()
        }
      } else {
        Log.v(LOG_TAG, "onPause()")

        // Save current position
        mLastPlayed.queuePosition = mPlayer.currentWindowIndex
        mLastPlayed.trackPosition = player.currentPosition

        mDatabaseWorker.submit {
          PlayQueueDatabase.getInstance(context)
            .getPlayQueueDao()
            .saveLastPlayed(mLastPlayed)
        }

        mProgressUpdater.stopUpdater()
      }

      player.playWhenReady = playWhenReady
      return true
    }

    override fun dispatchStop(player: Player, reset: Boolean): Boolean {
      Log.v(LOG_TAG, "onStop()")

      // Save Last Played settings (before stopping)
      mLastPlayed.queuePosition = mPlayer.currentWindowIndex
      mLastPlayed.trackPosition = mPlayer.currentPosition

      // Stop the player
      player.stop()

      mDatabaseWorker.submit {
        PlayQueueDatabase.getInstance(context)
          .getPlayQueueDao()
          .saveLastPlayed(mLastPlayed)
      }

      // Rewind track
      player.seekTo(0)

      // Cancel Handler
      mProgressUpdater.stopUpdater()

      return true
    }

    override fun dispatchSetShuffleModeEnabled(player: Player?, shuffleModeEnabled: Boolean): Boolean {
      Log.v(LOG_TAG, "setShuffleMode($shuffleModeEnabled, (dataSourceSize=${mQueueDataSource.size}))")

      // Use a new ShuffleOrder with a random seed
      mShuffleSeed = Random.nextLong()
      mQueueDataSource.setShuffleOrder(ShuffleOrder.DefaultShuffleOrder(mQueueDataSource.size, mShuffleSeed))

      return super.dispatchSetShuffleModeEnabled(player, shuffleModeEnabled)
    }

    fun dispatchSetShuffleModeEnabled(player: Player?, shuffleModeEnabled: Boolean, seed: Long): Boolean {
      Log.v(LOG_TAG, "setShuffleMode($shuffleModeEnabled, $seed, (dataSourceSize=${mQueueDataSource.size}))")

      // Use a new ShuffleOrder with supplied seed
      mShuffleSeed = seed
      mQueueDataSource.setShuffleOrder(ShuffleOrder.DefaultShuffleOrder(mQueueDataSource.size, mShuffleSeed))

      return super.dispatchSetShuffleModeEnabled(player, shuffleModeEnabled)
    }
  }

  //endregion

  //region -- PLAYER --

  private val mBecomingNoisyReceiver = BecomingNoisyReceiver(context, session.sessionToken)

  private val mPlayer = ExoPlayerFactory.newSimpleInstance(
    context,
    AudioOnlyRenderersFactory(context),
    DefaultTrackSelector()
  ).apply {
    setAudioAttributes(
      AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .build(),
      true
    )
    addListener(object : Player.EventListener {
      override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        // Register/unregister BecomingNoisyReceiver
        if (playbackState == Player.STATE_READY && playWhenReady) {
          mBecomingNoisyReceiver.register()
        } else {
          mBecomingNoisyReceiver.unregister()
        }
      }

      override fun onPositionDiscontinuity(reason: Int) {
        // Check against saved index if position changed
        val newIndex = this@apply.currentWindowIndex

        // Save current position
        mLastPlayed.queuePosition = newIndex
        mLastPlayed.trackPosition = 0

        mDatabaseWorker.submit {
          PlayQueueDatabase.getInstance(context)
            .getPlayQueueDao()
            .saveLastPlayed(mLastPlayed)
        }
      }

      override fun onRepeatModeChanged(repeatMode: Int) {
        Log.v(LOG_TAG, "onRepeatModeChanged($repeatMode)")

        mLastPlayed.repeatMode = repeatMode
        mDatabaseWorker.submit {
          PlayQueueDatabase.getInstance(context)
            .getPlayQueueDao()
            .saveLastPlayed(mLastPlayed)
        }
      }

      override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        Log.v(LOG_TAG, "onShuffleEnabledChanged($shuffleModeEnabled)")

        mLastPlayed.shuffleMode = if (shuffleModeEnabled) 1 else 0
        mLastPlayed.shuffleSeed = mShuffleSeed
        mDatabaseWorker.submit {
          PlayQueueDatabase.getInstance(context)
            .getPlayQueueDao()
            .saveLastPlayed(mLastPlayed)
        }
      }
    })
  }

  //endregion

  //region -- PLAYBACK PREPARER

  private var mDesiredQueuePosition: Int = -1

  private val mPlaybackPreparer = object : MediaSessionConnector.PlaybackPreparer {
    override fun onCommand(
      player: Player?,
      controlDispatcher: ControlDispatcher?,
      command: String?,
      extras: Bundle?,
      cb: ResultReceiver?
    ): Boolean {
      return false
    }

    override fun getSupportedPrepareActions(): Long = MediaSessionConnector.PlaybackPreparer.ACTIONS

    override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
      // Assuming mediaId is a path
      onPrepareFromUri(Uri.fromFile(File(mediaId)), extras)
    }

    override fun onPrepareFromUri(uri: Uri, extras: Bundle?) {
      Log.v(LOG_TAG, "onPrepareFromUri($uri, $extras) called from ${Thread.currentThread()}")

      mBackgroundHandler.post {
        // Get the desired position of the item to be moved to.
        mDesiredQueuePosition = extras?.getInt(EXTRA_DESIRED_QUEUE_POSITION, -1) ?: -1

        // Clear queue and load media
        mQueueEditor.clearQueue()

        val mediaSource = ProgressiveMediaSource.Factory(mDataSourceFactory).createMediaSource(uri)
        mQueueDataSource.addMediaSource(0, mediaSource, mMainHandler) {
          // Call prepare() on the main thread
          onPrepare()
        }
      }
    }

    override fun onPrepareFromSearch(query: String?, extras: Bundle?) {}

    fun onPrepareFromDescription(description: MediaDescriptionCompat, extras: Bundle?) {
      mBackgroundHandler.post {
        // Clear Queue
        mQueueEditor.clearQueue()

        // Get the desired position of the item to be moved to.
        mDesiredQueuePosition = extras?.getInt(EXTRA_DESIRED_QUEUE_POSITION, -1) ?: -1

        // Add Description to Queue
        mQueueEditor.onAddQueueItem(mPlayer, description, 0)

        mMainHandler.post {
          // Start playback when ready
          if (extras?.getBoolean(ACTION_PLAY_FROM_DESCRIPTION, false) == true) {
            mPlayer.playWhenReady = true
          }
        }
      }
    }

    override fun onPrepare() {
      Log.v(LOG_TAG, "onPrepare() called from ${Thread.currentThread()}")
      mPlayer.prepare(mQueueDataSource, false, true)
    }
  }

  //endregion

  //region -- CUSTOM ACTIONS

  private val mUpdaterActionProvider = object : MediaSessionConnector.CustomActionProvider {
    override fun getCustomAction(player: Player?): PlaybackStateCompat.CustomAction {
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

    override fun onCustomAction(
      player: Player?,
      controlDispatcher: ControlDispatcher?,
      action: String?,
      extras: Bundle?
    ) {
      when (action) {
        ACTION_START_UPDATER -> mProgressUpdater.startUpdater()
        ACTION_STOP_UPDATER -> mProgressUpdater.stopUpdater()
      }
    }
  }

  private val mPrepareFromDescriptionActionProvider = object : MediaSessionConnector.CustomActionProvider {
    override fun getCustomAction(player: Player?) = PlaybackStateCompat.CustomAction
      .Builder(ACTION_PREPARE_FROM_DESCRIPTION, "Prepare from MediaDescriptionCompat", -1)
      .build()

    override fun onCustomAction(
      player: Player?,
      controlDispatcher: ControlDispatcher?,
      action: String?,
      extras: Bundle?
    ) {
      if (action == ACTION_PREPARE_FROM_DESCRIPTION) {
        val description = extras!!.getParcelable<MediaDescriptionCompat>(ARGUMENT_DESCRIPTION)!!
        mPlaybackPreparer.onPrepareFromDescription(description, extras)
      }
    }
  }

  private val mSetShuffleModeActionProvider = object : MediaSessionConnector.CustomActionProvider {
    override fun getCustomAction(player: Player?) = PlaybackStateCompat.CustomAction
      .Builder(ACTION_SET_SHUFFLE_MODE, "Set Shuffle mode.", -1)
      .build()

    override fun onCustomAction(
      player: Player?,
      controlDispatcher: ControlDispatcher?,
      action: String?,
      extras: Bundle?
    ) {
      val shuffleMode = extras?.getInt(EXTRA_SHUFFLE_MODE)
      val shuffleSeed = extras?.getLong(EXTRA_SHUFFLE_SEED)

      mPlaybackController.dispatchSetShuffleModeEnabled(
        player,
        shuffleMode != PlaybackStateCompat.SHUFFLE_MODE_NONE,
        shuffleSeed ?: Random.nextLong()
      )
    }

  }

  //endregion

  //region -- METADATA PROVIDER --

  private val mMetadataProvider: ExoMetadataProvider = ExoMetadataProvider(
    mediaController = session.controller,
    placeholderAlbumArt = BitmapFactory.decodeResource(context.resources, R.drawable.placeholder_cover_art)
  ) {
    // Call invalidate on session when the metadata cache was updated.
    mSessionConnector.invalidateMediaSessionMetadata()
  }

  //endregion

  //region -- QUEUE NAVIGATOR --

  private val mQueueNavigator = object : TimelineQueueNavigator(session) {

    private val mWindow = Timeline.Window()

    override fun onSkipToQueueItem(player: Player?, controlDispatcher: ControlDispatcher?, id: Long) {
      Log.v(
        LOG_TAG,
        "onSkipToQueueItem($id) [QueueSize=${mQueueDataSource.size}] called from ${Thread.currentThread()}"
      )
      super.onSkipToQueueItem(player, controlDispatcher, id)
    }

    override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
      return player.currentTimeline.getWindow(windowIndex, mWindow, true).tag as MediaDescriptionCompat
    }
  }

  //endregion

  //region -- QUEUE EDITOR --

  private var mShuffleSeed = Random.nextLong()

  private val mQueueDataSource = ConcatenatingMediaSource(
    false,
    true,
    ShuffleOrder.DefaultShuffleOrder(0, mShuffleSeed)
  )

  private val mOnQueueChangedCallback: BulkTimelineQueueEditor.OnQueueChangedCallback =
    object : BulkTimelineQueueEditor.OnQueueChangedCallback {
      override fun onItemAdded(position: Int, description: MediaDescriptionCompat) {
        Log.v(LOG_TAG, "onItemAdded($position, $description) called from ${Thread.currentThread()}")

        // Reset Shuffle Order
        mQueueDataSource.setShuffleOrder(
          ShuffleOrder.DefaultShuffleOrder(
            mQueueDataSource.size,
            mShuffleSeed
          )
        )

        // Prepare player if needed
        mMainHandler.post {
          val playerState = mPlayer.playbackState
          if (playerState == Player.STATE_IDLE || playerState == Player.STATE_ENDED) {
            mPlaybackPreparer.onPrepare()
          }
        }

        // Save queue to Database
        mDatabaseWorker.submit {
          PlayQueueDatabase.getInstance(context)
            .getPlayQueueDao()
            .insertEntries(PlayQueueEntry.fromDescription(position, description))
        }
      }

      override fun onItemsAdded(position: Int, descriptions: Collection<MediaDescriptionCompat>) {
        Log.v(
          LOG_TAG,
          "onItemsAdded($position, [${descriptions.size} items]) called from ${Thread.currentThread()}"
        )

        // After all the other songs were added to the queue, move the first song to it's proper position.
        if (mDesiredQueuePosition in 0..mQueueDataSource.size) {
          mQueueEditor.onMoveQueueItem(mPlayer, 0, mDesiredQueuePosition)
          mDesiredQueuePosition = -1
        }

        // Reset Shuffle Order
        mQueueDataSource.setShuffleOrder(
          ShuffleOrder.DefaultShuffleOrder(
            mQueueDataSource.size,
            mShuffleSeed
          )
        )

        // Prepare player if needed
        mMainHandler.post {
          val playerState = mPlayer.playbackState
          if (playerState == Player.STATE_IDLE || playerState == Player.STATE_ENDED) {
            mPlaybackPreparer.onPrepare()
          }
        }

        // Save queue to Database
        mDatabaseWorker.submit {
          val queue = descriptions.mapIndexed { index, description ->
            PlayQueueEntry.fromDescription(position + index, description)
          }
          PlayQueueDatabase.getInstance(context)
            .getPlayQueueDao()
            .insertEntries(*queue.toTypedArray())
        }
      }

      override fun onItemRemoved(position: Int) {
        mDatabaseWorker.submit {
          PlayQueueDatabase.getInstance(context)
            .getPlayQueueDao()
            .removeEntry(position.toLong())
        }
      }

      override fun onItemsRemoved(from: Int, to: Int) {
        mDatabaseWorker.submit {
          PlayQueueDatabase.getInstance(context)
            .getPlayQueueDao()
            .removeEntriesInRange(from, to)
        }
      }

      override fun onItemMoved(from: Int, to: Int) {
        Log.v(LOG_TAG, "onItemsMoved($from -> $to) called from ${Thread.currentThread()}")

        mDatabaseWorker.submit {
          val queue = emptyList<PlayQueueEntry>().toMutableList()
          for (i in 0 until mQueueDataSource.size) {
            queue.add(
              PlayQueueEntry.fromDescription(
                i,
                mQueueDataSource.getMediaSource(i).tag as MediaDescriptionCompat
              )
            )
          }

          PlayQueueDatabase.getInstance(context)
            .getPlayQueueDao()
            .insertEntries(*queue.toTypedArray())
        }
      }

      override fun onQueueCleared() {
        Log.v(LOG_TAG, "onQueueCleared()")
        mDatabaseWorker.submit {
          PlayQueueDatabase.getInstance(context)
            .getPlayQueueDao()
            .clearQueue()
        }
      }
    }

  private val mDataSourceFactory = DefaultDataSourceFactory(
    context, Util.getUserAgent(context, BuildConfig.APPLICATION_ID)
  )

  private val mQueueMediaSourceFactory = object : TimelineQueueEditor.MediaSourceFactory {

    override fun createMediaSource(description: MediaDescriptionCompat): MediaSource? {
      Log.v(LOG_TAG, "createMediaSource($description) called from ${Thread.currentThread()}")

      // Create Metadata from Uri
      val uri = description.mediaUri ?: Uri.fromFile(File(description.mediaPath))

      // NOTE: ExtractorMediaSource.Factory is not reusable!

      // Create MediaSource from Metadata
      return ProgressiveMediaSource.Factory(mDataSourceFactory).setTag(description).createMediaSource(uri)
    }
  }

  private val mQueueEditor = BulkTimelineQueueEditor(
    session.controller,
    mQueueDataSource,
    mOnQueueChangedCallback,
    mQueueMediaSourceFactory,
    mBackgroundHandler
  )

  //endregion

  //region ERROR MESSAGE PROVIDER

  private val mErrorMessageProvider = ErrorMessageProvider<ExoPlaybackException> { exception ->
    when (exception.type) {
      TYPE_SOURCE -> Pair.create(exception.type, context.getString(R.string.error_source))
      TYPE_RENDERER -> Pair.create(exception.type, context.getString(R.string.error_renderer))
      TYPE_OUT_OF_MEMORY -> Pair.create(exception.type, context.getString(R.string.error_outOfMemory))
      TYPE_UNEXPECTED, TYPE_REMOTE -> Pair.create(exception.type, context.getString(R.string.error_unexpected))
      else -> Pair.create(exception.type, context.getString(R.string.error_unexpected))
    }
  }

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
  private val mLastPlayed = LastPlayed()

  // Connect this holder to the session
  private val mSessionConnector =
    MediaSessionConnector(session).apply {
      setPlayer(mPlayer)
      setMediaMetadataProvider(mMetadataProvider)
      setPlaybackPreparer(mPlaybackPreparer)
      setCustomActionProviders(
        mUpdaterActionProvider,
        mPrepareFromDescriptionActionProvider,
        mSetShuffleModeActionProvider
      )
      setControlDispatcher(mPlaybackController)
      setQueueNavigator(mQueueNavigator)
      setQueueEditor(mQueueEditor)
      setErrorMessageProvider(mErrorMessageProvider)
    }

  /**
   * Expose the inner player to the outside.
   */
  fun getPlayer(): Player = mPlayer

  fun release() {
    Log.d(LOG_TAG, "Releasing ExoPlayer and related...")

    mSessionConnector.setPlayer(null)

    // Release player
    mPlayer.release()

    // Stop background threads
    mWorkerThread.quitSafely()
    mDatabaseWorker.shutdown()  // or awaitTermination()?
  }

  companion object {
    @SuppressWarnings("unused")
    private const val LOG_TAG = "PlayerHolder"

    const val ACTION_START_UPDATER = "ACTION_START_UPDATER"
    const val ACTION_STOP_UPDATER = "ACTION_STOP_UPDATER"

    const val ACTION_PREPARE_FROM_DESCRIPTION = "ACTION_PREPARE_FROM_DESCRIPTION"
    const val ACTION_PLAY_FROM_DESCRIPTION = "ACTION_PLAY_FROM_DESCRIPTION"
    const val ARGUMENT_DESCRIPTION = "ARGUMENT_DESCRIPTION"

    const val EXTRA_DESIRED_QUEUE_POSITION = "EXTRA_DESIRED_QUEUE_POSITION"

    const val ACTION_SET_SHUFFLE_MODE = "ACTION_SET_SHUFFLE_MODE"
    const val EXTRA_SHUFFLE_MODE = "EXTRA_SHUFFLE_MODE"
    const val EXTRA_SHUFFLE_SEED = "EXTRA_SHUFFLE_SEED"
  }

}