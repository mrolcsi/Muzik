package hu.mrolcsi.android.lyricsplayer.player

import android.animation.ValueAnimator
import android.annotation.SuppressLint
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
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.forEach
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import androidx.transition.TransitionListenerAdapter
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.common.glide.GlideApp
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
import hu.mrolcsi.android.lyricsplayer.extensions.secondsToTimeStamp
import hu.mrolcsi.android.lyricsplayer.extensions.toColorHex
import hu.mrolcsi.android.lyricsplayer.theme.Theme
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager
import kotlinx.android.synthetic.main.content_player.*
import kotlinx.android.synthetic.main.fragment_player.*

class PlayerFragment : Fragment() {

  private lateinit var mPlayerModel: PlayerViewModel

  private var mUserIsSeeking = false

  // Prepare drawables (separate for each button)
  private val mPreviousBackground by lazy { context?.getDrawable(R.drawable.media_button_background) }
  private val mPlayPauseBackground by lazy { context?.getDrawable(R.drawable.media_button_background) }
  private val mNextBackground by lazy { context?.getDrawable(R.drawable.media_button_background) }

  private val mRepeatNone by lazy {
    context?.getDrawable(R.drawable.ic_repeat_all)
      ?.constantState
      ?.newDrawable(resources)
      ?.mutate()
      ?.apply { alpha = Theme.DISABLED_OPACITY }
  }
  private val mRepeatOne by lazy {
    context?.getDrawable(R.drawable.ic_repeat_one)
  }
  private val mRepeatAll by lazy {
    context?.getDrawable(R.drawable.ic_repeat_all)
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
      startPostponedEnterTransition()
      return false
    }

