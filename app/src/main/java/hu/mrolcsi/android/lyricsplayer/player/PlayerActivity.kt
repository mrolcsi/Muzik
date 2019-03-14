package hu.mrolcsi.android.lyricsplayer.player

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.AudioManager
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.GravityCompat
import androidx.core.view.forEach
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.common.pager.RVPageScrollState
import hu.mrolcsi.android.lyricsplayer.common.pager.RVPagerSnapHelperListenable
import hu.mrolcsi.android.lyricsplayer.common.pager.RVPagerStateListener
import hu.mrolcsi.android.lyricsplayer.common.pager.VisiblePageState
import hu.mrolcsi.android.lyricsplayer.database.playqueue.PlayQueueDatabase
import hu.mrolcsi.android.lyricsplayer.extensions.applyColorToNavigationBarIcons
import hu.mrolcsi.android.lyricsplayer.extensions.applyColorToStatusBarIcons
import hu.mrolcsi.android.lyricsplayer.extensions.media.albumArt
import hu.mrolcsi.android.lyricsplayer.extensions.media.duration
import hu.mrolcsi.android.lyricsplayer.extensions.media.isPlaying
import hu.mrolcsi.android.lyricsplayer.extensions.media.isSkipToNextEnabled
import hu.mrolcsi.android.lyricsplayer.extensions.media.isSkipToPreviousEnabled
import hu.mrolcsi.android.lyricsplayer.extensions.media.startProgressUpdater
import hu.mrolcsi.android.lyricsplayer.extensions.media.stopProgressUpdater
import hu.mrolcsi.android.lyricsplayer.extensions.secondsToTimeStamp
import hu.mrolcsi.android.lyricsplayer.theme.Theme
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.content_player.*

class PlayerActivity : AppCompatActivity() {

  private lateinit var mPlayerModel: PlayerViewModel

  private var mUserIsSeeking = false

  // Prepare drawables (separate for each button)
  private val mPreviousBackground by lazy { getDrawable(R.drawable.media_button_background) }
  private val mPlayPauseBackground by lazy { getDrawable(R.drawable.media_button_background) }
  private val mNextBackground by lazy { getDrawable(R.drawable.media_button_background) }

  private val mQueueAdapter = QueueAdapter().apply {
    setHasStableIds(true)
  }

