package hu.mrolcsi.muzik.player

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.postDelayed
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import androidx.transition.TransitionListenerAdapter
import dagger.android.support.DaggerFragment
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.DiffCallbacks
import hu.mrolcsi.muzik.common.pager.PagerSnapHelperVerbose
import hu.mrolcsi.muzik.common.pager.RVPagerSnapHelperListenable
import hu.mrolcsi.muzik.common.pager.RVPagerStateListener
import hu.mrolcsi.muzik.common.pager.VisiblePageState
import hu.mrolcsi.muzik.common.view.MVVMListAdapter
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunNavCommands
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunUiCommands
import hu.mrolcsi.muzik.databinding.FragmentPlayerBinding
import hu.mrolcsi.muzik.extensions.applyForegroundColor
import hu.mrolcsi.muzik.extensions.applyNavigationBarColor
import hu.mrolcsi.muzik.extensions.applyStatusBarColor
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.content_player.*
import kotlinx.android.synthetic.main.fragment_player.*
import javax.inject.Inject
import kotlin.math.abs

class PlayerFragment : DaggerFragment() {

  @Inject lateinit var viewModel: PlayerViewModel

  // Prepare drawables (separate for each button)
  private val mPreviousBackground by lazy { context?.getDrawable(R.drawable.media_button_background) }
  private val mPlayPauseBackground by lazy { context?.getDrawable(R.drawable.media_button_background) }
  private val mNextBackground by lazy { context?.getDrawable(R.drawable.media_button_background) }

  private lateinit var snapHelper: PagerSnapHelperVerbose
  private var scrollState: Int = RecyclerView.SCROLL_STATE_IDLE
  private val queueAdapter = MVVMListAdapter(
    diffCallback = DiffCallbacks.queueItemCallback,
    itemIdSelector = { it.queueId },
    viewHolderFactory = { parent, _ -> QueueItemHolder(parent) }
  )

  //region LIFECYCLE

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    viewModel.apply {
      requireContext().observeAndRunUiCommands(viewLifecycleOwner, this)
      findNavController().observeAndRunNavCommands(viewLifecycleOwner, this)
      queue.observe(viewLifecycleOwner, queueAdapter)
      albumArt.observe(viewLifecycleOwner, Observer {
        imgCoverArt.setImageDrawable(it)
        (view?.parent as? ViewGroup)?.doOnPreDraw {
          // Parent has been drawn. Start transitioning!
          startPostponedEnterTransition()
        }
      })
    }

    ThemeManager.getInstance(requireContext()).currentTheme.observe(viewLifecycleOwner, object : Observer<Theme> {

      private var initialLoad = true

      override fun onChanged(it: Theme) {
        if (initialLoad) {
          applyThemeStatic(it)
          initialLoad = false
        }
      }
    })
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    postponeEnterTransition()

    val animationDuration = context?.resources?.getInteger(R.integer.preferredAnimationDuration)?.toLong() ?: 300L

    val transitionInflater = TransitionInflater.from(requireContext())

    enterTransition = transitionInflater
      .inflateTransition(R.transition.slide_bottom)
      .setDuration(animationDuration)
      .excludeTarget(imgCoverArt, true)
      .onTransitionEnd {
        Log.v(LOG_TAG, "enterTransition.onTransitionEnd()")
        context?.let {
          ThemeManager.getInstance(it).currentTheme.value?.let { theme ->
            activity?.applyStatusBarColor(theme.statusBarColor)
          }
        }
      }

    returnTransition = transitionInflater
      .inflateTransition(R.transition.slide_bottom)
      .setDuration(animationDuration)
      .excludeTarget(imgCoverArt, true)
      .onTransitionEnd {
        Log.v(LOG_TAG, "returnTransition.onTransitionStart()")
        context?.let {
          ThemeManager.getInstance(requireContext()).currentTheme.value?.let { theme ->
            activity?.applyStatusBarColor(theme.primaryBackgroundColor)
          }
        }
      }

    sharedElementEnterTransition = transitionInflater
      .inflateTransition(android.R.transition.move)
      .setDuration(animationDuration)

    sharedElementReturnTransition = transitionInflater
      .inflateTransition(android.R.transition.move)
      .setDuration(animationDuration)

