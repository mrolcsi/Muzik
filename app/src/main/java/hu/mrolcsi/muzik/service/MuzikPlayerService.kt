package hu.mrolcsi.muzik.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.TaskStackBuilder
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import hu.mrolcsi.muzik.database.playqueue.PlayQueueDatabase
import hu.mrolcsi.muzik.database.playqueue.entities.LastPlayed
import hu.mrolcsi.muzik.service.exoplayer.ExoPlayerHolder
import hu.mrolcsi.muzik.service.exoplayer.notification.ExoNotificationManager
import hu.mrolcsi.muzik.service.extensions.media.albumArt
import hu.mrolcsi.muzik.service.extensions.media.isSkipToNextEnabled
import hu.mrolcsi.muzik.service.extensions.media.prepareFromDescriptions
import hu.mrolcsi.muzik.service.extensions.media.setShuffleMode
import hu.mrolcsi.muzik.service.theme.ThemeManager

class MuzikPlayerService : MuzikBrowserService() {

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

    // Create a MediaSessionCompat
    mMediaSession = MediaSessionCompat(this, LOG_TAG).apply {
      val playerActivityPendingIntent = TaskStackBuilder.create(this@MuzikPlayerService)
        .addNextIntent(Intent(ACTION_OPEN_PLAYER))
        .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
      setSessionActivity(playerActivityPendingIntent)

      // Set the session's token so that client activities can communicate with it.
      setSessionToken(sessionToken)

      // Enable callbacks from MediaButtons and TransportControls
      setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)

      // Connect this session with the ExoPlayer
      mPlayerHolder = ExoPlayerHolder(applicationContext, this).also { exo ->
        mExoNotificationManager = ExoNotificationManager(
          applicationContext,
          this,
          exo.getPlayer(),
          object : PlayerNotificationManager.NotificationListener {

            override fun onNotificationPosted(notificationId: Int, notification: Notification?, ongoing: Boolean) {
              if (exo.getPlayer().playWhenReady && !mIsForeground) {
                startService(Intent(applicationContext, MuzikPlayerService::class.java))
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

          override fun onPlayerError(error: ExoPlaybackException?) {
            val player = exo.getPlayer()

            if (controller.playbackState.isSkipToNextEnabled && player.playWhenReady) {
              // Skip to next track
              controller.transportControls.skipToNext()
              controller.transportControls.prepare()
              controller.transportControls.play()
            } else {
              // Send error to client
            }
          }
        })
      }

      // Register basic callbacks
      controller.registerCallback(object : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {}

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
          metadata?.albumArt?.let {
            ThemeManager.getInstance(applicationContext).updateFromBitmap(it)
          }
        }
      })

      // Check permissions before proceeding
      if (ContextCompat.checkSelfPermission(
          applicationContext,
          Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
      ) {
        AsyncTask.execute {
          // Get last played queue from the database
          val queue = PlayQueueDatabase.getInstance(applicationContext)
            .getPlayQueueDao()
            .getQueue()
            .map { it.toDescription() }

          Log.d(LOG_TAG, "Loaded queue from database: $queue")

          if (queue.isNotEmpty()) {
            // Get last played positions from the database
            mLastPlayed = PlayQueueDatabase.getInstance(applicationContext)
              .getPlayQueueDao()
              .getLastPlayed()

            mLastPlayed?.let { lastPlayed ->
              // Load last played songs (starting with last played position)
              val queuePosition = if (lastPlayed.queuePosition in 0 until queue.size) lastPlayed.queuePosition else 0
              controller.prepareFromDescriptions(queue, queuePosition)

              controller.transportControls.setRepeatMode(lastPlayed.repeatMode)
              controller.transportControls.setShuffleMode(lastPlayed.shuffleMode, lastPlayed.shuffleSeed)
            }
          }
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
    private const val LOG_TAG = "MuzikPlayerService"

    const val ACTION_OPEN_PLAYER = "hu.mrolcsi.muzik.OPEN_PLAYER"
  }
}