/*
 * Copyright 2018 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.mrolcsi.android.lyricsplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY
import android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT
import android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
import android.support.v4.media.session.PlaybackStateCompat.ACTION_STOP
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import hu.mrolcsi.android.lyricsplayer.BuildConfig
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.media.isPlayEnabled
import hu.mrolcsi.android.lyricsplayer.extensions.media.isPlaying
import hu.mrolcsi.android.lyricsplayer.extensions.media.isSkipToNextEnabled
import hu.mrolcsi.android.lyricsplayer.extensions.media.isSkipToPreviousEnabled

/**
 * Helper class to encapsulate code for building notifications.
 */
class LPNotificationBuilder(private val context: Context) {

  private val platformNotificationManager: NotificationManager =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  private val skipToPreviousAction = NotificationCompat.Action(
    android.R.drawable.ic_media_previous,
    context.getString(R.string.mediaControl_previous),
    MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_SKIP_TO_PREVIOUS)
  )
  private val playAction = NotificationCompat.Action(
    android.R.drawable.ic_media_play,
    context.getString(R.string.mediaControl_play),
    MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_PLAY)
  )
  private val pauseAction = NotificationCompat.Action(
    android.R.drawable.ic_media_pause,
    context.getString(R.string.mediaControl_pause),
    MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_PAUSE)
  )
  private val skipToNextAction = NotificationCompat.Action(
    android.R.drawable.ic_media_next,
    context.getString(R.string.mediaControl_next),
    MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_SKIP_TO_NEXT)
  )
  private val stopPendingIntent =
    MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_STOP)

  fun buildNotification(sessionToken: MediaSessionCompat.Token): Notification {
    if (shouldCreateNowPlayingChannel()) {
      createNowPlayingChannel()
    }

    val controller = MediaControllerCompat(context, sessionToken)
    val description = controller.metadata.description
    val playbackState = controller.playbackState

    val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)

    // Only add actions for skip back, play/pause, skip forward, based on what's enabled.
    var playPauseIndex = 0
    if (playbackState.isSkipToPreviousEnabled) {
      builder.addAction(skipToPreviousAction)
      ++playPauseIndex
    }
    if (playbackState.isPlaying) {
      builder.addAction(pauseAction)
    } else if (playbackState.isPlayEnabled) {
      builder.addAction(playAction)
    }
    if (playbackState.isSkipToNextEnabled) {
      builder.addAction(skipToNextAction)
    }

    val mediaStyle = MediaStyle()
      .setCancelButtonIntent(stopPendingIntent)
      .setMediaSession(sessionToken)
      .setShowActionsInCompactView(playPauseIndex)
      .setShowCancelButton(true)

    return builder.setContentIntent(controller.sessionActivity)
      .setContentText(description.subtitle)
      .setContentTitle(description.title)
      .setDeleteIntent(stopPendingIntent)
      .setLargeIcon(description.iconBitmap)
      .setOnlyAlertOnce(true)
      .setSmallIcon(R.drawable.ic_song)
      .setStyle(mediaStyle)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .build()
  }

  private fun shouldCreateNowPlayingChannel() =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !nowPlayingChannelExists()

  @RequiresApi(Build.VERSION_CODES.O)
  private fun nowPlayingChannelExists() =
    platformNotificationManager.getNotificationChannel(NOTIFICATION_CHANNEL) != null

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createNowPlayingChannel() {
    val notificationChannel = NotificationChannel(
      NOTIFICATION_CHANNEL,
      context.getString(R.string.notification_nowPlaying),
      NotificationManager.IMPORTANCE_LOW
    ).apply {
      description = context.getString(R.string.notification_nowPlaying_description)
    }

    platformNotificationManager.createNotificationChannel(notificationChannel)
  }

  companion object {
    const val NOTIFICATION_ID = 6854
    private const val NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID + ".LPChannel"
  }
}