    override fun onLoadFailed(
      e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean
    ): Boolean {
      startPostponedEnterTransition()
      return false
    }
  }

  // Gesture Detection
  private val mGestureDetector by lazy {
    GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {

      private val SWIPE_THRESHOLD = 100
      private val SWIPE_VELOCITY_THRESHOLD = 100

      override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        val diffY = e2.y - e1.y
        val diffX = e2.x - e1.x
        if (Math.abs(diffX) < Math.abs(diffY)) {
          if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
            if (diffY > 0) {
              // onSwipeDown
              onBackPressed()
              return true
            }
          }
        }
        return false
      }
    })
  }

  //region LIFECYCLE

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    postponeEnterTransition()

    enterTransition = TransitionInflater
      .from(requireContext())
      .inflateTransition(R.transition.slide_bottom)
      .setDuration(Theme.PREFERRED_ANIMATION_DURATION)
      .excludeTarget(imgCoverArt, true)
      .addListener(object : TransitionListenerAdapter() {
        override fun onTransitionEnd(transition: Transition) {
          Log.v(LOG_TAG, "enterTransition.onTransitionEnd()")

          context?.let {
            ThemeManager.getInstance(it).currentTheme.value?.let { theme ->
              activity?.window?.statusBarColor = theme.statusBarColor
              activity?.applyColorToStatusBarIcons(theme.statusBarColor)
            }
          }
        }
      })

    returnTransition = TransitionInflater
      .from(requireContext())
      .inflateTransition(R.transition.slide_bottom)
      .setDuration(Theme.PREFERRED_ANIMATION_DURATION)
      .excludeTarget(imgCoverArt, true)
      .addListener(object : TransitionListenerAdapter() {
        override fun onTransitionStart(transition: Transition) {
          Log.v(LOG_TAG, "returnTransition.onTransitionStart()")

          context?.let {
            ThemeManager.getInstance(requireContext()).currentTheme.value?.let { theme ->
              activity?.window?.statusBarColor = theme.primaryBackgroundColor
              activity?.applyColorToStatusBarIcons(theme.primaryBackgroundColor)
            }
          }
        }
      })

    sharedElementEnterTransition = TransitionInflater
      .from(requireContext())
      .inflateTransition(android.R.transition.move)
      .setDuration(Theme.PREFERRED_ANIMATION_DURATION)

    sharedElementReturnTransition = TransitionInflater
      .from(requireContext())
      .inflateTransition(android.R.transition.move)
      .setDuration(Theme.PREFERRED_ANIMATION_DURATION)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    activity?.run {
      mPlayerModel = ViewModelProviders.of(this).get(PlayerViewModel::class.java).apply {
        Log.d(LOG_TAG, "Got PlayerViewModel: $this")

        mediaController.observe(viewLifecycleOwner, Observer { controller ->
          controller?.let {
            // Apply MediaController to this Activity
            MediaControllerCompat.setMediaController(requireActivity(), it)

            // Finish building the UI
            setupTransportControls(controller)
          }
        })
        currentMediaMetadata.observe(viewLifecycleOwner, Observer { metadata ->
          metadata?.let {
            updateSongData(metadata)
            updatePager()
          }
        })
        currentPlaybackState.observe(viewLifecycleOwner, Observer { state ->
          state?.let {
            updateControls(state)
          }
        })
        currentQueue.observe(viewLifecycleOwner, Observer {
          // Update Queue
          mQueueAdapter.submitList(it)
        })
      }

      ThemeManager.getInstance(requireContext()).currentTheme.observe(viewLifecycleOwner, object : Observer<Theme> {

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
//            val visiblePosition = if (mQueueAdapter.realItemCount == 0) -1
//            else mSnapHelper.findSnapPosition(rvQueue.layoutManager) //% mQueueAdapter.realItemCount
              val visiblePosition = mSnapHelper.findSnapPosition(rvQueue.layoutManager)

              val activeId = MediaControllerCompat.getMediaController(requireActivity())
                ?.playbackState?.activeQueueItemId ?: -1
              val activePosition = mQueueAdapter.getItemPositionById(activeId/*, visiblePosition*/)

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
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    inflater.inflate(R.layout.fragment_player, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    setupToolbar()
    setupPager()
  }

  override fun onResume() {
    super.onResume()
    activity?.volumeControlStream = AudioManager.STREAM_MUSIC
  }

  override fun onStop() {
    super.onStop()

    MediaControllerCompat.getMediaController(requireActivity())?.transportControls?.stopProgressUpdater()
  }

  // TODO: handle onBackPressed!
  fun onBackPressed() {
    // If drawer is open, just close it
    if (drawer_layout.isDrawerOpen(GravityCompat.END)) {
      drawer_layout.closeDrawer(GravityCompat.END)
      return
    }

    // Fade out queue
    ViewCompat.animate(rvQueue)
      .alpha(0f)
      .setDuration(Theme.PREFERRED_ANIMATION_DURATION)
      .start()
  }

  //endregion

  private fun setupToolbar() {
    ThemeManager.getInstance(requireContext()).currentTheme.value?.let { theme ->
      playerToolbar.menu?.forEach { item ->
        item.icon.setColorFilter(theme.primaryForegroundColor, PorterDuff.Mode.SRC_IN)
      }
    }

    playerToolbar.setOnMenuItemClickListener { item ->
      when (item?.itemId) {
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
  }

  private fun setupPager() {
    rvQueue.run {
      layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
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

              activity?.window?.statusBarColor = statusBarColor
              activity?.applyColorToStatusBarIcons(statusBarColor)
            }
          }
        }
      }

      override fun onScrollStateChanged(state: RVPageScrollState) {
        mScrollState = state

        if (state == RVPageScrollState.IDLE) {
          val controller = MediaControllerCompat.getMediaController(requireActivity())
          // check if item position is different from the now playing position
          val queueId = controller.playbackState.activeQueueItemId
          val pagerPosition = mSnapHelper.findSnapPosition(rvQueue.layoutManager)
          val itemId = mQueueAdapter.getItemId(pagerPosition)

          if (queueId != itemId) {
            controller.transportControls.skipToQueueItem(itemId)
          }
        }
      }
    })
  }

  // TODO: handle swipe down gesture
