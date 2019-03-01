package hu.mrolcsi.android.lyricsplayer.player

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.LayerDrawable
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.palette.graphics.Palette
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.album
import hu.mrolcsi.android.lyricsplayer.extensions.albumArt
import hu.mrolcsi.android.lyricsplayer.extensions.applyColorToNavigationBarIcons
import hu.mrolcsi.android.lyricsplayer.extensions.applyColorToStatusBarIcons
import hu.mrolcsi.android.lyricsplayer.extensions.artist
import hu.mrolcsi.android.lyricsplayer.extensions.duration
import hu.mrolcsi.android.lyricsplayer.extensions.isPlaying
import hu.mrolcsi.android.lyricsplayer.extensions.isSkipToNextEnabled
import hu.mrolcsi.android.lyricsplayer.extensions.isSkipToPreviousEnabled
import hu.mrolcsi.android.lyricsplayer.extensions.secondsToTimeStamp
import hu.mrolcsi.android.lyricsplayer.extensions.startProgressUpdater
import hu.mrolcsi.android.lyricsplayer.extensions.stopProgressUpdater
import hu.mrolcsi.android.lyricsplayer.extensions.title
import hu.mrolcsi.android.lyricsplayer.theme.Theme
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.activity_player.view.*
import kotlin.math.roundToInt

class PlayerActivity : AppCompatActivity() {

  private lateinit var mPlayerModel: PlayerViewModel

  private var mUserIsSeeking = false

  // Prepare drawables
  private val mPreviousDrawable by lazy { getDrawable(R.drawable.media_previous) as LayerDrawable }
  private val mPlayDrawable by lazy { getDrawable(R.drawable.media_play) as LayerDrawable }
  private val mPauseDrawable by lazy { getDrawable(R.drawable.media_pause) as LayerDrawable }
  private val mNextDrawable by lazy { getDrawable(R.drawable.media_next) as LayerDrawable }

  // Glide transition
  private var mCovertArtIndex = 0
  private val imgCoverArt by lazy { arrayOf(imgCoverArt0, imgCoverArt1) }
  private var mCoverInAnimation = android.R.anim.fade_in
  private var mCoverOutAnimation = android.R.anim.fade_out

