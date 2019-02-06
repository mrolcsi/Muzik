package hu.mrolcsi.android.lyricsplayer.player

import android.content.ComponentName
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.appcompat.app.AppCompatActivity
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.service.LPBrowserService
import kotlinx.android.synthetic.main.activity_player.*

class PlayerActivity : AppCompatActivity() {

  private lateinit var mMediaBrowser: MediaBrowserCompat

  private val mConnectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
    override fun onConnected() {

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
      ComponentName(this, LPBrowserService::class.java),
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

    // Display the initial state
    val metadata = mediaController.metadata
    val pbState = mediaController.playbackState

    // update music controls
    updateControls(pbState)

    // update song metadata
    updateSongData(metadata)

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
}
