package hu.mrolcsi.android.lyricsplayer.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.AsyncTask
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.Player
import hu.mrolcsi.android.lyricsplayer.database.playqueue.PlayQueueDatabase
import hu.mrolcsi.android.lyricsplayer.database.playqueue.entities.LastPlayed
import hu.mrolcsi.android.lyricsplayer.extensions.media.addQueueItems
import hu.mrolcsi.android.lyricsplayer.extensions.media.albumArt
import hu.mrolcsi.android.lyricsplayer.extensions.media.from
import hu.mrolcsi.android.lyricsplayer.player.PlayerActivity
import hu.mrolcsi.android.lyricsplayer.service.exoplayer.ExoPlayerHolder
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager

class LPPlayerService : LPBrowserService() {

  // Indicates if the service is running in the foreground
  private var isForegroundService = false

  // Indicates f the service has been ordered to stop
  private var isServiceStopped = false

  // MediaSession and Player implementations
  private lateinit var mMediaSession: MediaSessionCompat
  private lateinit var mPlayerHolder: ExoPlayerHolder

  // Last played position
  private var mLastPlayed: LastPlayed? = null

  // ExoPlayer Notification
  //private lateinit var mExoNotificationManager: ExoNotificationManager
  //private var mNotificationId: Int = 0
  //private var mNotification: Notification? = null

  // Custom built Notification
  private lateinit var mNotificationBuilder: LPNotificationBuilder

  override fun onCreate() {
    super.onCreate()

    Log.i(LOG_TAG, "onCreate()")

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
            or MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS
      )

