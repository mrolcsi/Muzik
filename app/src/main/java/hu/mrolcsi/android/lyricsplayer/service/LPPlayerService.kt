package hu.mrolcsi.android.lyricsplayer.service

import android.app.PendingIntent
import android.content.Intent
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.media.session.MediaButtonReceiver
import hu.mrolcsi.android.lyricsplayer.extensions.albumArt
import hu.mrolcsi.android.lyricsplayer.player.PlayerActivity
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager


class LPPlayerService : LPBrowserService() {

  private var isForegroundService = false

  private lateinit var mMediaSession: MediaSessionCompat

  private lateinit var mNotificationBuilder: LPNotificationBuilder

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
      )

      // MySessionCallback() has methods that handle callbacks from a media mediaController
      // Pass the service as an argument, so the service can be handled from the callbacks.
      setCallback(LPSessionCallback(applicationContext, this))

      // Set the session's token so that client activities can communicate with it.
      setSessionToken(sessionToken)

      controller.registerCallback(object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
          controller.playbackState?.let {
            updateNotification(it)
            if (metadata?.albumArt != null) {
              ThemeManager.update(metadata.albumArt)
            } // TODO: else -> create theme from placeholder
          }
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

      // Load last played MediaItem
      with(LastPlayedSetting(applicationContext)) {
        lastPlayedMedia?.let {
          Log.d(LOG_TAG, "Loading last played: $it")
          controller.transportControls.prepareFromMediaId(it, null)
          controller.transportControls.seekTo(lastPlayedPosition)
        }
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

    const val ACTION_START_UPDATER = "ACTION_START_UPDATER"
    const val ACTION_STOP_UPDATER = "ACTION_STOP_UPDATER"
  }
}