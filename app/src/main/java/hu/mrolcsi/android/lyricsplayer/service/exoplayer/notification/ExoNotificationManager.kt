package hu.mrolcsi.android.lyricsplayer.service.exoplayer.notification

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.os.AsyncTask
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import hu.mrolcsi.android.lyricsplayer.BuildConfig
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.media.albumArt
import hu.mrolcsi.android.lyricsplayer.extensions.media.artist
import hu.mrolcsi.android.lyricsplayer.extensions.media.from
import hu.mrolcsi.android.lyricsplayer.extensions.media.title

class ExoNotificationManager(
  context: Context,
  session: MediaSessionCompat,
  player: Player,
  notificationListener: PlayerNotificationManager.NotificationListener
) {

  private val mDescriptionAdapter = object : PlayerNotificationManager.MediaDescriptionAdapter {

    private var mLastValidMetadata: MediaMetadataCompat? = null

    override fun createCurrentContentIntent(player: Player): PendingIntent? = session.controller.sessionActivity

    override fun getCurrentContentText(player: Player): String? {
      val metadata = getSessionMetadata(session)
      return metadata?.artist ?: "Unknown Artist" // TODO: I18n
    }

    override fun getCurrentContentTitle(player: Player): String? {
      val metadata = getSessionMetadata(session)
      return metadata?.title ?: "Unknown Song" // TODO: I18n
    }

    override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback?): Bitmap? {
      val metadata = getSessionMetadata(session)

      metadata?.let {
        if (metadata.albumArt == null) {
          AsyncTask.execute {
            // load bitmap from MetadataRetriever
            val newMetadata = MediaMetadataCompat.Builder(metadata).from(metadata.description).build()
            callback?.onBitmap(newMetadata.albumArt)
            mLastValidMetadata = newMetadata
          }
        } else {
          mLastValidMetadata = metadata
        }
      }

      return metadata?.albumArt ?: mLastValidMetadata?.albumArt
    }

    /**
     * Get current [MediaDescriptionCompat] from the provided [MediaSessionCompat].
     */
    private fun getSessionMetadata(session: MediaSessionCompat): MediaMetadataCompat? {
      return session.controller?.metadata
    }
  }

  // Connect this notification manager to the session
  private val mNotificationManager = PlayerNotificationManager2.createWithNotificationChannel(
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
    setUseChronometer(true)
    setUseStopAction(false)
    setRewindIncrementMs(0)
    setFastForwardIncrementMs(0)
    setSmallIcon(R.drawable.ic_song)
    setPlayIcon(R.drawable.ic_media_play)
    setPauseIcon(R.drawable.ic_media_pause)
    setPreviousIcon(R.drawable.ic_media_previous)
    setNextIcon(R.drawable.ic_media_next)
  }

  fun release() {
    mNotificationManager.setPlayer(null)
  }

  companion object {
    const val NOTIFICATION_ID = 6854
    const val NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID + ".LPChannel"
  }
}