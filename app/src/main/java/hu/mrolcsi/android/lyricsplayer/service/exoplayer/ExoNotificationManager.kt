package hu.mrolcsi.android.lyricsplayer.service.exoplayer

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import hu.mrolcsi.android.lyricsplayer.BuildConfig
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.media.from
import hu.mrolcsi.android.lyricsplayer.extensions.media.fullDescription

class ExoNotificationManager(
  context: Context,
  session: MediaSessionCompat,
  player: Player,
  notificationListener: PlayerNotificationManager.NotificationListener
) {

  private val mDescriptionAdapter = object : PlayerNotificationManager.MediaDescriptionAdapter {

    private var mLastUsedDescription: MediaDescriptionCompat? = null

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

    /**
     * Get current [MediaDescriptionCompat] from the provided [MediaSessionCompat].
     */
    private fun getDescription(session: MediaSessionCompat): MediaDescriptionCompat? {
      val description = session.controller?.metadata?.description
      // Check if mediaId has changed
      if (description != null && description.mediaId != mLastUsedDescription?.mediaId) {
        // Load additional metadata into description
        mLastUsedDescription = MediaMetadataCompat.Builder().from(description).build().fullDescription
      }

      return mLastUsedDescription
    }
  }

  // Connect this notification manager to the session
  private val mNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
    context,
    NOTIFICATION_CHANNEL,
    R.string.notification_nowPlaying,
    NOTIFICATION_ID,
    mDescriptionAdapter,
    notificationListener
  ).apply {
    setMediaSessionToken(session.sessionToken)
    setPlayer(player)
    // Some customization
    setUseChronometer(false)
    setUseStopAction(false)
    setRewindIncrementMs(0)
    setFastForwardIncrementMs(0)
    setSmallIcon(R.drawable.ic_song)
  }

  fun release() {
    mNotificationManager.setPlayer(null)
  }

  companion object {
    const val NOTIFICATION_ID = 6854
    const val NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID + ".LPChannel"
  }
}