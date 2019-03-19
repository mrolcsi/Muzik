package hu.mrolcsi.android.lyricsplayer.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.AsyncTask
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.TaskStackBuilder
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import hu.mrolcsi.android.lyricsplayer.database.playqueue.PlayQueueDatabase
import hu.mrolcsi.android.lyricsplayer.database.playqueue.entities.LastPlayed
import hu.mrolcsi.android.lyricsplayer.extensions.media.addQueueItems
import hu.mrolcsi.android.lyricsplayer.extensions.media.albumArt
import hu.mrolcsi.android.lyricsplayer.extensions.media.from
import hu.mrolcsi.android.lyricsplayer.player.PlayerActivity
import hu.mrolcsi.android.lyricsplayer.service.exoplayer.ExoPlayerHolder
import hu.mrolcsi.android.lyricsplayer.service.exoplayer.notification.ExoNotificationManager
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager

class LPPlayerService : LPBrowserService() {

  // MediaSession and Player implementations
  private lateinit var mMediaSession: MediaSessionCompat
  private lateinit var mPlayerHolder: ExoPlayerHolder

  // Last played position
  private var mLastPlayed: LastPlayed? = null

  // ExoPlayer Notification
  private lateinit var mExoNotificationManager: ExoNotificationManager

  override fun onCreate() {
    super.onCreate()

    Log.i(LOG_TAG, "onCreate()")

    // Build a PendingIntent that can be used to launch the PlayerActivity.
    val playerActivityPendingIntent = TaskStackBuilder.create(this)
      // add all of DetailsActivity's parents to the stack,
      // followed by DetailsActivity itself
      .addNextIntentWithParentStack(Intent(this, PlayerActivity::class.java))
      .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

    // Create a MediaSessionCompat
    mMediaSession = MediaSessionCompat(this, LOG_TAG).apply {
      setSessionActivity(playerActivityPendingIntent)

      // Set the session's token so that client activities can communicate with it.
      setSessionToken(sessionToken)

      // Enable callbacks from MediaButtons and TransportControls
      setFlags(
        MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
            or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            or MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS
      )

      // Connect this session with the ExoPlayer
      mPlayerHolder = ExoPlayerHolder(applicationContext, this).also {
        mExoNotificationManager =
          ExoNotificationManager(applicationContext,
            this,
            it.getPlayer(),
            object : PlayerNotificationManager.NotificationListener {

              private var isForeground = false

              override fun onNotificationPosted(notificationId: Int, notification: Notification?, ongoing: Boolean) {
                if (it.getPlayer().playWhenReady && !isForeground) {
                  startService(Intent(applicationContext, LPPlayerService::class.java))
                  startForeground(notificationId, notification)
                  isForeground = true
                }

                if (!it.getPlayer().playWhenReady && isForeground) {
                  stopForeground(false)
                  isForeground = false
                }
              }

              override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                stopForeground(true)
                stopSelf()
                isForeground = false
              }
            })

        it.getPlayer().addListener(object : Player.EventListener {
          override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Log.v(LOG_TAG, "onPlayerStateChanged(playWhenReady=$playWhenReady, playbackState=$playbackState)")

            // Make notification dismissible
            if (!playWhenReady) {
              stopForeground(false)
            }

            when (playbackState) {
              Player.STATE_READY -> {
                // Set player to last played settings
                mLastPlayed?.let { lastPlayed ->
                  Log.d(LOG_TAG, "Loaded 'Last Played' from database: $lastPlayed")

                  // Skip to last played song
                  controller.transportControls.skipToQueueItem(lastPlayed.queuePosition.toLong())

                  // Seek to saved position
                  controller.transportControls.seekTo(lastPlayed.trackPosition)

                  // Set mLastPlayed to null, so we won't call this again
                  mLastPlayed = null
                }
              }
            }
          }
        })
      }

      // Register basic callbacks
      controller.registerCallback(object : MediaControllerCompat.Callback() {

        // Last received metadata
        private var previousMetadata: MediaMetadataCompat? = null

        // Previous load
        private var currentLoadTask: AsyncTask<MediaMetadataCompat, Nothing, MediaMetadataCompat>? = null

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
          state?.let {
            //updateNotification(it)
          }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
          // Ignore nulls
          controller.playbackState?.let {
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
                  ThemeManager.getInstance(applicationContext).updateFromBitmap(bitmap)
                }
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

    Log.i(LOG_TAG, "onDestroy()")

    // Deactivate the session
    mMediaSession.run {
      isActive = false
      release()
    }

    // Detach the player from the notification
    mExoNotificationManager.release()

    // Release player and related resources
    mPlayerHolder.release()

    // Close database
    //PlayQueueDatabase.getInstance(applicationContext).close()
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