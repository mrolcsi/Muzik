package hu.mrolcsi.android.lyricsplayer.service

import android.app.PendingIntent
import android.content.Intent
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.media.session.MediaButtonReceiver
import hu.mrolcsi.android.lyricsplayer.player.PlayerActivity


class LPPlayerService : LPBrowserService() {

  private var isForegroundService = false

  private lateinit var mMediaSession: MediaSessionCompat

  private lateinit var mNotificationBuilder: LPNotificationBuilder

  override fun onCreate() {
    super.onCreate()

    Log.d(LOG_TAG, "onCreate()")

    // Build a PendingIntent that can be used to launch the PlayerActivity.
    val playerActivityPendingIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, PlayerActivity::class.java),
      0
    )

    // Create a MediaSessionCompat
    mMediaSession = MediaSessionCompat(this, LOG_TAG).apply {
      setSessionActivity(playerActivityPendingIntent)

      // Enable session.
      isActive = true

      // Enable callbacks from MediaButtons and TransportControls
      setFlags(
        MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
            or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
      )

      // MySessionCallback() has methods that handle callbacks from a media controller
      setCallback(LPSessionCallback(this))

      // Set the session's token so that client activities can communicate with it.
      setSessionToken(sessionToken)

      controller.registerCallback(object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
          controller.playbackState?.let { updateNotification(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
          state?.let { updateNotification(it) }
        }

        private fun updateNotification(playbackState: PlaybackStateCompat) {
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
                if (playbackState.state == PlaybackStateCompat.STATE_NONE) {
                  stopSelf()
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
    }

    // prepare notification
    mNotificationBuilder = LPNotificationBuilder(this)
  }

  override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
    Log.d(LOG_TAG, "Intent received: $intent")
    MediaButtonReceiver.handleIntent(mMediaSession, intent)
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {
    super.onDestroy()

    mMediaSession.run {
      isActive = false
      release()
    }

    // Remove notification
    NotificationManagerCompat.from(this).cancel(LPNotificationBuilder.NOTIFICATION_ID)
  }

  companion object {
    const val LOG_TAG = "LPPlayerService"
  }
}