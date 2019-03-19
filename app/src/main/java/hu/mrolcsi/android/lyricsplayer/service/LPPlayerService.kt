package hu.mrolcsi.android.lyricsplayer.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.AsyncTask
import android.os.CancellationSignal
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.TaskStackBuilder
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import hu.mrolcsi.android.lyricsplayer.database.playqueue.PlayQueueDatabase
import hu.mrolcsi.android.lyricsplayer.database.playqueue.entities.LastPlayed
import hu.mrolcsi.android.lyricsplayer.extensions.media.addQueueItems
import hu.mrolcsi.android.lyricsplayer.extensions.media.albumArt
import hu.mrolcsi.android.lyricsplayer.extensions.media.from
import hu.mrolcsi.android.lyricsplayer.player.PlayerActivity
import hu.mrolcsi.android.lyricsplayer.service.exoplayer.ExoPlayerHolder
import hu.mrolcsi.android.lyricsplayer.service.exoplayer.notification.ExoNotificationManager
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager

class LPPlayerService : LPBrowserService() {

  private val mBackgroundThread = HandlerThread("PlayerServiceHandler").apply { start() }
  private val mBackgroundHandler = Handler(mBackgroundThread.looper)

  // MediaSession and Player implementations
  private lateinit var mMediaSession: MediaSessionCompat
  private lateinit var mPlayerHolder: ExoPlayerHolder

  // Last played position
  private var mLastPlayed: LastPlayed? = null

  // ExoPlayer Notification
  private lateinit var mExoNotificationManager: ExoNotificationManager
  private var mIsForeground = false

  override fun onCreate() {
    super.onCreate()

    Log.i(LOG_TAG, "onCreate()")

    // Build a PendingIntent that can be used to launch the PlayerActivity.
    val playerActivityPendingIntent = TaskStackBuilder.create(this)
      // add all of DetailsActivity's parents to the stack,
      // followed by DetailsActivity itself
      .addNextIntentWithParentStack(Intent(this, PlayerActivity::class.java))
      .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

    // Create a MediaSessionCompat
    mMediaSession = MediaSessionCompat(this, LOG_TAG).apply {
      setSessionActivity(playerActivityPendingIntent)

      // Set the session's token so that client activities can communicate with it.
      setSessionToken(sessionToken)

      // Enable callbacks from MediaButtons and TransportControls
      setFlags(
        MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
            or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            or MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS
      )

      // Connect this session with the ExoPlayer
      mPlayerHolder = ExoPlayerHolder(applicationContext, this).also { exo ->
        mExoNotificationManager = ExoNotificationManager(
          applicationContext,
          this,
          exo.getPlayer(),
          object : PlayerNotificationManager.NotificationListener {

            override fun onNotificationPosted(notificationId: Int, notification: Notification?, ongoing: Boolean) {
              if (exo.getPlayer().playWhenReady && !mIsForeground) {
                startService(Intent(applicationContext, LPPlayerService::class.java))
                startForeground(notificationId, notification)
                mIsForeground = true
              }

              if (!exo.getPlayer().playWhenReady && mIsForeground) {
                stopForeground(false)
                mIsForeground = false
              }
            }

            override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
              stopForeground(true)
              stopSelf()
              mIsForeground = false
            }
          })

        exo.getPlayer().addListener(object : Player.EventListener {
          override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Log.v(LOG_TAG, "onPlayerStateChanged(playWhenReady=$playWhenReady, playbackState=$playbackState)")

            // Make notification dismissible
            if (!playWhenReady && mIsForeground) {
              stopForeground(false)
              mIsForeground = false
            }

            when (playbackState) {
              Player.STATE_READY -> {
                // Set player to last played settings
                mLastPlayed?.let { lastPlayed ->
                  Log.d(LOG_TAG, "Loaded 'Last Played' from database: $lastPlayed")

                  // Skip to last played song
                  controller.transportControls.skipToQueueItem(lastPlayed.queuePosition.toLong())

                  // Seek to saved position
                  controller.transportControls.seekTo(lastPlayed.trackPosition)

                  // Set mLastPlayed to null, so we won't call this again
                  mLastPlayed = null
                }
              }
            }
          }
        })
      }

      // Register basic callbacks
      controller.registerCallback(object : MediaControllerCompat.Callback() {

        // Last received metadata
        private var previousMetadata: MediaMetadataCompat? = null

        private var mLoaderRunnable: Runnable? = null
        private var mCancellationSignal: CancellationSignal? = null

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {}

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
          // Ignore nulls
          metadata?.let {
            Log.v(LOG_TAG, "onMetadataChanged(${metadata.description}, albumArt=${metadata.albumArt})")

            // Check if metadata has actually changed
            val mediaId = metadata.description?.mediaId
            val differentMediaId = mediaId != previousMetadata?.description?.mediaId

            previousMetadata = metadata

            when {
              differentMediaId || metadata.albumArt == null -> {
                mCancellationSignal?.cancel()
                mCancellationSignal = loadNewMetadata(metadata)
              }
              else -> {
                // update Theme
                ThemeManager.getInstance(applicationContext).updateFromBitmap(metadata.albumArt!!)
              }
            }
          }
        }

        private fun loadNewMetadata(source: MediaMetadataCompat): CancellationSignal {
          if (mLoaderRunnable != null) {
            mBackgroundHandler.removeCallbacks(mLoaderRunnable)
          }
          val cancellationSignal = CancellationSignal()
          mLoaderRunnable = getLoaderRunnable(source, cancellationSignal)
          mBackgroundHandler.post(mLoaderRunnable)
          return cancellationSignal
        }

        private fun getLoaderRunnable(source: MediaMetadataCompat, cancellationSignal: CancellationSignal): Runnable {
          return Runnable {
            Log.d(LOG_TAG, "Loading additional metadata for ${source.description.mediaId}")

            val newMetadata = MediaMetadataCompat.Builder(source).from(source.description).build()

            if (!cancellationSignal.isCanceled) {
              setMetadata(newMetadata)
            }
          }
        }

      }, mBackgroundHandler)

      AsyncTask.execute {
        // Get last played queue from the database
        val queue = PlayQueueDatabase.getInstance(applicationContext)
          .getPlayQueueDao()
          .getQueue()
          .map { it.createDescription() }

        Log.d(LOG_TAG, "Loaded queue from database: $queue")

        if (queue.isNotEmpty()) {
          // Load last played songs
          controller.addQueueItems(queue)

          // Get last played positions from the database
          mLastPlayed = PlayQueueDatabase.getInstance(applicationContext)
            .getPlayQueueDao()
            .getLastPlayed()
        }
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    MediaButtonReceiver.handleIntent(mMediaSession, intent)
    return Service.START_STICKY
  }

  override fun onDestroy() {
    // Avoid calling stop multiple times.

    Log.i(LOG_TAG, "onDestroy()")

    // Deactivate the session
    mMediaSession.run {
      isActive = false
      release()
    }

    // Detach the player from the notification
    mExoNotificationManager.release()

    // Release player and related resources
    mPlayerHolder.release()

    // Close database
    //PlayQueueDatabase.getInstance(applicationContext).close()
  }

  companion object {
    const val LOG_TAG = "LPPlayerService"
  }
}