    if (savedInstanceState != null) {
      updatePager()
      // Re-apply status bar color after rotation
      ThemeManager.getInstance(requireContext()).currentTheme.value?.let { theme ->
        activity?.applyStatusBarColor(theme.statusBarColor)
      }
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    FragmentPlayerBinding.inflate(inflater, container, false).also {
      it.viewModel = viewModel
      it.lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    setupToolbar()
    setupPager()
    setupControls()
  }

  //endregion

  private fun setupToolbar() {
    playerToolbar.run {
      setupWithNavController(findNavController())

      setOnMenuItemClickListener { item ->
        when (item?.itemId) {
          R.id.menuPlaylist -> {
            drawer_layout.openDrawer(GravityCompat.END)
            true
          }
          else -> super.onOptionsItemSelected(item)
        }
      }
    }
  }

  private fun setupPager() {
    rvQueue.run {
      layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
      adapter = queueAdapter
      setHasFixedSize(true)
      doOnNextLayout {
        // This could be more sophisticated
        it.postDelayed(500) { updatePager() }
      }
    }

    snapHelper = RVPagerSnapHelperListenable().attachToRecyclerView(rvQueue, object : RVPagerStateListener {

      override fun onPageScroll(pagesState: List<VisiblePageState>) {
        if (scrollState == RecyclerView.SCROLL_STATE_IDLE && pagesState.size == 1) {
          // Apply theme of visible holder
          val holder = rvQueue.findContainingViewHolder(pagesState.first().view) as QueueItemHolder
          holder.usedTheme?.let { applyThemeStatic(it) }
        }

        // Blend colors while scrolling
        if (pagesState.size == 2) {  // between 2 pages -> blend colors
          val leftHolder = rvQueue.findContainingViewHolder(pagesState.first().view) as QueueItemHolder
          val rightHolder = rvQueue.findContainingViewHolder(pagesState.last().view) as QueueItemHolder

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

            activity?.applyStatusBarColor(statusBarColor)
          }
        }
      }

      override fun onScrollStateChanged(state: Int) {
        scrollState = state

        if (state == RecyclerView.SCROLL_STATE_IDLE) {
          // check if item position is different from the now playing position
          val queueId = viewModel.getCurrentQueueId()
          val pagerPosition = snapHelper.findSnapPosition(rvQueue.layoutManager)
          val itemId = rvQueue.adapter?.getItemId(pagerPosition) ?: RecyclerView.NO_ID

          Log.d(LOG_TAG, "onScrollStateChanged($state) queueId=$queueId itemId=$itemId")

          // Skip to visible item in queue
          if (queueId != itemId) {
            viewModel.skipToQueueItem(itemId)
          }
        }
      }
    })
  }

  private fun updatePager() {
    // Scroll pager to current item
    val visiblePosition = snapHelper.findSnapPosition(rvQueue.layoutManager)

    // Skip if Pager is not ready yet
    if (visiblePosition < 0) {
      Log.d(LOG_TAG, "updatePager(visiblePosition=$visiblePosition)")
      return
    }

    val visibleId = rvQueue.adapter?.getItemId(visiblePosition) ?: RecyclerView.NO_ID

    // If Metadata has changed, then PlaybackState should have changed as well.
    val queueId = viewModel.getCurrentQueueId()
    val queuePosition = queueAdapter.getItemPositionById(queueId)

    Log.d(
      LOG_TAG,
      "updatePager(" +
          "visiblePosition=$visiblePosition, " +
          "visibleId=$visibleId, " +
          "queuePosition=$queuePosition, " +
          "queueId=$queueId) " +
          "ScrollState=$scrollState"
    )

    if (scrollState == RecyclerView.SCROLL_STATE_IDLE) {
      if (queuePosition > RecyclerView.NO_POSITION && visibleId != queueId) {
        if (abs(queuePosition - visiblePosition) > 1) {
          rvQueue.scrollToPosition(queuePosition)
        } else {
          rvQueue.smoothScrollToPosition(queuePosition)
        }
        // Make view visible
        ViewCompat.animate(rvQueue)
          .alpha(1f)
          .setDuration(context?.resources?.getInteger(R.integer.preferredAnimationDuration)?.toLong() ?: 300L)
          .withEndAction { imgCoverArt?.alpha = 0.0f }
          .start()
      } else if (queuePosition == RecyclerView.NO_POSITION) {
        // Try again after the adapter settles?
        Log.v(LOG_TAG, "updatePager() DELAY CHANGE")
        rvQueue.postDelayed(300) { updatePager() }
      }
    }
  }

  private fun setupControls() {
    btnPrevious.setOnClickListener {
      val currentPosition = snapHelper.findSnapPosition(rvQueue.layoutManager)
      rvQueue.smoothScrollToPosition(currentPosition - 1)
    }

    btnNext.setOnClickListener {
      val currentPosition = snapHelper.findSnapPosition(rvQueue.layoutManager)
      rvQueue.smoothScrollToPosition(currentPosition + 1)
    }
  }

  private fun applyThemeStatic(theme: Theme) {
    Log.i(LOG_TAG, "applyThemeStatic($theme)")

    applyBackgroundColor(theme.primaryBackgroundColor)
    applyForegroundColor(theme.primaryForegroundColor)
  }

  private fun applyForegroundColor(color: Int) {
    // Toolbar
    playerToolbar.applyForegroundColor(color)

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
    btnShuffle.imageTintList = ColorStateList.valueOf(color)
    btnRepeat.imageTintList = ColorStateList.valueOf(color)
  }

  private fun applyBackgroundColor(color: Int) {
    // Window background
    view?.setBackgroundColor(color)

    // Navigation Bar
    activity?.window?.navigationBarColor = color
    activity?.applyNavigationBarColor(color)

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

    private const val FAST_FORWARD_INTERVAL = 500
  }
}

fun Transition.onTransitionEnd(listener: (Transition) -> Unit) =
  addListener(object : TransitionListenerAdapter() {
    override fun onTransitionEnd(transition: Transition) {
      listener.invoke(transition)
    }
  })