package hu.mrolcsi.android.lyricsplayer.service.exoplayer.notification

import android.content.Context
import androidx.annotation.StringRes
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.util.NotificationUtil

class PlayerNotificationManagerExt(
  context: Context?,
  channelId: String?,
  notificationId: Int,
  mediaDescriptionAdapter: MediaDescriptionAdapter?,
  notificationListener: NotificationListener?
) : PlayerNotificationManager(context, channelId, notificationId, mediaDescriptionAdapter, notificationListener) {

  override fun getActionIndicesForCompactView(actionNames: MutableList<String>, player: Player): IntArray {
    val playPauseActionIndex = super.getActionIndicesForCompactView(actionNames, player)
    val nextActionIndex = actionNames.indexOf(ACTION_NEXT)
    return if (nextActionIndex != -1) {
      playPauseActionIndex + nextActionIndex
    } else {
      playPauseActionIndex
    }
  }

  companion object {
    /**
     * Creates a notification manager and a low-priority notification channel with the specified
     * `channelId` and `channelName`. The [com.google.android.exoplayer2.ui.PlayerNotificationManager.NotificationListener] passed as the last
     * parameter will be notified when the notification is created and cancelled.
     *
     * @param context The [Context].
     * @param channelId The id of the notification channel.
     * @param channelName A string resource identifier for the user visible name of the channel. The
     * recommended maximum length is 40 characters; the value may be truncated if it is too long.
     * @param notificationId The id of the notification.
     * @param mediaDescriptionAdapter The [com.google.android.exoplayer2.ui.PlayerNotificationManager.MediaDescriptionAdapter].
     * @param notificationListener The [com.google.android.exoplayer2.ui.PlayerNotificationManager.NotificationListener].
     */
    fun createWithNotificationChannel(
      context: Context,
      channelId: String,
      @StringRes channelName: Int,
      notificationId: Int,
      mediaDescriptionAdapter: MediaDescriptionAdapter,
      notificationListener: NotificationListener?
    ): PlayerNotificationManagerExt {
      NotificationUtil.createNotificationChannel(
        context, channelId, channelName, NotificationUtil.IMPORTANCE_LOW
      )
      return PlayerNotificationManagerExt(
        context, channelId, notificationId, mediaDescriptionAdapter, notificationListener
      )
    }
  }
}