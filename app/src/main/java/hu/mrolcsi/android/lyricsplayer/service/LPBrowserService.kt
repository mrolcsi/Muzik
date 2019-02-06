package hu.mrolcsi.android.lyricsplayer.service

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContentResolverCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import hu.mrolcsi.android.lyricsplayer.BuildConfig
import hu.mrolcsi.android.lyricsplayer.R

class LPBrowserService : MediaBrowserServiceCompat() {

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

  override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
    // (Optional) Control the level of access for the specified package name.
    // You'll need to write your own logic to do this.
    return if (allowBrowsing(clientPackageName, clientUid)) {
      // Returns a root ID that clients can use with onLoadChildren() to retrieve
      // the content hierarchy.
      MediaBrowserServiceCompat.BrowserRoot(MEDIA_ROOT_ID, null)
    } else {
      // Clients can connect, but this BrowserRoot is an empty hierarchy
      // so onLoadChildren returns nothing. This disables the ability to browse for content.
      MediaBrowserServiceCompat.BrowserRoot(EMPTY_MEDIA_ROOT_ID, null)
    }
  }

  override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
    //  Browsing not allowed
    if (parentId == EMPTY_MEDIA_ROOT_ID) {
      result.sendResult(null)
      return
    }

    // Assume for example that the music catalog is already loaded/cached.

    val mediaItems = emptyList<MediaBrowserCompat.MediaItem>().toMutableList()

    when (parentId) {
      MEDIA_ARTISTS_ID -> {
        val cursorWithArtists = ContentResolverCompat.query(
          contentResolver,
          MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
          arrayOf(
            MediaStore.Audio.ArtistColumns.ARTIST_KEY,
            MediaStore.Audio.ArtistColumns.ARTIST
          ),
          null,
          null,
          MediaStore.Audio.ArtistColumns.ARTIST_KEY,
          null
        )

        // finish in the background
        result.detach()

        cursorWithArtists.use {
          while (it.moveToNext()) {

            val description = MediaDescriptionCompat.Builder()
              .setMediaId(it.getString(0))
              .setTitle(it.getString(1))
              .build()

            mediaItems.add(
              MediaBrowserCompat.MediaItem(
                description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
              )
            )
          }
        }
      }
    }

//    // Check if this is the root menu:
//    if (parentId == MEDIA_ARTISTS_ID) {
//      // Build the MediaItem objects for the top level,
//      // and put them in the mediaItems list...
//    } else {
//      // Examine the passed parentMediaId to see which submenu we're at,
//      // and put the children of that menu in the mediaItems list...
//    }

    result.sendResult(mediaItems)
  }

  // --------

  private fun allowBrowsing(clientPackageName: String, clientUid: Int): Boolean {
    return clientPackageName.startsWith(BuildConfig.APPLICATION_ID)
  }

  companion object {
    private const val LOG_TAG = "LPBrowserService"

    const val MEDIA_ARTISTS_ID = "media_artists"
    const val MEDIA_ALBUMS_ID = "media_albums"
    const val MEDIA_SONGS_ID = "media_songs"

    private const val MEDIA_ROOT_ID = MEDIA_ARTISTS_ID
    private const val EMPTY_MEDIA_ROOT_ID = "empty_root_id"

    private const val NOTIFICATION_ID = 6854
    private const val NOTIFICATION_CHANNEL = "LPCHannel"
  }
}
