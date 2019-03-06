package hu.mrolcsi.android.lyricsplayer.service.exoplayer

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import hu.mrolcsi.android.lyricsplayer.BuildConfig
import hu.mrolcsi.android.lyricsplayer.R

class ExoNotificationManager(
  context: Context,
  session: MediaSessionCompat,
  player: Player,
  notificationListener: PlayerNotificationManager.NotificationListener
) {

  private val mDescriptionAdapter = object : PlayerNotificationManager.MediaDescriptionAdapter {
    override fun createCurrentContentIntent(player: Player): PendingIntent? = session.controller.sessionActivity

    override fun getCurrentContentText(player: Player): String? {
      val description = getDescription(session)
      return description?.subtitle.toString()
    }

    override fun getCurrentContentTitle(player: Player): String {
      val description = getDescription(session)
      return description?.title.toString()
    }

    override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback?): Bitmap? {
      val description = getDescription(session)
      return description?.iconBitmap
    }
  }

  // Connect this notification manager to the session
  private val mNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
    context,
    NOTIFICATION_CHANNEL,
    R.string.notification_nowPlaying_description,
    NOTIFICATION_ID,
    mDescriptionAdapter
  ).apply {
    setNotificationListener(notificationListener)
    setMediaSessionToken(session.sessionToken)
    setPlayer(player)
    // Some customization
    setOngoing(true)
    setStopAction(null)
    setRewindIncrementMs(0)
    setFastForwardIncrementMs(0)
    setSmallIcon(R.drawable.ic_song)
  }

  /**
   * Get the [MediaDescriptionCompat] from the provided [Player].
   */
  private fun getDescription(player: Player): MediaDescriptionCompat? {
    // Get current playlist position
    val windowIndex = player.currentWindowIndex
    // Get description from Window
    return player.currentTimeline.getWindow(windowIndex, Timeline.Window(), true).tag as MediaDescriptionCompat
  }

  /**
   * Get current [MediaDescriptionCompat] from the provided [MediaSessionCompat].
   */
  private fun getDescription(session: MediaSessionCompat): MediaDescriptionCompat? =
    session.controller?.metadata?.description

  fun release() {
    mNotificationManager.setPlayer(null)
  }

  companion object {
    const val NOTIFICATION_ID = 6854
    const val NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID + ".LPChannel"
  }
}