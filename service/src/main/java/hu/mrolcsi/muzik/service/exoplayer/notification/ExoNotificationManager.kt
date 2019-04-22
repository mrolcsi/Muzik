package hu.mrolcsi.muzik.service.exoplayer.notification

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.os.AsyncTask
import android.provider.MediaStore
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.collection.LruCache
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import hu.mrolcsi.muzik.service.BuildConfig
import hu.mrolcsi.muzik.service.R
import hu.mrolcsi.muzik.service.extensions.media.albumArtUri
import hu.mrolcsi.muzik.service.extensions.media.artist
import hu.mrolcsi.muzik.service.extensions.media.id
import hu.mrolcsi.muzik.service.extensions.media.mediaId
import hu.mrolcsi.muzik.service.extensions.media.title

class ExoNotificationManager(
  context: Context,
  session: MediaSessionCompat,
  player: Player,
  notificationListener: PlayerNotificationManager.NotificationListener
) {

  private val mDescriptionAdapter = object : PlayerNotificationManager.MediaDescriptionAdapter {

    private val mBitmapCache = LruCache<String, Bitmap>(20)

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

      val cachedBitmap = metadata?.mediaId?.let { mBitmapCache[it] }
      if (cachedBitmap == null) {
        AsyncTask.execute {
          metadata?.let {
            if (it.id > 0) {
              try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it.albumArtUri)

                it.mediaId?.let { id ->
                  mBitmapCache.put(id, bitmap)
                }

                callback?.onBitmap(bitmap)
              } catch (e: NullPointerException) {
                // MediaStore throws a NullPointerException when the image doesn't exist
              }
            }
          }
        }
      }

      return cachedBitmap
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
    setSmallIcon(R.drawable.ic_notification)
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