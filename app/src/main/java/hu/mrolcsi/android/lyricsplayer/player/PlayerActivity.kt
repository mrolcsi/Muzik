package hu.mrolcsi.android.lyricsplayer.player

import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.album
import hu.mrolcsi.android.lyricsplayer.extensions.albumArt
import hu.mrolcsi.android.lyricsplayer.extensions.artist
import hu.mrolcsi.android.lyricsplayer.extensions.duration
import hu.mrolcsi.android.lyricsplayer.extensions.title
import kotlinx.android.synthetic.main.activity_player.*

class PlayerActivity : AppCompatActivity() {

  private lateinit var mPlayerModel: PlayerViewModel

  //region LIFECYCLE

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_player)
    setupToolbar()

    // Disable controls
    sbSongProgress.isEnabled = false
    btnPrevious.isEnabled = false
    btnPlayPause.isEnabled = false
    btnNext.isEnabled = false

    // Observe changes through ViewModel
    mPlayerModel = ViewModelProviders.of(this).get(PlayerViewModel::class.java).apply {
      currentMediaMetadata.observe(this@PlayerActivity, Observer { metadata ->
        metadata?.let { updateSongData(metadata) }
      })
      currentPlaybackState.observe(this@PlayerActivity, Observer { state ->
        state?.let { updateControls(state) }
      })
      mediaController.observe(this@PlayerActivity, Observer { controller ->
        controller?.let {
          MediaControllerCompat.setMediaController(this@PlayerActivity, controller)

//          val args = intent?.extras?.let {
//            PlayerActivityArgs.fromBundle(it)
//          }
//          if (args != null) {
//            // load song from args
//            args.mediaPath?.let {
//              val currentMediaId = controller.metadata?.description?.mediaId
//              Log.d(LOG_TAG, "Current media: $currentMediaId")
//              if (it != currentMediaId) {
//                controller.transportControls.playFromMediaId(it, null)
//              }
//            }
//          }

          // Finish building the UI
          setupTransportControls()
        }
      })
    }
  }

  override fun onStart() {
    super.onStart()
    mPlayerModel.connect()
  }

  public override fun onResume() {
    super.onResume()
    volumeControlStream = AudioManager.STREAM_MUSIC
  }

  override fun onStop() {
    super.onStop()
    mPlayerModel.disconnect()
  }

  //endregion

  private fun setupToolbar() {
    setSupportActionBar(playerToolbar)
    // show home button
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    // hide title
    supportActionBar?.setDisplayShowTitleEnabled(false)
  }

  private fun setupTransportControls() {
    val mediaController = MediaControllerCompat.getMediaController(this@PlayerActivity)

    // Enable controls
    sbSongProgress.isEnabled = true
    btnPrevious.isEnabled = true
    btnPlayPause.isEnabled = true
    btnNext.isEnabled = true

    // update song metadata
    val metadata = mediaController.metadata
    if (metadata != null) {
      updateSongData(metadata)
    }

    // update music controls
    val pbState = mediaController.playbackState
    if (pbState != null) {
      updateControls(pbState)
    }
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