  //region LIFECYCLE

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_player)
    setupToolbar()

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
          // Apply MediaController to this Activity
          MediaControllerCompat.setMediaController(this@PlayerActivity, controller)

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

    // Apply StatusBar and NavigationBar colors again
    applyColorToStatusBarIcons(ThemeManager.currentTheme.value?.backgroundColor ?: Color.BLACK)
    applyColorToNavigationBarIcons(ThemeManager.currentTheme.value?.backgroundColor ?: Color.BLACK)
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
      Pair.create(imgCoverArt[mCovertArtIndex], ViewCompat.getTransitionName(imgCoverArt[mCovertArtIndex]))
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

    // Set icons
    btnPrevious.setImageDrawable(mPreviousDrawable)
    btnNext.setImageDrawable(mNextDrawable)

    // Enable controls
    sbSongProgress.isEnabled = true
    btnPrevious.isEnabled = true
    btnPlayPause.isEnabled = true
    btnNext.isEnabled = true

    // Update song metadata
    val metadata = mediaController.metadata
    if (metadata != null) {
      updateSongData(metadata)
    }

    // Update music controls
    val pbState = mediaController.playbackState
    if (pbState != null) {
      updateControls(pbState)
    }

    // Setup listeners

    btnPrevious.setOnClickListener {
      val controller = MediaControllerCompat.getMediaController(this@PlayerActivity)
      if (sbSongProgress.progress > 5) {
        // restart the song
        controller.transportControls.seekTo(0)
      } else {
        mCoverInAnimation = R.anim.slide_in_right
        mCoverOutAnimation = R.anim.slide_out_right
        controller.transportControls.skipToPrevious()
      }
    }

    btnNext.setOnClickListener {
      val controller = MediaControllerCompat.getMediaController(this@PlayerActivity)
      mCoverInAnimation = R.anim.slide_in_left
      mCoverOutAnimation = R.anim.slide_out_left
      controller.transportControls.skipToNext()
    }

    btnPlayPause.apply {
      setOnClickListener {
        val controller = MediaControllerCompat.getMediaController(this@PlayerActivity)
        when (controller.playbackState.state) {
          PlaybackStateCompat.STATE_PLAYING -> {
            // Pause playback, stop updater
            controller.transportControls.pause()
            controller.transportControls.startProgressUpdater()
            btnPlayPause.setImageDrawable(mPlayDrawable)
          }
          PlaybackStateCompat.STATE_PAUSED,
          PlaybackStateCompat.STATE_STOPPED -> {
            // Start playback, start updater
            controller.transportControls.play()
            controller.transportControls.stopProgressUpdater()
            btnPlayPause.setImageDrawable(mPauseDrawable)
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

    btnPrevious.isEnabled = playbackState.isSkipToPreviousEnabled
    btnNext.isEnabled = playbackState.isSkipToNextEnabled

    when (playbackState.isPlaying) {
      true -> {
        controller?.transportControls?.startProgressUpdater()
        btnPlayPause.setImageDrawable(mPauseDrawable)
      }
      false -> {
        controller?.transportControls?.stopProgressUpdater()
        btnPlayPause.setImageDrawable(mPlayDrawable)
      }
    }
  }

  private fun updateSongData(metadata: MediaMetadataCompat) {
    tvAlbum.text = metadata.album
    tvArtist.text = metadata.artist
    tvTitle.text = metadata.title

    // slide out current imgView
    AnimationUtils.loadAnimation(this, mCoverOutAnimation).also {
      imgCoverArt[mCovertArtIndex].startAnimation(it)
    }

    // currentIndex = currentIndex + 1 rem 2
    mCovertArtIndex = (mCovertArtIndex + 1).rem(2)

    // set image to next imgView
    imgCoverArt[mCovertArtIndex].setImageBitmap(metadata.albumArt)

    // slide in current imgView
    AnimationUtils.loadAnimation(this, mCoverInAnimation).also {
      it.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationRepeat(animation: Animation?) {}

        override fun onAnimationEnd(animation: Animation?) {}

        override fun onAnimationStart(animation: Animation?) {
          imgCoverArt[mCovertArtIndex].bringToFront()
        }
      })
      imgCoverArt[mCovertArtIndex].startAnimation(it)
    }

    if (metadata.albumArt != null) {
      // Upper 10% of Cover Art
      Palette.from(metadata.albumArt)
        .clearFilters()
        .setRegion(
          0,
          0,
          metadata.albumArt.width,
          TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24F, resources.displayMetrics).toInt()
        ).generate {
          val color = it?.dominantSwatch?.rgb ?: Color.BLACK
          window?.statusBarColor = color
          applyColorToStatusBarIcons(color)
        }
    }

    sbSongProgress.max = (metadata.duration / 1000).toInt()
  }

  private fun applyTheme(theme: Theme) {
    Log.d(LOG_TAG, "Applying theme...")

    applyColorToNavigationBarIcons(theme.backgroundColor)

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

    // Foreground Color
    ValueAnimator.ofArgb(
      ThemeManager.previousTheme?.foregroundColor ?: Color.WHITE,
      theme.foregroundColor
    ).apply {
      duration = animationDuration  // milliseconds
      addUpdateListener {
        val color = it.animatedValue as Int

        applyForegroundColor(color)
      }
      start()
    }

    // Test out colors
    view1.setBackgroundColor(theme.palette.getLightVibrantColor(Color.BLACK))
    view2.setBackgroundColor(theme.palette.getVibrantColor(Color.BLACK))
    view3.setBackgroundColor(theme.palette.getDarkVibrantColor(Color.BLACK))
    view4.setBackgroundColor(theme.palette.getDominantColor(Color.BLACK))
    view5.setBackgroundColor(theme.palette.getLightMutedColor(Color.BLACK))
    view6.setBackgroundColor(theme.palette.getMutedColor(Color.BLACK))
    view7.setBackgroundColor(theme.palette.getDarkMutedColor(Color.BLACK))
  }

  private fun applyForegroundColor(color: Int) {
    // Toolbar
    playerToolbar.navigationIcon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)

    // Texts
    tvTitle.setTextColor(color)
    tvArtist.setTextColor(color)
    tvAlbum.setTextColor(color)
    tvElapsedTime.setTextColor(color)
    tvRemainingTime.setTextColor(color)
    tvSeekProgress.setTextColor(color)

    // SeekBar
    sbSongProgress.progressDrawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    sbSongProgress.thumb.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)

    // Media Buttons Background
    mPreviousDrawable.getDrawable(0).setTint(color)
    mPlayDrawable.getDrawable(0).setTint(color)
    mPauseDrawable.getDrawable(0).setTint(color)
    mNextDrawable.getDrawable(0).setTint(color)

    // Media Buttons Ripple (need to use separate drawables)
    val rippleColor = ColorUtils.setAlphaComponent(color, (255 * 0.5).roundToInt())
    btnPrevious.background = Theme.getRippleDrawable(rippleColor)
    btnPlayPause.background = Theme.getRippleDrawable(rippleColor)
    btnNext.background = Theme.getRippleDrawable(rippleColor)
  }

  private fun applyBackgroundColor(color: Int) {
    // Window background
    playerRoot.setBackgroundColor(color)

    // Navigation Bar
    window.apply {
      navigationBarColor = color
    }

    // Seek Progress background
    tvSeekProgress.setBackgroundColor(ColorUtils.setAlphaComponent(color, (255 * 0.5).roundToInt()))

    // Media Buttons Icon
    mPreviousDrawable.getDrawable(1).setTint(color)
    mPlayDrawable.getDrawable(1).setTint(color)
    mPauseDrawable.getDrawable(1).setTint(color)
    mNextDrawable.getDrawable(1).setTint(color)
  }

  companion object {
    const val LOG_TAG = "PlayerActivity"
  }
}
