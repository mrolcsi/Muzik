package hu.mrolcsi.android.lyricsplayer.service

import android.app.Notification
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
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import hu.mrolcsi.android.lyricsplayer.extensions.media.albumArt
import hu.mrolcsi.android.lyricsplayer.player.PlayerActivity
import hu.mrolcsi.android.lyricsplayer.service.exoplayer.ExoNotificationManager
import hu.mrolcsi.android.lyricsplayer.service.exoplayer.ExoPlayerHolder

class LPPlayerService : LPBrowserService() {

  private var isForegroundService = false

  private lateinit var mMediaSession: MediaSessionCompat
  private lateinit var mPlayerHolder: ExoPlayerHolder

  private lateinit var mNotificationManager: ExoNotificationManager
  private var mNotificationId: Int = 0
  private var mNotification: Notification? = null

  override fun onCreate() {
    super.onCreate()

    Log.d(LOG_TAG, "onCreate()")

    // Build a PendingIntent that can be used to launch the PlayerActivity.
    val playerActivityPendingIntent = TaskStackBuilder.create(this)
      // add all of DetailsActivity's parents to the stack,
      // followed by DetailsActivity itself
      .addNextIntentWithParentStack(Intent(this, PlayerActivity::class.java))
      .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

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
            if (mNotification != null) {
              when (playWhenReady) {
                // Enable swipe-to-dismiss on the notification
                false -> stopForeground(false)
                // Lock notification again
                true -> startForeground(mNotificationId, mNotification)
              }
            }
          }
        })
      }

      // Prepare notification
      mNotificationManager = ExoNotificationManager(
        applicationContext,
        this,
        mPlayerHolder.getPlayer(),
        object : PlayerNotificationManager.NotificationListener {
          override fun onNotificationStarted(notificationId: Int, notification: Notification?) {
            // Cache the id and the created notification for later use
            mNotificationId = notificationId
            mNotification = notification

            // Start service
            if (!isForegroundService) {
              startService(Intent(applicationContext, this@LPPlayerService.javaClass))
              startForeground(notificationId, notification)
              isForegroundService = true
            } else if (notification != null) {
              NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
            }
          }

          override fun onNotificationCancelled(notificationId: Int) {
            if (isForegroundService) {
              stopForeground(false)
              isForegroundService = false

              // If playback has ended, also stop the service.
              stopSelf()
              stopForeground(true)
            }
          }
        }
      )

      // Set the session's token so that client activities can communicate with it.
      setSessionToken(sessionToken)

      // Register basic callbacks
      controller.registerCallback(object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
          metadata?.albumArt?.let { bitmap ->
            //FIXME: ThemeManager.updateFromBitmap(bitmap)
          }
          // TODO: else -> create theme from placeholder?
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
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
    mNotificationManager.release()

    // Release player and related resources
    mPlayerHolder.release()

    super.onDestroy()
  }

  companion object {
    const val LOG_TAG = "LPPlayerService"
  }
}