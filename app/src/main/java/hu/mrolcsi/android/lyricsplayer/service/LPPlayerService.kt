package hu.mrolcsi.android.lyricsplayer.service

import android.app.PendingIntent
import android.content.Intent
import android.os.AsyncTask
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.os.bundleOf
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.Player
import hu.mrolcsi.android.lyricsplayer.database.playqueue.PlayQueueDatabase
import hu.mrolcsi.android.lyricsplayer.extensions.media.addQueueItems
import hu.mrolcsi.android.lyricsplayer.extensions.media.albumArt
import hu.mrolcsi.android.lyricsplayer.extensions.media.from
import hu.mrolcsi.android.lyricsplayer.extensions.media.prepareFromDescription
import hu.mrolcsi.android.lyricsplayer.player.PlayerActivity
import hu.mrolcsi.android.lyricsplayer.service.exoplayer.ExoPlayerHolder
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager

class LPPlayerService : LPBrowserService() {

  // Indicates if the service is running in the foreground
  private var isForegroundService = false

  // MediaSession and Player implementations
  private lateinit var mMediaSession: MediaSessionCompat
  private lateinit var mPlayerHolder: ExoPlayerHolder

  // ExoPlayer Notification
  //private lateinit var mExoNotificationManager: ExoNotificationManager
  //private var mNotificationId: Int = 0
  //private var mNotification: Notification? = null

  // Custom built Notification
  private lateinit var mNotificationBuilder: LPNotificationBuilder

  override fun onCreate() {
    super.onCreate()

    Log.d(LOG_TAG, "onCreate()")

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
//            if (mNotification != null) {
//              when (playWhenReady) {
//                // Enable swipe-to-dismiss on the notification
//                false -> stopForeground(false)
//                // Lock notification again
//                true -> startForeground(mNotificationId, mNotification)
//              }
//            }
          }
        })
      }

      //region -- NOTIFICATION WITH EXO PLAYER --

      // Prepare notification
//      mExoNotificationManager = ExoNotificationManager(
//        applicationContext,
//        this,
//        mPlayerHolder.getPlayer(),
//        object : PlayerNotificationManager.NotificationListener {
//          override fun onNotificationStarted(notificationId: Int, notification: Notification?) {
//            // Cache the id and the created notification for later use
//            mNotificationId = notificationId
//            mNotification = notification
//
//            // Start service
//            if (!isForegroundService) {
//              startService(Intent(applicationContext, this@LPPlayerService.javaClass))
//              startForeground(notificationId, notification)
//              isForegroundService = true
//            } else if (notification != null) {
//              NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
//            }
//          }
//
//          override fun onNotificationCancelled(notificationId: Int) {
//            if (isForegroundService) {
//              stopForeground(false)
//              isForegroundService = false
//
//              // If playback has ended, also stop the service.
//              stopSelf()
//              stopForeground(true)
//            }
//          }
//        }
//      )

      //endregion

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

              // Prepare oonPostExecute callback
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

        private fun updateNotification(playbackState: PlaybackStateCompat) {
          val metadata = controller.metadata
          if (metadata == null) {
            Log.w(LOG_TAG, "MediaMetadata is null!")
            return
          }

          // Skip building a notification when state is "none".
          val notification = if (playbackState.state != PlaybackStateCompat.STATE_NONE) {
            mNotificationBuilder.buildNotification(sessionToken)
          } else {
            null
          }

          when (playbackState.state) {
            PlaybackStateCompat.STATE_PLAYING -> {
              if (!isForegroundService) {
                startService(Intent(applicationContext, this@LPPlayerService.javaClass))
                startForeground(LPNotificationBuilder.NOTIFICATION_ID, notification)
                isForegroundService = true
              } else if (notification != null) {
                NotificationManagerCompat.from(applicationContext)
                  .notify(LPNotificationBuilder.NOTIFICATION_ID, notification)
              }
            }
            else -> {
              if (isForegroundService) {
                stopForeground(false)
                isForegroundService = false

                // If playback has ended, also stop the service.
                when (playbackState.state) {
                  PlaybackStateCompat.STATE_NONE,
                  PlaybackStateCompat.STATE_STOPPED -> stopSelf()
                }

                if (notification != null) {
                  NotificationManagerCompat.from(applicationContext)
                    .notify(LPNotificationBuilder.NOTIFICATION_ID, notification)
                } else {
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

        if (queue.isNotEmpty()) {
          // Get last played positions from the database
          val lastPlayed = PlayQueueDatabase.getInstance(applicationContext)
            .getPlayQueueDao()
            .getLastPlayed()

          lastPlayed?.let {
            // Load last played song
            controller.transportControls.prepareFromDescription(
              queue[lastPlayed.queuePosition],
              bundleOf(ExoPlayerHolder.EXTRA_DESIRED_QUEUE_POSITION to lastPlayed.queuePosition)
            )
            // Add the other songs to the queue
            controller.addQueueItems(queue.filterIndexed { index, _ -> index != lastPlayed.queuePosition })
            // Seek to saved position
            controller.transportControls.seekTo(lastPlayed.trackPosition)
          }
        }
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    MediaButtonReceiver.handleIntent(mMediaSession, intent)
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {

    // Deactivate the session
    mMediaSession.run {
      isActive = false
      release()
    }

    // Detach the player from the notification
    //mExoNotificationManager.release()

    // Release player and related resources
    mPlayerHolder.release()

    // Remove notification
    NotificationManagerCompat.from(this).cancel(LPNotificationBuilder.NOTIFICATION_ID)

    super.onDestroy()
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