      // Connect this session with the ExoPlayer
      mPlayerHolder = ExoPlayerHolder(applicationContext, this).apply {
        getPlayer().addListener(object : Player.EventListener {
          override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Log.v(LOG_TAG, "onPlayerStateChanged(playWhenReady=$playWhenReady, playbackState=$playbackState)")

            when (playbackState) {
              Player.STATE_READY -> {
                // Set player to last played settings
                mLastPlayed?.let {
                  Log.d(LOG_TAG, "Loaded 'Last Played' from database: $it")

                  // Skip to last played song
                  controller.transportControls.skipToQueueItem(it.queuePosition.toLong())

                  // Seek to saved position
                  controller.transportControls.seekTo(it.trackPosition)

                  // Set mLastPlayed to null, so we won't call this again
                  mLastPlayed = null
                }
              }
            }
          }
        })
      }

      // Set the session's token so that client activities can communicate with it.
      setSessionToken(sessionToken)

      // Register basic callbacks
      controller.registerCallback(object : MediaControllerCompat.Callback() {

        // Last received metadata
        private var previousMetadata: MediaMetadataCompat? = null

        // Previous load
        private var currentLoadTask: AsyncTask<MediaMetadataCompat, Nothing, MediaMetadataCompat>? = null

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
          state?.let { updateNotification(it) }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
          // Ignore nulls
          controller.playbackState?.let { state ->
            metadata?.let {

              // Check if metadata has actually changed
              val mediaId = metadata.description?.mediaId
              val differentMediaId = mediaId != previousMetadata?.description?.mediaId

              // Prepare onPostExecute callback
              val onPostExecute: (MediaMetadataCompat) -> Unit = { newMetadata ->
                // Give newly created metadata to the session
                setMetadata(newMetadata)

                // Update Theme
                newMetadata.albumArt?.let { bitmap ->
                  ThemeManager.updateFromBitmap(bitmap)
                }

                // Update notification
                updateNotification(state)
              }

              // Same mediaId
              when {
                differentMediaId -> {
                  // Cancel previous load
                  currentLoadTask?.cancel(true)

                  // Save as last received metadata
                  previousMetadata = metadata

                  // Start new load
                  currentLoadTask = MetadataLoaderTask(onPostExecute).execute(metadata)
                }
                metadata.albumArt == null && currentLoadTask?.status != AsyncTask.Status.RUNNING -> {
                  // Start new load
                  currentLoadTask = MetadataLoaderTask(onPostExecute).execute(metadata)
                }
                else -> {
                  // Update notification anyway
                  // (Needed to properly update media buttons)
                  updateNotification(state)
                }
              }
            }
          }
        }

        private fun updateNotification(state: PlaybackStateCompat) {
          Log.v(LOG_TAG, "updateNotification($state)")
          val updatedState = state.state
//          if (controller.metadata == null) {
//            return
//          }

          // Skip building a notification when state is "none".
          val notification = if (updatedState != PlaybackStateCompat.STATE_NONE) {
            mNotificationBuilder.buildNotification(sessionToken)
          } else {
            null
          }

          when (updatedState) {
            PlaybackStateCompat.STATE_BUFFERING,
            PlaybackStateCompat.STATE_PLAYING -> {
              /**
               * This may look strange, but the documentation for [Service.startForeground]
               * notes that "calling this method does *not* put the service in the started
               * state itself, even though the name sounds like it."
               */
              if (!isForegroundService) {
                Log.d(LOG_TAG, "updateNotification(): startService(...) and startForeground(id,notification) called.")
                startService(Intent(applicationContext, this@LPPlayerService.javaClass))
                startForeground(LPNotificationBuilder.NOTIFICATION_ID, notification)
                isForegroundService = true
              } else if (notification != null) {
                Log.v(LOG_TAG, "updateNotification() notify(id, notification) called.")
                NotificationManagerCompat.from(applicationContext)
                  .notify(LPNotificationBuilder.NOTIFICATION_ID, notification)
              }
            }
            else -> {
              if (isForegroundService) {
                Log.d(LOG_TAG, "updateNotification(): stopForeground(false) called.")
                stopForeground(false)
                isForegroundService = false

                // If playback has ended, also stop the service.
                if (updatedState == PlaybackStateCompat.STATE_NONE) {
                  Log.d(LOG_TAG, "updateNotification(): stopSelf() called.")
                  stopSelf()
                  //stopThis()
                }

                if (notification != null) {
                  Log.v(LOG_TAG, "updateNotification() notify(id, notification) called.")
                  NotificationManagerCompat.from(applicationContext)
                    .notify(LPNotificationBuilder.NOTIFICATION_ID, notification)
                } else {
                  Log.d(LOG_TAG, "updateNotification(): stopForeground(true) called.")
                  stopForeground(true)
                }
              }
            }
          }
        }
      })

      AsyncTask.execute {
        // Get last played queue from the database
        val queue = PlayQueueDatabase.getInstance(applicationContext)
          .getPlayQueueDao()
          .getQueue()
          .map { it.createDescription() }

        Log.d(LOG_TAG, "Loaded queue from database: $queue")

        if (queue.isNotEmpty()) {
          // Load last played songs
          controller.addQueueItems(queue)

          // Get last played positions from the database
          mLastPlayed = PlayQueueDatabase.getInstance(applicationContext)
            .getPlayQueueDao()
            .getLastPlayed()
        }
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    MediaButtonReceiver.handleIntent(mMediaSession, intent)
    return Service.START_STICKY
  }

  override fun onDestroy() {
    // Avoid calling stop multiple times.
    if (!isServiceStopped) {
      Log.i(LOG_TAG, "onDestroy()")

      // Deactivate the session
      mMediaSession.run {
        isActive = false
        release()
      }

      // Detach the player from the notification
      //mExoNotificationManager.release()

      // Release player and related resources
      mPlayerHolder.release()

      // Close database
      PlayQueueDatabase.getInstance(applicationContext).close()
    }
  }

  private fun stopThis() {
    stopSelf()
    onDestroy()

    isServiceStopped = true
  }

  companion object {
    const val LOG_TAG = "LPPlayerService"

    private open class MetadataLoaderTask(private val onPostExecute: (MediaMetadataCompat) -> Unit) :
      AsyncTask<MediaMetadataCompat, Nothing, MediaMetadataCompat>() {

      override fun doInBackground(vararg params: MediaMetadataCompat?): MediaMetadataCompat {
        val oldMetadata = params[0]!!

        Log.d(LOG_TAG, "Loading metadata in the background for ${oldMetadata.description.mediaId}")

        return MediaMetadataCompat.Builder(oldMetadata).from(oldMetadata.description).build()
      }

      override fun onPostExecute(result: MediaMetadataCompat) {
        if (!isCancelled) {
          // Deliver result
          onPostExecute.invoke(result)
        }
      }
    }
  }
}