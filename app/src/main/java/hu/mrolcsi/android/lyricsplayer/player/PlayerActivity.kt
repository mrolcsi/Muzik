package hu.mrolcsi.android.lyricsplayer.player

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
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
import hu.mrolcsi.android.lyricsplayer.theme.Theme
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.activity_player.view.*
import kotlin.math.roundToInt

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

    // Apply changes in theme on-the-fly
    ThemeManager.currentTheme.observe(this@PlayerActivity, Observer { theme ->
      applyTheme(theme)
    })
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

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    return when (item?.itemId) {
      android.R.id.home -> {
        onBackPressed()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onBackPressed() {
    // Respond to the action bar's Up/Home button
    val upIntent: Intent? = NavUtils.getParentActivityIntent(this)

    // Prepare options for Shared Element Transition
    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
      this,
      Pair.create(imgCoverArt, ViewCompat.getTransitionName(imgCoverArt))
    )

    when {
      upIntent == null -> throw IllegalStateException("No Parent Activity Intent")
      NavUtils.shouldUpRecreateTask(this, upIntent) -> {
        Log.d(LOG_TAG, "Navigate back creating new stack.")
        // This activity is NOT part of this app's task, so create a new task
        // when navigating up, with a synthesized back stack.
        TaskStackBuilder.create(this)
          // Add all of this activity's parents to the back stack
          .addNextIntentWithParentStack(upIntent)
          // Navigate up to the closest parent
          .startActivities(options.toBundle())
      }
      else -> {
        Log.d(LOG_TAG, "Navigate back directly.")
        // TODO: Create Shared Element Transition
        // This activity is part of this app's task, so simply
        // navigate up to the logical parent activity.
        //NavUtils.navigateUpTo(this, upIntent)
        finishAfterTransition()
      }
    }
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

  private fun applyTheme(theme: Theme) {
    val animationDuration: Long = 500

    // Background Color
    ValueAnimator.ofArgb(
      ThemeManager.previousTheme?.backgroundColor ?: ContextCompat.getColor(this, R.color.backgroundColor),
      theme.backgroundColor
    ).apply {
      duration = animationDuration  // milliseconds
      addUpdateListener {
        val color = it.animatedValue as Int
        applyBackgroundColor(color)
      }
      start()
    }

    // Text Color
    ValueAnimator.ofArgb(
      ThemeManager.previousTheme?.foregroundColor ?: Color.WHITE,
      theme.foregroundColor
    ).apply {
      duration = animationDuration  // milliseconds
      addUpdateListener {
        val color = it.animatedValue as Int

        applyTextColor(color)
      }
      start()
    }

    // TODO: applyControlColors(swatch.titleTextColor)

    // Test out colors
    view1.setBackgroundColor(theme.palette.getLightVibrantColor(Color.BLACK))
    view2.setBackgroundColor(theme.palette.getVibrantColor(Color.BLACK))
    view3.setBackgroundColor(theme.palette.getDarkVibrantColor(Color.BLACK))
    view4.setBackgroundColor(theme.palette.getDominantColor(Color.BLACK))
    view5.setBackgroundColor(theme.palette.getLightMutedColor(Color.BLACK))
    view6.setBackgroundColor(theme.palette.getMutedColor(Color.BLACK))
    view7.setBackgroundColor(theme.palette.getDarkMutedColor(Color.BLACK))
  }

  private fun applyTextColor(color: Int) {
    // Toolbar
    playerToolbar.navigationIcon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)

    // Texts
    tvTitle.setTextColor(color)
    tvArtist.setTextColor(color)
    tvAlbum.setTextColor(color)
    tvElapsedTime.setTextColor(color)
    tvRemainingTime.setTextColor(color)
    tvSeekProgress.setTextColor(color)
  }

  private fun applyBackgroundColor(color: Int) {
    // Window background
    playerRoot.setBackgroundColor(color)

    // Status bar and navigation bar
    window.apply {
      statusBarColor = color
      navigationBarColor = color
      //          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      //            window?.decorView?.apply {
      //              val systemUiFlags = systemUiVisibility
      //              systemUiVisibility =
      //                if (ColorUtils.calculateLuminance(it.animatedValue as Int) < 0.5) {
      //                  systemUiFlags or
      //                      -View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or -View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
      //                } else {
      //                  systemUiFlags or
      //                      View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
      //                }
      //            }
      //        }
    }

    // Seek Progress background
    tvSeekProgress.setBackgroundColor(
      Color.argb(
        (255 * 0.5).roundToInt(),
        Color.red(color),
        Color.green(color),
        Color.blue(color)
      )
    )
  }

  companion object {
    const val LOG_TAG = "PlayerActivity"
  }
}
