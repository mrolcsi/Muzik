package hu.mrolcsi.android.lyricsplayer.service

import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import hu.mrolcsi.android.lyricsplayer.R

class LPPlayerService : LPBrowserService() {

  private lateinit var mMediaSession: MediaSessionCompat

  private lateinit var mStateBuilder: PlaybackStateCompat.Builder

  override fun onCreate() {
    super.onCreate()

    // Create a MediaSessionCompat
    mMediaSession = MediaSessionCompat(baseContext, LOG_TAG).apply {

      // Enable callbacks from MediaButtons and TransportControls
      setFlags(
        MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
            or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
      )

      // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
      mStateBuilder = PlaybackStateCompat.Builder()
        .setActions(
          PlaybackStateCompat.ACTION_PLAY
              or PlaybackStateCompat.ACTION_PLAY_PAUSE
        )
      setPlaybackState(mStateBuilder.build())

      // MySessionCallback() has methods that handle callbacks from a media controller
      setCallback(LPSessionCallback())

      // Set the session's token so that client activities can communicate with it.
      setSessionToken(sessionToken)
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

    // Given a media session and its context (usually the component containing the session)
    // Create a NotificationCompat.Builder

    // Get the session's metadata
    val controller = mMediaSession.controller
    val mediaMetadata = controller.metadata
    val description = mediaMetadata.description

    val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL).apply {
      // Add the metadata for the currently playing track
      setContentTitle(description.title)
      setContentText(description.subtitle)
      setSubText(description.description)
      setLargeIcon(description.iconBitmap)

      // Enable launching the player by clicking the notification
      setContentIntent(controller.sessionActivity)

      // Stop the service when the notification is swiped away
      setDeleteIntent(
        MediaButtonReceiver.buildMediaButtonPendingIntent(
          applicationContext,
          PlaybackStateCompat.ACTION_STOP
        )
      )

      // Make the transport controls visible on the lockscreen
      setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

      // Add an app icon and set its accent color
      // Be careful about the color
      setSmallIcon(android.R.drawable.ic_media_play)
      color = ContextCompat.getColor(applicationContext, R.color.primaryDarkColor)

      // Add a pause button
      addAction(
        NotificationCompat.Action(
          android.R.drawable.ic_media_pause,
          "Pause",
          MediaButtonReceiver.buildMediaButtonPendingIntent(
            applicationContext,
            PlaybackStateCompat.ACTION_PLAY_PAUSE
          )
        )
      )

      // Take advantage of MediaStyle features
      setStyle(
        androidx.media.app.NotificationCompat.MediaStyle()
          .setMediaSession(mMediaSession.sessionToken)
          .setShowActionsInCompactView(0)

          // Add a cancel button
          .setShowCancelButton(true)
          .setCancelButtonIntent(
            MediaButtonReceiver.buildMediaButtonPendingIntent(
              applicationContext,
              PlaybackStateCompat.ACTION_STOP
            )
          )
      )
    }

    // Display the notification and place the service in the foreground
    startForeground(NOTIFICATION_ID, builder.build())

    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {
    super.onDestroy()

    // Remove notification
    NotificationManagerCompat.from(applicationContext).cancel(NOTIFICATION_ID)
  }

  companion object {
    private const val LOG_TAG = "LPPlayerService"

    private const val NOTIFICATION_ID = 6854
    private const val NOTIFICATION_CHANNEL = "LPChannel"
  }
}