  //region LIFECYCLE

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_player)
    setupToolbar()
    setupPager()

    //supportPostponeEnterTransition()

    // Observe changes through ViewModel
    mPlayerModel = ViewModelProviders.of(this).get(PlayerViewModel::class.java).apply {
      Log.d(LOG_TAG, "Got PlayerViewModel: $this")

      currentMediaMetadata.observe(this@PlayerActivity, Observer { metadata ->
        metadata?.let {
          updateSongData(metadata)
        }
      })
      currentPlaybackState.observe(this@PlayerActivity, Observer { state ->
        state?.let {
          updateControls(state)

          // Scroll pager to current item
          val layoutManager = (rvQueue.layoutManager as LinearLayoutManager)
          val currentPosition =
            (layoutManager.findLastCompletelyVisibleItemPosition() + layoutManager.findFirstCompletelyVisibleItemPosition()) / 2

          val queuePosition = it.activeQueueItemId.toInt()

          if (currentPosition > -1 && currentPosition != queuePosition) {
//            if (Math.abs(queuePosition - currentPosition) > 1) {
//              rvQueue.scrollToPosition(queuePosition)
//            } else {
//              rvQueue.smoothScrollToPosition(queuePosition)
//            }
          }
        }
      })
      mediaController.observe(this@PlayerActivity, Observer { controller ->
        controller?.let {
          // Apply MediaController to this Activity
          MediaControllerCompat.setMediaController(this@PlayerActivity, controller)

          // Finish building the UI
          setupTransportControls()
        }
      })

//      currentQueue.observe(this@PlayerActivity, Observer {
//        mQueueAdapter.submitList(it)
//      })
    }

    PlayQueueDatabase.getInstance(this)
      .getPlayQueueDao()
      .fetchQueue()
      .observe(this@PlayerActivity, Observer {
        mQueueAdapter.submitList(it)
      })

    // Apply changes in theme on-the-fly
    ThemeManager.currentTheme.observe(this@PlayerActivity, object : Observer<Theme> {

      private var initialLoad = true

      override fun onChanged(theme: Theme) {
        if (initialLoad) {
          applyThemeStatic(theme)
          initialLoad = false
        } else {
          applyThemeAnimated(theme)
        }
      }
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
    ThemeManager.currentTheme.value?.let {
      applyColorToStatusBarIcons(it.primaryBackgroundColor)
      applyColorToNavigationBarIcons(it.primaryBackgroundColor)
    }
  }

  override fun onStop() {
    super.onStop()
    mPlayerModel.disconnect()

    MediaControllerCompat.getMediaController(this)
      .transportControls.stopProgressUpdater()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_player, menu)
    ThemeManager.currentTheme.value?.let { theme ->
      menu?.forEach { item ->
        item.icon.setColorFilter(theme.primaryForegroundColor, PorterDuff.Mode.SRC_IN)
      }
    }
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    return when (item?.itemId) {
      android.R.id.home -> {
        onBackPressed()
        true
      }
      R.id.menuPlaylist -> {
        drawer_layout.openDrawer(GravityCompat.END)
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onBackPressed() {
    // If drawer is open, just close it
    if (drawer_layout.isDrawerOpen(GravityCompat.END)) {
      drawer_layout.closeDrawer(GravityCompat.END)
      return
    }

    // Respond to the action bar's Up/Home button
    val upIntent: Intent? = NavUtils.getParentActivityIntent(this)

    // TODO: Prepare options for Shared Element Transition
//    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
//      this,
//      Pair.create(imgCoverArt, ViewCompat.getTransitionName(imgCoverArt))
//    )

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
          .startActivities(/*options.toBundle()*/)
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

  private fun setupPager() {
    rvQueue.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    rvQueue.adapter = mQueueAdapter

    RVPagerSnapHelperListenable().attachToRecyclerView(rvQueue, object : RVPagerStateListener {

      private var currentIndex = -1
      private var newIndex = -1

      override fun onPageScroll(pagesState: List<VisiblePageState>) {
        // TODO: Blend colors

      }

      override fun onScrollStateChanged(state: RVPageScrollState) {
        Log.v(LOG_TAG, "state=$state, currentIndex=$currentIndex, newIndex=$newIndex")
        if (state is RVPageScrollState.Idle && newIndex > -1) {
          mQueueAdapter.getItemId(newIndex)
          MediaControllerCompat.getMediaController(this@PlayerActivity)
            ?.transportControls
            ?.skipToQueueItem(newIndex.toLong())
        }
      }

      override fun onPageSelected(index: Int) {
        newIndex = index
      }
    })
  }

  private fun setupTransportControls() {
    val mediaController = MediaControllerCompat.getMediaController(this)

    // Set icons
//    btnPrevious.setImageDrawable(mPreviousDrawable)
//    btnNext.setImageDrawable(mNextDrawable)

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
        controller.transportControls.skipToPrevious()
      }
    }

    btnNext.setOnClickListener {
      val controller = MediaControllerCompat.getMediaController(this@PlayerActivity)
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
            //btnPlayPause.setImageResource(mPlayDrawable)
          }
          PlaybackStateCompat.STATE_PAUSED,
          PlaybackStateCompat.STATE_STOPPED -> {
            // Start playback, start updater
            controller.transportControls.play()
            controller.transportControls.stopProgressUpdater()
            //btnPlayPause.setImageDrawable(mPauseDrawable)
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
    btnPrevious.alpha = if (playbackState.isSkipToPreviousEnabled) 1f else 0.5f

    btnNext.isEnabled = playbackState.isSkipToNextEnabled
    btnNext.alpha = if (playbackState.isSkipToNextEnabled) 1f else 0.5f

    when (playbackState.isPlaying) {
      true -> {
        controller?.transportControls?.startProgressUpdater()
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
      }
      false -> {
        controller?.transportControls?.stopProgressUpdater()
        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
      }
    }
  }

  private fun updateSongData(metadata: MediaMetadataCompat) {
    metadata.albumArt?.let { bitmap ->
      AsyncTask.execute {
        // Upper 10% of Cover Art
        Palette.from(bitmap)
          .clearFilters()
          .setRegion(
            0,
            0,
            bitmap.width,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24F, resources.displayMetrics).toInt()
          ).generate { palette ->
            val color = palette?.dominantSwatch?.rgb ?: Color.BLACK
            window?.statusBarColor = color
            applyColorToStatusBarIcons(color)
          }
      }
    }

    sbSongProgress.max = (metadata.duration / 1000).toInt()
  }

  private fun applyThemeStatic(theme: Theme) {
    Log.d(LOG_TAG, "Applying theme (static)...")

    applyBackgroundColor(theme.primaryBackgroundColor)
    applyForegroundColor(theme.primaryForegroundColor)
  }

  private fun applyThemeAnimated(theme: Theme) {
    Log.d(LOG_TAG, "Applying theme (animated)...")

    applyColorToNavigationBarIcons(theme.primaryBackgroundColor)

    // Background Color
    ValueAnimator.ofArgb(
      ThemeManager.previousTheme?.primaryBackgroundColor ?: ContextCompat.getColor(this, R.color.backgroundColor),
      theme.primaryBackgroundColor
    ).apply {
      duration = Theme.PREFERRED_ANIMATION_DURATION
      addUpdateListener {
        val color = it.animatedValue as Int
        applyBackgroundColor(color)
      }
      start()
    }

    // Foreground Color
    ValueAnimator.ofArgb(
      ThemeManager.previousTheme?.primaryForegroundColor ?: Color.WHITE,
      theme.primaryForegroundColor
    ).apply {
      duration = Theme.PREFERRED_ANIMATION_DURATION
      addUpdateListener {
        val color = it.animatedValue as Int

        applyForegroundColor(color)
      }
      start()
    }
  }

  private fun applyForegroundColor(color: Int) {
    // Toolbar Icons
    with(playerToolbar) {
      navigationIcon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
      menu.forEach { item ->
        item.icon.setColorFilter(color, PorterDuff.Mode.SRC_IN)
      }
    }

    // Texts
    tvElapsedTime.setTextColor(color)
    tvRemainingTime.setTextColor(color)
    tvSeekProgress.setTextColor(color)

    // SeekBar
    sbSongProgress.progressDrawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    sbSongProgress.thumb.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)

    // Media Buttons Background
    mPreviousBackground?.setTint(color)
    mPlayPauseBackground?.setTint(color)
    mNextBackground?.setTint(color)
  }

  private fun applyBackgroundColor(color: Int) {
    // Window background
    content_player.setBackgroundColor(color)

    // Navigation Bar
    window.apply {
      navigationBarColor = color
    }

    // Seek Progress background
    tvSeekProgress.setBackgroundColor(ColorUtils.setAlphaComponent(color, Theme.DISABLED_OPACITY))

    // Media Buttons Icon
    btnPrevious.setColorFilter(color)
    btnPlayPause.setColorFilter(color)
    btnNext.setColorFilter(color)

    // Media Buttons Ripple (need to use separate drawables)
    val rippleColor = ColorUtils.setAlphaComponent(color, Theme.DISABLED_OPACITY)
    btnPrevious.background = Theme.getRippleDrawable(rippleColor, mPreviousBackground)
    btnPlayPause.background = Theme.getRippleDrawable(rippleColor, mPlayPauseBackground)
    btnNext.background = Theme.getRippleDrawable(rippleColor, mNextBackground)
  }

  companion object {
    private const val LOG_TAG = "PlayerActivity"
  }
}
