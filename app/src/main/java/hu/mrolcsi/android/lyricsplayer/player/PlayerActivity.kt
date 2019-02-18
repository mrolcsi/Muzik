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
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.service.LPPlayerService
import kotlinx.android.synthetic.main.activity_player.*

class PlayerActivity : AppCompatActivity() {

  private val args: PlayerActivityArgs by navArgs()

  private lateinit var mMediaBrowser: MediaBrowserCompat

  private val mConnectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
    override fun onConnected() {

      Log.d(LOG_TAG, "Connected to MediaService.")

      // Get the token for the MediaSession
      mMediaBrowser.sessionToken.also { token ->

        // Create a MediaControllerCompat
        val mediaController = MediaControllerCompat(
          this@PlayerActivity, // Context
          token
        )

        // Save the controller
        MediaControllerCompat.setMediaController(this@PlayerActivity, mediaController)
      }

      args.mediaPath?.let {
        mediaController.transportControls.playFromMediaId(it, null)
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
    val mediaController = MediaControllerCompat.getMediaController(this)

    // Grab the view for the play/pause button
    btnPlayPause.apply {
      setOnClickListener {
        // Since this is a play/pause button, you'll need to test the current state
        // and choose the action accordingly

        if (playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
          mediaController.transportControls.pause()
        } else {
          mediaController.transportControls.play()
        }
      }
    }
  }

  private fun updateSongData(metadata: MediaMetadataCompat) {
    tvAlbum.text = metadata.getText(MediaMetadataCompat.METADATA_KEY_ALBUM)
    tvArtist.text = metadata.getText(MediaMetadataCompat.METADATA_KEY_ARTIST)
    tvTitle.text = metadata.getText(MediaMetadataCompat.METADATA_KEY_TITLE)
  }

  companion object {
    const val LOG_TAG = "PlayerActivity"
  }
}
