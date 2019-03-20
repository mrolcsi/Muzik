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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.util.Pair
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.forEach
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.common.pager.PagerSnapHelperVerbose
import hu.mrolcsi.android.lyricsplayer.common.pager.RVPageScrollState
import hu.mrolcsi.android.lyricsplayer.common.pager.RVPagerSnapHelperListenable
import hu.mrolcsi.android.lyricsplayer.common.pager.RVPagerStateListener
import hu.mrolcsi.android.lyricsplayer.common.pager.VisiblePageState
import hu.mrolcsi.android.lyricsplayer.database.playqueue.PlayQueueDatabase
import hu.mrolcsi.android.lyricsplayer.extensions.applyColorToNavigationBarIcons
import hu.mrolcsi.android.lyricsplayer.extensions.applyColorToStatusBarIcons
import hu.mrolcsi.android.lyricsplayer.extensions.media.duration
import hu.mrolcsi.android.lyricsplayer.extensions.media.isPlaying
import hu.mrolcsi.android.lyricsplayer.extensions.media.isSkipToNextEnabled
import hu.mrolcsi.android.lyricsplayer.extensions.media.isSkipToPreviousEnabled
import hu.mrolcsi.android.lyricsplayer.extensions.media.startProgressUpdater
import hu.mrolcsi.android.lyricsplayer.extensions.media.stopProgressUpdater
import hu.mrolcsi.android.lyricsplayer.extensions.mediaControllerCompat
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
  private lateinit var mSnapHelper: PagerSnapHelperVerbose
  private var mScrollState: RVPageScrollState = RVPageScrollState.IDLE

  //region LIFECYCLE

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_player)
    setupToolbar()
    setupPager()

    supportPostponeEnterTransition()

    // Observe changes through ViewModel
    mPlayerModel = ViewModelProviders.of(this).get(PlayerViewModel::class.java).apply {
      Log.d(LOG_TAG, "Got PlayerViewModel: $this")

      currentMediaMetadata.observe(this@PlayerActivity, Observer { metadata ->
        metadata?.let {
          updateSongData(metadata)

          // Scroll pager to current item
          val layoutManager = (rvQueue.layoutManager as LinearLayoutManager)
          val adapterPosition = mSnapHelper.findSnapPosition(layoutManager)

          // If Metadata has changed, then PlaybackState should have changed as well.
          val queuePosition = mediaControllerCompat.playbackState.activeQueueItemId.toInt()

          Log.d(
            LOG_TAG, "onMetadataChange(" +
                "adapterPosition=$adapterPosition, " +
                "queuePosition=$queuePosition) " +
                "ScrollState=$mScrollState"
          )

          if (mScrollState == RVPageScrollState.IDLE) {
            if (adapterPosition > RecyclerView.NO_POSITION && adapterPosition != queuePosition) {
              if (Math.abs(queuePosition - adapterPosition) > 1) {
                rvQueue.scrollToPosition(queuePosition)
              } else {
                rvQueue.smoothScrollToPosition(queuePosition)
              }
            }
          }
        }
      })
      currentPlaybackState.observe(this@PlayerActivity, Observer { state ->
        state?.let {
          updateControls(state)
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
    }

    ThemeManager.getInstance(this).currentTheme.observe(this, object : Observer<Theme> {

      private var initialLoad = true

      override fun onChanged(it: Theme) {
        if (initialLoad) {
          applyTheme(it, false)
          initialLoad = false
        } else {
          val layoutManager = (rvQueue.layoutManager as LinearLayoutManager)
          val adapterPosition = mSnapHelper.findSnapPosition(layoutManager)

          // If Metadata has changed, then PlaybackState should have changed as well.
          val queuePosition = mediaControllerCompat.playbackState.activeQueueItemId.toInt()

          if (Math.abs(queuePosition - adapterPosition) > 1) {
            applyTheme(it, true)
          }
        }
      }
    })

    // TODO: Use SessionQueue
    PlayQueueDatabase.getInstance(this)
      .getPlayQueueDao()
      .fetchQueue()
      .observe(this@PlayerActivity, Observer {
        mQueueAdapter.submitList(it)
        supportStartPostponedEnterTransition()
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

    mediaControllerCompat.transportControls.stopProgressUpdater()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_player, menu)
    ThemeManager.getInstance(this).currentTheme.value?.let { theme ->
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

    // Prepare options for Shared Element Transition
    val view = mSnapHelper.findSnapView(rvQueue.layoutManager)?.findViewById<ImageView>(R.id.imgCoverArt)
    val options = view?.let {
      ActivityOptionsCompat.makeSceneTransitionAnimation(
        this,
        Pair.create(view, ViewCompat.getTransitionName(view))
      )
    }

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
          .startActivities(options?.toBundle())
      }
      else -> {
        Log.d(LOG_TAG, "Navigate back directly.")
        // TODO: Create Shared Element Transition
        // This activity is part of this app's task, so simply
        // navigate up to the logical parent activity.
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
    rvQueue.apply {
      layoutManager = LinearLayoutManager(this@PlayerActivity, LinearLayoutManager.HORIZONTAL, false)
      adapter = mQueueAdapter
      setHasFixedSize(true)
    }

    mSnapHelper = RVPagerSnapHelperListenable().attachToRecyclerView(rvQueue, object : RVPagerStateListener {

      override fun onPageScroll(pagesState: List<VisiblePageState>) {
        // Blend colors while scrolling
        when (pagesState.size) {
          2 -> {  // between 2 pages -> blend colors
            val leftHolder = rvQueue.findContainingViewHolder(pagesState.first().view) as QueueAdapter.QueueItemHolder
            val rightHolder = rvQueue.findContainingViewHolder(pagesState.last().view) as QueueAdapter.QueueItemHolder

            val leftTheme = leftHolder.usedTheme
            val rightTheme = rightHolder.usedTheme

            if (leftTheme != null && rightTheme != null) {

              val ratio = 1f - pagesState.first().distanceToSettled

              val backgroundColor = ColorUtils.blendARGB(
                leftTheme.primaryBackgroundColor,
                rightTheme.primaryBackgroundColor,
                ratio
              )
              val foregroundColor = ColorUtils.blendARGB(
                leftTheme.primaryForegroundColor,
                rightTheme.primaryForegroundColor,
                ratio
              )

              applyBackgroundColor(backgroundColor)
              applyForegroundColor(foregroundColor)

              val statusBarColor = ColorUtils.blendARGB(
                leftTheme.statusBarColor,
                rightTheme.statusBarColor,
                ratio
              )

              window?.statusBarColor = statusBarColor
              applyColorToStatusBarIcons(statusBarColor)
            }
          }
        }
      }

      override fun onScrollStateChanged(state: RVPageScrollState) {
        mScrollState = state

        if (state == RVPageScrollState.IDLE) {
          // check if item position is different from the now playing position
          val queuePosition = mediaControllerCompat.playbackState.activeQueueItemId.toInt()
          val pagerPosition = mSnapHelper.findSnapPosition(rvQueue.layoutManager)

          if (queuePosition != pagerPosition) {
            mQueueAdapter.getItemId(pagerPosition)
            mediaControllerCompat.transportControls?.skipToQueueItem(pagerPosition.toLong())
          }
        }
      }
    })
  }

  private fun setupTransportControls() {
    // Enable controls
    sbSongProgress.isEnabled = true
    btnPrevious.isEnabled = true
    btnPlayPause.isEnabled = true
    btnNext.isEnabled = true

    // Update song metadata
    mediaControllerCompat.metadata?.let {
      updateSongData(it)
    }

    // Update music controls
    mediaControllerCompat.playbackState?.let {
      updateControls(it)
    }

    // Setup listeners

    btnPrevious.setOnClickListener {
      if (sbSongProgress.progress > 5) {
        // restart the song
        mediaControllerCompat.transportControls?.seekTo(0)
      } else {
        //mediaControllerCompat.transportControls?.skipToPrevious()
        val currentPosition = mSnapHelper.findSnapPosition(rvQueue.layoutManager)
        rvQueue.smoothScrollToPosition(currentPosition - 1)
      }
    }

    btnNext.setOnClickListener {
      //mediaControllerCompat.transportControls?.skipToNext()
      val currentPosition = mSnapHelper.findSnapPosition(rvQueue.layoutManager)
      rvQueue.smoothScrollToPosition(currentPosition + 1)
    }

    btnPlayPause.apply {
      setOnClickListener {
        when (mediaControllerCompat.playbackState.state) {
          PlaybackStateCompat.STATE_PLAYING -> {
            // Pause playback, stop updater
            mediaControllerCompat.transportControls.pause()
            mediaControllerCompat.transportControls.startProgressUpdater()
          }
          PlaybackStateCompat.STATE_PAUSED,
          PlaybackStateCompat.STATE_STOPPED -> {
            // Start playback, start updater
            mediaControllerCompat.transportControls.play()
            mediaControllerCompat.transportControls.stopProgressUpdater()
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


    btnPrevious.isEnabled = playbackState.isSkipToPreviousEnabled
    btnPrevious.alpha = if (playbackState.isSkipToPreviousEnabled) 1f else 0.5f

    btnNext.isEnabled = playbackState.isSkipToNextEnabled
    btnNext.alpha = if (playbackState.isSkipToNextEnabled) 1f else 0.5f

    when (playbackState.isPlaying) {
      true -> {
        mediaControllerCompat.transportControls.startProgressUpdater()
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
      }
      false -> {
        mediaControllerCompat.transportControls.stopProgressUpdater()
        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
      }
    }
  }

  private fun updateSongData(metadata: MediaMetadataCompat) {
    sbSongProgress.max = (metadata.duration / 1000).toInt()
  }

  private fun applyTheme(theme: Theme, animate: Boolean) {
    if (animate) {
      applyThemeAnimated(theme)
    } else {
      applyThemeStatic(theme)
    }
  }

  private fun applyThemeStatic(theme: Theme) {
    Log.i(LOG_TAG, "applyThemeStatic($theme)")

    window?.statusBarColor = theme.statusBarColor
    applyColorToStatusBarIcons(theme.statusBarColor)

    applyBackgroundColor(theme.primaryBackgroundColor)
    applyForegroundColor(theme.primaryForegroundColor)
  }

  private fun applyThemeAnimated(theme: Theme) {
    Log.i(LOG_TAG, "applyingThemeAnimated($theme)")

    val previousTheme = ThemeManager.getInstance(this).previousTheme

    // StatusBar Color
    ValueAnimator.ofArgb(
      previousTheme?.statusBarColor ?: ContextCompat.getColor(this, R.color.backgroundColor),
      theme.statusBarColor
    ).apply {
      duration = Theme.PREFERRED_ANIMATION_DURATION
      addUpdateListener {
        val color = it.animatedValue as Int

        window?.statusBarColor = color
        applyColorToStatusBarIcons(color)
      }
      start()
    }

    // Background Color
    ValueAnimator.ofArgb(
      previousTheme?.primaryBackgroundColor ?: ContextCompat.getColor(this, R.color.backgroundColor),
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
      previousTheme?.primaryForegroundColor ?: Color.WHITE,
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
    window?.navigationBarColor = color
    applyColorToNavigationBarIcons(color)

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
