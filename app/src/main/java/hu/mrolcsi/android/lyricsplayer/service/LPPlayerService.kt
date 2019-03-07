package hu.mrolcsi.android.lyricsplayer.service

import android.app.PendingIntent
import android.content.Intent
import android.os.AsyncTask
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.Player
import hu.mrolcsi.android.lyricsplayer.extensions.media.albumArt
import hu.mrolcsi.android.lyricsplayer.player.PlayerActivity
import hu.mrolcsi.android.lyricsplayer.service.exoplayer.ExoPlayerHolder
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager

class LPPlayerService : LPBrowserService() {

  // Indicates if the service is running in the foreground
  private var isForegroundService = false

  // MediaSession and Player implementations
  private lateinit var mMediaSession: MediaSessionCompat
  private lateinit var mPlayerHolder: ExoPlayerHolder

  // ExoPlayer Notification
  //private lateinit var mExoNotificationManager: ExoNotificationManager
  //private var mNotificationId: Int = 0
  //private var mNotification: Notification? = null

  // Custom built Notification
  private lateinit var mNotificationBuilder: LPNotificationBuilder

  // Last received metadata
  private var mLastMetadata: MediaMetadataCompat? = null

  override fun onCreate() {
    super.onCreate()

    Log.d(LOG_TAG, "onCreate()")

    // Build a PendingIntent that can be used to launch the PlayerActivity.
    val playerActivityPendingIntent = TaskStackBuilder.create(this)
      // add all of DetailsActivity's parents to the stack,
      // followed by DetailsActivity itself
      .addNextIntentWithParentStack(Intent(this, PlayerActivity::class.java))
      .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

    // prepare notification
    mNotificationBuilder = LPNotificationBuilder(this)

    // Create a MediaSessionCompat
    mMediaSession = MediaSessionCompat(this, LOG_TAG).apply {
      setSessionActivity(playerActivityPendingIntent)

      // Enable callbacks from MediaButtons and TransportControls
      setFlags(
        MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
            or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            or MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS
      )

      // Connect this session with the ExoPlayer
      mPlayerHolder = ExoPlayerHolder(applicationContext, this).apply {
        getPlayer().addListener(object : Player.EventListener {
          override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
//            if (mNotification != null) {
//              when (playWhenReady) {
//                // Enable swipe-to-dismiss on the notification
//                false -> stopForeground(false)
//                // Lock notification again
//                true -> startForeground(mNotificationId, mNotification)
//              }
//            }
          }
        })
      }

      //region -- NOTIFICATION WITH EXO PLAYER --

      // Prepare notification
//      mExoNotificationManager = ExoNotificationManager(
//        applicationContext,
//        this,
//        mPlayerHolder.getPlayer(),
//        object : PlayerNotificationManager.NotificationListener {
//          override fun onNotificationStarted(notificationId: Int, notification: Notification?) {
//            // Cache the id and the created notification for later use
//            mNotificationId = notificationId
//            mNotification = notification
//
//            // Start service
//            if (!isForegroundService) {
//              startService(Intent(applicationContext, this@LPPlayerService.javaClass))
//              startForeground(notificationId, notification)
//              isForegroundService = true
//            } else if (notification != null) {
//              NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
//            }
//          }
//
//          override fun onNotificationCancelled(notificationId: Int) {
//            if (isForegroundService) {
//              stopForeground(false)
//              isForegroundService = false
//
//              // If playback has ended, also stop the service.
//              stopSelf()
//              stopForeground(true)
//            }
//          }
//        }
//      )

      //endregion

      // Set the session's token so that client activities can communicate with it.
      setSessionToken(sessionToken)

      // Register basic callbacks
      controller.registerCallback(object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
          controller.playbackState?.let { state ->

            updateNotification(state)
            // Check if metadata has actually changed
            if (metadata?.description?.mediaId != mLastMetadata?.description?.mediaId) {
              metadata?.albumArt?.let { bitmap ->
                ThemeManager.updateFromBitmap(bitmap)
              }

              // Save as last received metadata
              mLastMetadata = metadata
            } // TODO: else -> create theme from placeholder
          }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
          state?.let { updateNotification(it) }
        }

        private fun updateNotification(playbackState: PlaybackStateCompat) {
          if (controller.metadata == null) {
            Log.w(LOG_TAG, "MediaMetadata is null!")
            return
          }

          // Skip building a notification when state is "none".
          val notification = if (playbackState.state != PlaybackStateCompat.STATE_NONE) {
            mNotificationBuilder.buildNotification(sessionToken)
          } else {
            null
          }

          when (playbackState.state) {
            PlaybackStateCompat.STATE_PLAYING -> {
              if (!isForegroundService) {
                startService(Intent(applicationContext, this@LPPlayerService.javaClass))
                startForeground(LPNotificationBuilder.NOTIFICATION_ID, notification)
                isForegroundService = true
              } else if (notification != null) {
                NotificationManagerCompat.from(applicationContext)
                  .notify(LPNotificationBuilder.NOTIFICATION_ID, notification)
              }
            }
            else -> {
              if (isForegroundService) {
                stopForeground(false)
                isForegroundService = false

                // If playback has ended, also stop the service.
                when (playbackState.state) {
                  PlaybackStateCompat.STATE_NONE,
                  PlaybackStateCompat.STATE_STOPPED -> stopSelf()
                }

                if (notification != null) {
                  NotificationManagerCompat.from(applicationContext)
                    .notify(LPNotificationBuilder.NOTIFICATION_ID, notification)
                } else {
                  stopForeground(true)
                }
              }
            }
          }
        }
      })

      // FIXME: Load last played queue in the background
      AsyncTask.execute {
        with(LastPlayedSetting(applicationContext)) {
          if (lastPlayedQueue.isNotEmpty()) {
            // create descriptions from StringSet
          }
          // load descriptions into the queue
        }
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    MediaButtonReceiver.handleIntent(mMediaSession, intent)
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {

    // Deactivate the session
    mMediaSession.run {
      isActive = false
      release()
    }

    // Detach the player from the notification
    //mExoNotificationManager.release()

    // Release player and related resources
    mPlayerHolder.release()

    // Remove notification
    NotificationManagerCompat.from(this).cancel(LPNotificationBuilder.NOTIFICATION_ID)

    super.onDestroy()
  }

  companion object {
    const val LOG_TAG = "LPPlayerService"
  }
}