package hu.mrolcsi.android.lyricsplayer.player

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.util.Pair
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.forEach
import androidx.core.view.postDelayed
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import hu.mrolcsi.android.lyricsplayer.GlideApp
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.common.pager.PagerSnapHelperVerbose
import hu.mrolcsi.android.lyricsplayer.common.pager.RVPageScrollState
import hu.mrolcsi.android.lyricsplayer.common.pager.RVPagerSnapHelperListenable
import hu.mrolcsi.android.lyricsplayer.common.pager.RVPagerStateListener
import hu.mrolcsi.android.lyricsplayer.common.pager.VisiblePageState
import hu.mrolcsi.android.lyricsplayer.extensions.applyColorToNavigationBarIcons
import hu.mrolcsi.android.lyricsplayer.extensions.applyColorToStatusBarIcons
import hu.mrolcsi.android.lyricsplayer.extensions.media.albumArt
import hu.mrolcsi.android.lyricsplayer.extensions.media.duration
import hu.mrolcsi.android.lyricsplayer.extensions.media.isPlaying
import hu.mrolcsi.android.lyricsplayer.extensions.media.isSkipToNextEnabled
import hu.mrolcsi.android.lyricsplayer.extensions.media.isSkipToPreviousEnabled
import hu.mrolcsi.android.lyricsplayer.extensions.media.startProgressUpdater
import hu.mrolcsi.android.lyricsplayer.extensions.media.stopProgressUpdater
import hu.mrolcsi.android.lyricsplayer.extensions.mediaControllerCompat
import hu.mrolcsi.android.lyricsplayer.extensions.secondsToTimeStamp
import hu.mrolcsi.android.lyricsplayer.extensions.toColorHex
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

  private val mRepeatNone by lazy {
    getDrawable(R.drawable.ic_repeat_all)
      ?.constantState
      ?.newDrawable(resources)
      ?.mutate()
      ?.apply { alpha = Theme.DISABLED_OPACITY }
  }
  private val mRepeatOne by lazy {
    getDrawable(R.drawable.ic_repeat_one)
  }
  private val mRepeatAll by lazy {
    getDrawable(R.drawable.ic_repeat_all)
  }

  private val mQueueAdapter = QueueAdapter().apply {
    setHasStableIds(true)
  }
  private lateinit var mSnapHelper: PagerSnapHelperVerbose
  private var mScrollState: RVPageScrollState = RVPageScrollState.IDLE

  private val mGlideListener = object : RequestListener<Drawable> {
    override fun onResourceReady(
      resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean
    ): Boolean {
      supportStartPostponedEnterTransition()
      if (rvQueue.alpha == 0.0f) {
        ViewCompat.animate(rvQueue)
          .alpha(1f)
          .setStartDelay(Theme.PREFERRED_ANIMATION_DURATION)
          .setDuration(Theme.PREFERRED_ANIMATION_DURATION)
          .start()
      }
      return false
    }

    override fun onLoadFailed(
      e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean
    ): Boolean {
      return false
    }
  }

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

      mediaController.observe(this@PlayerActivity, Observer { controller ->
        controller?.let {
          // Apply MediaController to this Activity
          mediaControllerCompat = controller

          // Finish building the UI
          setupTransportControls(controller)
        }
      })
      currentMediaMetadata.observe(this@PlayerActivity, Observer { metadata ->
        metadata?.let {
          updateSongData(metadata)
          updatePager()
        }
      })
      currentPlaybackState.observe(this@PlayerActivity, Observer { state ->
        state?.let {
          updateControls(state)
        }
      })
      currentQueue.observe(this@PlayerActivity, Observer {
        // Update Queue
        mQueueAdapter.submitList(it)
      })
    }

    ThemeManager.getInstance(this).currentTheme.observe(this, object : Observer<Theme> {

      private var initialLoad = true

      override fun onChanged(it: Theme) {
        if (initialLoad) {
          applyThemeStatic(it)
          initialLoad = false
        } else {
          val backgroundColor = (content_player.background as ColorDrawable).color
          Log.v(
            LOG_TAG,
            "onThemeChanged(" +
                "backgroundColor=${backgroundColor.toColorHex()}, " +
                "themeColor=${it.primaryBackgroundColor.toColorHex()}" +
                ")"
          )
          if (backgroundColor != it.primaryBackgroundColor) {
            val visiblePosition = if (mQueueAdapter.realItemCount == 0) {
              -1
            } else {
              mSnapHelper.findSnapPosition(rvQueue.layoutManager) % mQueueAdapter.realItemCount
            }
            val activeId = mediaControllerCompat?.playbackState?.activeQueueItemId ?: -1
            val activePosition = mQueueAdapter.getItemPositionById(activeId)

            if (Math.abs(visiblePosition - activePosition) > 1) {
              applyThemeAnimated(it)
            } else {
              applyThemeStatic(it)
            }
          }
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
  }

  override fun onStop() {
    super.onStop()
    mPlayerModel.disconnect()

    mediaControllerCompat?.transportControls?.stopProgressUpdater()
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
    //val view = mSnapHelper.findSnapView(rvQueue.layoutManager)?.findViewById<ImageView>(R.id.imgCoverArt)
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
    rvQueue.run {
      layoutManager = LinearLayoutManager(this@PlayerActivity, LinearLayoutManager.HORIZONTAL, false)
      adapter = mQueueAdapter
      setHasFixedSize(true)
      doOnNextLayout {
        // This could be more sophisticated
        it.postDelayed(500) { updatePager() }
      }
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
          val queuePosition = mediaControllerCompat?.playbackState?.activeQueueItemId
          val pagerPosition = mSnapHelper.findSnapPosition(rvQueue.layoutManager)
          val itemId = mQueueAdapter.getItemId(pagerPosition)

          if (queuePosition != itemId) {
            mediaControllerCompat?.transportControls?.skipToQueueItem(itemId)
          }
        }
      }
    })
  }

  private fun setupTransportControls(controller: MediaControllerCompat) {
    // Enable controls
    sbSongProgress.isEnabled = true
    btnPrevious.isEnabled = true
    btnPlayPause.isEnabled = true
    btnNext.isEnabled = true

    // Update song metadata
    controller.metadata?.let {
      updateSongData(it)
    }

    // Update music controls
    controller.playbackState?.let {
      updateControls(it)
    }

    // Setup listeners

    btnPrevious.setOnClickListener {
      if (sbSongProgress.progress > 5) {
        // restart the song
        controller.transportControls?.seekTo(0)
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

    btnPlayPause.setOnClickListener {
      when (controller.playbackState.state) {
        PlaybackStateCompat.STATE_PLAYING -> {
          // Pause playback, stop updater
          controller.transportControls.pause()
          controller.transportControls.startProgressUpdater()
        }
        PlaybackStateCompat.STATE_PAUSED,
        PlaybackStateCompat.STATE_STOPPED -> {
          // Start playback, start updater
          controller.transportControls.play()
          controller.transportControls.stopProgressUpdater()
        }
      }
    }

    btnShuffle.setOnClickListener {
      when (controller.shuffleMode) {
        PlaybackStateCompat.SHUFFLE_MODE_NONE -> {
          controller.transportControls.setShuffleMode(
            PlaybackStateCompat.SHUFFLE_MODE_ALL
          )
          Toast.makeText(this, R.string.player_shuffleEnabled, Toast.LENGTH_SHORT).show()
        }
        else -> {
          controller.transportControls.setShuffleMode(
            PlaybackStateCompat.SHUFFLE_MODE_NONE
          )
          Toast.makeText(this, R.string.player_shuffleDisabled, Toast.LENGTH_SHORT).show()
        }
      }
    }

    btnRepeat.setOnClickListener {
      when (controller.repeatMode) {
        PlaybackStateCompat.REPEAT_MODE_NONE -> {
          controller.transportControls.setRepeatMode(
            PlaybackStateCompat.REPEAT_MODE_ONE
          )
          Toast.makeText(this, R.string.player_repeatOne, Toast.LENGTH_SHORT).show()
        }
        PlaybackStateCompat.REPEAT_MODE_ONE -> {
          controller.transportControls.setRepeatMode(
            PlaybackStateCompat.REPEAT_MODE_ALL
          )
          Toast.makeText(this, R.string.player_repeatAll, Toast.LENGTH_SHORT).show()
        }
        else -> {
          controller.transportControls.setRepeatMode(
            PlaybackStateCompat.REPEAT_MODE_NONE
          )
          Toast.makeText(this, R.string.player_repeatDisabled, Toast.LENGTH_SHORT).show()
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
        controller.transportControls.seekTo((mProgress * 1000).toLong())
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
    btnPrevious.alpha = if (playbackState.isSkipToPreviousEnabled) 1f else Theme.DISABLED_ALPHA

    btnNext.isEnabled = playbackState.isSkipToNextEnabled
    btnNext.alpha = if (playbackState.isSkipToNextEnabled) 1f else Theme.DISABLED_ALPHA

    when (playbackState.isPlaying) {
      true -> {
        mediaControllerCompat?.transportControls?.startProgressUpdater()
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
      }
      false -> {
        mediaControllerCompat?.transportControls?.stopProgressUpdater()
        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
      }
    }

    when (mediaControllerCompat?.shuffleMode) {
      PlaybackStateCompat.SHUFFLE_MODE_NONE -> btnShuffle.alpha = Theme.DISABLED_ALPHA
      PlaybackStateCompat.SHUFFLE_MODE_ALL -> btnShuffle.alpha = 1f
    }

    when (mediaControllerCompat?.repeatMode) {
      PlaybackStateCompat.REPEAT_MODE_NONE -> btnRepeat.setImageDrawable(mRepeatNone)
      PlaybackStateCompat.REPEAT_MODE_ONE -> btnRepeat.setImageDrawable(mRepeatOne)
      PlaybackStateCompat.REPEAT_MODE_ALL -> btnRepeat.setImageDrawable(mRepeatAll)
    }
  }

  private fun updateSongData(metadata: MediaMetadataCompat) {

    GlideApp.with(this)
      .load(metadata.albumArt)
      .listener(mGlideListener)
      .into(imgCoverArt)

    sbSongProgress.max = (metadata.duration / 1000).toInt()
  }

  private fun updatePager() {
    // Scroll pager to current item
    val visiblePosition = if (mQueueAdapter.realItemCount == 0) {
      -1
    } else {
      mSnapHelper.findSnapPosition(rvQueue.layoutManager) % mQueueAdapter.realItemCount
    }

    // Skip if Pager is not ready yet
    if (visiblePosition < 0) {
      Log.d(LOG_TAG, "updatePager(visiblePosition=$visiblePosition)")
      return
    }

    val visibleId = mQueueAdapter.getItemId(visiblePosition)

    // If Metadata has changed, then PlaybackState should have changed as well.
    val activeId = mediaControllerCompat?.playbackState?.activeQueueItemId ?: -1
    val activePosition = mQueueAdapter.getItemPositionById(activeId)

    Log.d(
      LOG_TAG,
      "updatePager(" +
          "visiblePosition=$visiblePosition, " +
          "visibleId=$visibleId, " +
          "activeId=$activeId, " +
          "activePosition=$activePosition) " +
          "ScrollState=$mScrollState"
    )

    if (mScrollState == RVPageScrollState.IDLE) {
      if (activePosition > RecyclerView.NO_POSITION && visibleId != activeId) {
        if (Math.abs(activePosition - visiblePosition) > 1) {
          rvQueue.scrollToPosition(activePosition)
        } else {
          rvQueue.smoothScrollToPosition(activePosition)
        }
      } else if (activePosition == RecyclerView.NO_POSITION) {
        // Try again after the adapter settles?
        Log.v(LOG_TAG, "updatePager() DELAY CHANGE")
        rvQueue.postDelayed(300) { updatePager() }
      }
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
    ).run {
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
    ).run {
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
    ).run {
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

    // Additional Buttons
    btnShuffle.drawable.setTint(color)

    mRepeatNone?.setTint(color)
    mRepeatOne?.setTint(color)
    mRepeatAll?.setTint(color)
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