//  override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
//    return mGestureDetector.onTouchEvent(ev) || super.dispatchTouchEvent(ev)
//  }

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
          Toast.makeText(context, R.string.player_shuffleEnabled, Toast.LENGTH_SHORT).show()
        }
        else -> {
          controller.transportControls.setShuffleMode(
            PlaybackStateCompat.SHUFFLE_MODE_NONE
          )
          Toast.makeText(context, R.string.player_shuffleDisabled, Toast.LENGTH_SHORT).show()
        }
      }
    }

    btnRepeat.setOnClickListener {
      when (controller.repeatMode) {
        PlaybackStateCompat.REPEAT_MODE_NONE -> {
          controller.transportControls.setRepeatMode(
            PlaybackStateCompat.REPEAT_MODE_ONE
          )
          Toast.makeText(context, R.string.player_repeatOne, Toast.LENGTH_SHORT).show()
        }
        PlaybackStateCompat.REPEAT_MODE_ONE -> {
          controller.transportControls.setRepeatMode(
            PlaybackStateCompat.REPEAT_MODE_ALL
          )
          Toast.makeText(context, R.string.player_repeatAll, Toast.LENGTH_SHORT).show()
        }
        else -> {
          controller.transportControls.setRepeatMode(
            PlaybackStateCompat.REPEAT_MODE_NONE
          )
          Toast.makeText(context, R.string.player_repeatDisabled, Toast.LENGTH_SHORT).show()
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

    val controller = MediaControllerCompat.getMediaController(requireActivity())

    when (playbackState.isPlaying) {
      true -> {
        controller.transportControls.startProgressUpdater()
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
      }
      false -> {
        controller.transportControls.stopProgressUpdater()
        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
      }
    }

    when (controller.shuffleMode) {
      PlaybackStateCompat.SHUFFLE_MODE_NONE -> btnShuffle.alpha = Theme.DISABLED_ALPHA
      PlaybackStateCompat.SHUFFLE_MODE_ALL -> btnShuffle.alpha = 1f
    }

    when (controller.repeatMode) {
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
//    val visiblePosition = if (mQueueAdapter.realItemCount == 0) -1
//    else mSnapHelper.findSnapPosition(rvQueue.layoutManager) % mQueueAdapter.realItemCount
    val visiblePosition = mSnapHelper.findSnapPosition(rvQueue.layoutManager)

    // Skip if Pager is not ready yet
    if (visiblePosition < 0) {
      Log.d(LOG_TAG, "updatePager(visiblePosition=$visiblePosition)")
      return
    }

    val visibleId = mQueueAdapter.getItemId(visiblePosition)

    // If Metadata has changed, then PlaybackState should have changed as well.
    val queueId = MediaControllerCompat.getMediaController(requireActivity()).playbackState.activeQueueItemId
    val queuePosition = mQueueAdapter.getItemPositionById(queueId/*, visiblePosition*/)

    Log.d(
      LOG_TAG,
      "updatePager(" +
          "visiblePosition=$visiblePosition, " +
          "visibleId=$visibleId, " +
          "queuePosition=$queuePosition, " +
          "queueId=$queueId) " +
          "ScrollState=$mScrollState"
    )

    if (mScrollState == RVPageScrollState.IDLE) {
      if (queuePosition > RecyclerView.NO_POSITION && visibleId != queueId) {
        if (Math.abs(queuePosition - visiblePosition) > 1) {
          rvQueue.scrollToPosition(queuePosition)
        } else {
          rvQueue.smoothScrollToPosition(queuePosition)
        }

        // Make view visible
        ViewCompat.animate(rvQueue)
          .alpha(1f)
          .setDuration(Theme.PREFERRED_ANIMATION_DURATION)
          .start()

      } else if (queuePosition == RecyclerView.NO_POSITION) {
        // Try again after the adapter settles?
        Log.v(LOG_TAG, "updatePager() DELAY CHANGE")
        rvQueue.postDelayed(300) { updatePager() }
      }
    }
  }

  private fun applyThemeStatic(theme: Theme) {
    Log.i(LOG_TAG, "applyThemeStatic($theme)")

    applyBackgroundColor(theme.primaryBackgroundColor)
    applyForegroundColor(theme.primaryForegroundColor)
  }

  private fun applyThemeAnimated(theme: Theme) {
    Log.i(LOG_TAG, "applyingThemeAnimated($theme)")

    val previousTheme = ThemeManager.getInstance(requireContext()).previousTheme

    // StatusBar Color
    ValueAnimator.ofArgb(
      previousTheme?.statusBarColor ?: ContextCompat.getColor(requireContext(), R.color.backgroundColor),
      theme.statusBarColor
    ).run {
      duration = Theme.PREFERRED_ANIMATION_DURATION
      addUpdateListener {
        val color = it.animatedValue as Int

        activity?.window?.statusBarColor = color
        activity?.applyColorToStatusBarIcons(color)
      }
      start()
    }

    // Background Color
    ValueAnimator.ofArgb(
      previousTheme?.primaryBackgroundColor ?: ContextCompat.getColor(requireContext(), R.color.backgroundColor),
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
    activity?.window?.navigationBarColor = color
    activity?.applyColorToNavigationBarIcons(color)

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
    private const val LOG_TAG = "PlayerFragment"
  }
}
