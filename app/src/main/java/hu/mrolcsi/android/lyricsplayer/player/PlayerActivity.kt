package hu.mrolcsi.android.lyricsplayer.player

import android.annotation.SuppressLint
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.album
import hu.mrolcsi.android.lyricsplayer.extensions.albumArt
import hu.mrolcsi.android.lyricsplayer.extensions.artist
import hu.mrolcsi.android.lyricsplayer.extensions.duration
import hu.mrolcsi.android.lyricsplayer.extensions.secondsToTimeStamp
import hu.mrolcsi.android.lyricsplayer.extensions.title
import hu.mrolcsi.android.lyricsplayer.service.LPPlayerService
import kotlinx.android.synthetic.main.activity_player.*

class PlayerActivity : AppCompatActivity() {

  private lateinit var mPlayerModel: PlayerViewModel

  private var mUserIsSeeking = false

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

          val args = intent?.extras?.let {
            PlayerActivityArgs.fromBundle(it)
          }
          if (args != null) {
            // load song from args
            args.mediaPath?.let {
              val currentMediaId = controller.metadata?.description?.mediaId
              Log.d(LOG_TAG, "Current media: $currentMediaId")
              if (it != currentMediaId) {
                controller.transportControls.playFromMediaId(it, null)
              }
            }
            // clear args
            intent?.extras?.clear()
          }

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

    // setup listeners
    btnPlayPause.apply {
      setOnClickListener {
        val controller = MediaControllerCompat.getMediaController(this@PlayerActivity)
        when (controller.playbackState.state) {
          PlaybackStateCompat.STATE_PLAYING -> {
            // Pause playback, stop updater
            controller.transportControls.pause()
            controller.transportControls.sendCustomAction(LPPlayerService.ACTION_STOP_UPDATER, null)
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
          }
          PlaybackStateCompat.STATE_PAUSED,
          PlaybackStateCompat.STATE_STOPPED -> {
            // Start playback, start updater
            controller.transportControls.play()
            controller.transportControls.sendCustomAction(LPPlayerService.ACTION_START_UPDATER, null)
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
          }
        }
      }
    }

    sbSongProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

      private var mProgress = 0

      override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser) {
          mProgress = progress
          tvSeekProgress.text = progress.secondsToTimeStamp()
          tvSeekProgress.visibility = View.VISIBLE
        }
      }

      override fun onStartTrackingTouch(seekBar: SeekBar?) {
        mUserIsSeeking = true
      }

      override fun onStopTrackingTouch(seekBar: SeekBar?) {
        tvSeekProgress.visibility = View.GONE
        mediaController.transportControls.seekTo((mProgress * 1000).toLong())
        mUserIsSeeking = false
      }
    })
  }

  @SuppressLint("SetTextI18n")
  private fun updateControls(playbackState: PlaybackStateCompat) {
    // Update progress
    val elapsedTime = playbackState.position / 1000
    val remainingTime = sbSongProgress.max - elapsedTime

    tvElapsedTime.text = elapsedTime.toInt().secondsToTimeStamp()
    tvRemainingTime.text = "-${remainingTime.toInt().secondsToTimeStamp()}"

    if (!mUserIsSeeking) {
      sbSongProgress.progress = elapsedTime.toInt()
    }

    val controller = MediaControllerCompat.getMediaController(this) ?: null

    when (playbackState.state) {
      PlaybackStateCompat.STATE_PLAYING -> {
        controller?.transportControls?.sendCustomAction(LPPlayerService.ACTION_START_UPDATER, null)
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
      }
      PlaybackStateCompat.STATE_PAUSED,
      PlaybackStateCompat.STATE_STOPPED -> {
        controller?.transportControls?.sendCustomAction(LPPlayerService.ACTION_STOP_UPDATER, null)
        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
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
