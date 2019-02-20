package hu.mrolcsi.android.lyricsplayer.player

import android.content.ComponentName
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.navArgs
import com.example.android.uamp.media.extensions.album
import com.example.android.uamp.media.extensions.albumArt
import com.example.android.uamp.media.extensions.artist
import com.example.android.uamp.media.extensions.duration
import com.example.android.uamp.media.extensions.title
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.service.LPPlayerService
import kotlinx.android.synthetic.main.activity_player.*

class PlayerActivity : AppCompatActivity() {

  private val args: PlayerActivityArgs by navArgs()

  private lateinit var mMediaBrowser: MediaBrowserCompat
  private var mMediaController: MediaControllerCompat? = null

  private val mConnectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
    override fun onConnected() {

      Log.d(LOG_TAG, "Connected to MediaService.")

      // Get the token for the MediaSession
      mMediaBrowser.sessionToken.also { token ->

        // try to retrieve previous controller
        mMediaController = MediaControllerCompat.getMediaController(this@PlayerActivity)
        if (mMediaController == null) {
          // Create a new MediaControllerCompat
          val mediaController = MediaControllerCompat(this@PlayerActivity, token)

          // Save the controller
          MediaControllerCompat.setMediaController(this@PlayerActivity, mediaController)
        }
      }

      val args = intent?.extras?.let {
        PlayerActivityArgs.fromBundle(it)
      }
      if (args != null) {
        // load song from args
        args.mediaPath?.let {
          val currentMediaId = mediaController.metadata?.description?.mediaId
          Log.d(LOG_TAG, "Current media: $currentMediaId")
          if (it != currentMediaId) {
            mediaController.transportControls.playFromMediaId(it, null)
          }
        }
      }

      // Finish building the UI
      buildTransportControls()
    }

    override fun onConnectionSuspended() {
      // The Service has crashed. Disable transport controls until it automatically reconnects
      sbSongProgress.isEnabled = false
      btnPrevious.isEnabled = false
      btnPlayPause.isEnabled = false
      btnNext.isEnabled = false
    }

    override fun onConnectionFailed() {
      // The Service has refused our connection
      sbSongProgress.isEnabled = false
      btnPrevious.isEnabled = false
      btnPlayPause.isEnabled = false
      btnNext.isEnabled = false
    }
  }

  private var controllerCallback = object : MediaControllerCompat.Callback() {

    override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
      metadata?.let { updateSongData(metadata) }
    }

    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
      state?.let { updateControls(state) }
    }
  }

  //region LIFECYCLE

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_player)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    // Create MediaBrowserServiceCompat
    mMediaBrowser = MediaBrowserCompat(
      this,
      ComponentName(this, LPPlayerService::class.java),
      mConnectionCallbacks,
      null // optional Bundle
    )
  }

  public override fun onStart() {
    super.onStart()
    mMediaBrowser.connect()
  }

  public override fun onResume() {
    super.onResume()
    volumeControlStream = AudioManager.STREAM_MUSIC
  }

  public override fun onStop() {
    super.onStop()
    // (see "stay in sync with the MediaSession")
    MediaControllerCompat.getMediaController(this)?.unregisterCallback(controllerCallback)
    mMediaBrowser.disconnect()
  }

  //endregion

  fun buildTransportControls() {
    val mediaController = MediaControllerCompat.getMediaController(this@PlayerActivity)

    // Enable controls
    sbSongProgress.isEnabled = true
    btnPrevious.isEnabled = true
    btnPlayPause.isEnabled = true
    btnNext.isEnabled = true

    // update music controls
    val pbState = mediaController.playbackState
    if (pbState != null) {
      updateControls(pbState)
    }

    // update song metadata
    val metadata = mediaController.metadata
    if (metadata != null) {
      updateSongData(metadata)
    }

    // Register a Callback to stay in sync
    mediaController.registerCallback(controllerCallback)
  }

  private fun updateControls(playbackState: PlaybackStateCompat) {
    // Update progress
    val elapsedTime = playbackState.position / 1000
    val remainingTime = sbSongProgress.max - elapsedTime

    tvElapsedTime.text = elapsedTime.toString()
    tvRemainingTime.text = "-$remainingTime"

    sbSongProgress.progress = elapsedTime.toInt()

    // Update controls
    val mediaController = MediaControllerCompat.getMediaController(this)

    btnPlayPause.apply {
      if (playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
        setOnClickListener {
          mediaController.transportControls.pause()
        }
        setImageResource(android.R.drawable.ic_media_pause)
      } else {
        setOnClickListener {
          mediaController.transportControls.play()
        }
        setImageResource(android.R.drawable.ic_media_play)
      }
    }
  }

  private fun updateSongData(metadata: MediaMetadataCompat) {
    tvAlbum.text = metadata.album
    tvArtist.text = metadata.artist
    tvTitle.text = metadata.title

    imgCoverArt.setImageBitmap(metadata.albumArt)

    sbSongProgress.max = (metadata.duration / 1000).toInt()
  }

  companion object {
    const val LOG_TAG = "PlayerActivity"
  }
}
