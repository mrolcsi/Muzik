package hu.mrolcsi.muzik.data.service.media

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import androidx.navigation.NavDeepLinkBuilder
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.squareup.picasso.Picasso
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.data.local.MuzikDatabase
import hu.mrolcsi.muzik.data.model.media.albumArtUri
import hu.mrolcsi.muzik.data.model.media.isSkipToNextEnabled
import hu.mrolcsi.muzik.data.model.media.prepareFromDescriptions
import hu.mrolcsi.muzik.data.model.media.setQueueTitle
import hu.mrolcsi.muzik.data.model.media.setShuffleMode
import hu.mrolcsi.muzik.data.model.playQueue.LastPlayed
import hu.mrolcsi.muzik.data.service.media.exoplayer.ExoPlayerHolder
import hu.mrolcsi.muzik.data.service.media.exoplayer.notification.ExoNotificationManager
import hu.mrolcsi.muzik.data.service.theme.ThemeService
import hu.mrolcsi.muzik.ui.common.into
import org.koin.android.ext.android.inject
import timber.log.Timber

class MuzikPlayerService : MuzikBrowserService() {

  private val themeService: ThemeService by inject()

  // MediaSession and Player implementations
  private lateinit var mMediaSession: MediaSessionCompat
  private lateinit var mPlayerHolder: ExoPlayerHolder

  // Last played position
  private var mLastPlayed: LastPlayed? = null

  // ExoPlayer Notification
  private lateinit var mExoNotificationManager: ExoNotificationManager
  private var mIsForeground = false

  @SuppressLint("WrongConstant")
  override fun onCreate() {
    super.onCreate()

    Timber.i("onCreate()")

    // Create a MediaSessionCompat
    mMediaSession = MediaSessionCompat(this, "MuzikPlayerService").apply {
      // Prepare Pending Intent to Player
      val playerPendingIntent = NavDeepLinkBuilder(this@MuzikPlayerService)
        .setGraph(R.navigation.main_navigation)
        .setDestination(R.id.navPlayer)
        .createPendingIntent()
      setSessionActivity(playerPendingIntent)

      // Set the session's token so that client activities can communicate with it.
      setSessionToken(sessionToken)

      // Enable callbacks from MediaButtons and TransportControls
      setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)

      // Connect this session with the ExoPlayer
      mPlayerHolder = ExoPlayerHolder(applicationContext, this).also { exo ->
        mExoNotificationManager = ExoNotificationManager(
          applicationContext,
          this,
          exo.getPlayer(),
          object : PlayerNotificationManager.NotificationListener {

            override fun onNotificationPosted(notificationId: Int, notification: Notification?, ongoing: Boolean) {
              if (exo.getPlayer().playWhenReady && !mIsForeground) {
                startService(
                  Intent(
                    applicationContext,
                    MuzikPlayerService::class.java
                  )
                )
                startForeground(notificationId, notification)
                mIsForeground = true
              }

              if (!exo.getPlayer().playWhenReady && mIsForeground) {
                stopForeground(false)
                mIsForeground = false
              }
            }

            override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
              stopForeground(true)
              stopSelf()
              mIsForeground = false
            }
          })

        exo.getPlayer().addListener(object : Player.EventListener {
          override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Timber.v("onPlayerStateChanged(playWhenReady=$playWhenReady, playbackState=$playbackState)")

            // Make notification dismissible
            if (!playWhenReady && mIsForeground) {
              stopForeground(false)
              mIsForeground = false
            }

            when (playbackState) {
              Player.STATE_READY -> {
                // Set player to last played settings
                mLastPlayed?.let { lastPlayed ->
                  Timber.d("Loaded 'Last Played' from database: $lastPlayed")

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

          override fun onPlayerError(error: ExoPlaybackException?) {
            val player = exo.getPlayer()

            if (controller.playbackState.isSkipToNextEnabled && player.playWhenReady) {
              // Skip to next track
              controller.transportControls.skipToNext()
              controller.transportControls.prepare()
              controller.transportControls.play()
            } else {
              // Send error to client
            }
          }
        })
      }

      // Register basic callbacks
      controller.registerCallback(object : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {}

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
          Picasso.get()
            .load(metadata?.albumArtUri)
            .into { bitmap, _ ->
              themeService.updateTheme(bitmap)
            }
        }
      })

      // Check permissions before proceeding
      if (ContextCompat.checkSelfPermission(
          applicationContext,
          Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
      ) {
        AsyncTask.execute {
          // Get last played queue from the database
          val queue = MuzikDatabase.getInstance(applicationContext)
            .getPlayQueueDao()
            .getQueue()
            .map { it.toDescription() }

          Timber.d("Loaded queue from database: $queue")

          if (queue.isNotEmpty()) {
            // Get last played positions from the database
            mLastPlayed = MuzikDatabase.getInstance(applicationContext)
              .getPlayQueueDao()
              .getLastPlayed()

            mLastPlayed?.let { lastPlayed ->
              // Load last played songs (starting with last played position)
              val queuePosition = if (lastPlayed.queuePosition in queue.indices) lastPlayed.queuePosition else 0
              controller.prepareFromDescriptions(queue, queuePosition)

              controller.transportControls.setRepeatMode(lastPlayed.repeatMode)
              controller.transportControls.setShuffleMode(lastPlayed.shuffleMode, lastPlayed.shuffleSeed)
              controller.setQueueTitle(lastPlayed.queueTitle)
            }
          }
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

    Timber.i("onDestroy()")

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
    //MuzikDatabase.getInstance(applicationContext).close()
  }
}