package hu.mrolcsi.muzik.player

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
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
import androidx.transition.TransitionInflater
import dagger.android.support.DaggerFragment
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.pager.PagerSnapHelperVerbose
import hu.mrolcsi.muzik.common.pager.RVPagerSnapHelperListenable
import hu.mrolcsi.muzik.common.pager.RVPagerStateListener
import hu.mrolcsi.muzik.common.pager.VisiblePageState
import hu.mrolcsi.muzik.common.view.MVVMListAdapter
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunNavCommands
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunUiCommands
import hu.mrolcsi.muzik.databinding.FragmentPlayerBinding
import hu.mrolcsi.muzik.theme.Theme
import hu.mrolcsi.muzik.theme.ThemeService
import kotlinx.android.synthetic.main.content_player.*
import kotlinx.android.synthetic.main.fragment_player.*
import javax.inject.Inject
import kotlin.math.abs

class PlayerFragment : DaggerFragment() {

  @Inject lateinit var viewModel: PlayerViewModel
  @Inject lateinit var themeService: ThemeService

  private lateinit var binding: FragmentPlayerBinding

  private lateinit var snapHelper: PagerSnapHelperVerbose
  private var scrollState: Int = RecyclerView.SCROLL_STATE_IDLE

  private val queueAdapter = MVVMListAdapter(
    diffCallback = ThemedQueueItem.DiffCallback,
    itemIdSelector = { it.queueItem.queueId },
    viewHolderFactory = { parent, _ -> QueueItemHolder(parent) }
  )

  // Gesture Detection
  private val mGestureDetector by lazy {
    GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {

      private val SWIPE_THRESHOLD = 100
      private val SWIPE_VELOCITY_THRESHOLD = 100

      override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        val diffY = e2.y - e1.y
        val diffX = e2.x - e1.x
        if (abs(diffX) < abs(diffY)) {
          if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
            if (diffY > 0) {
              // onSwipeDown
              activity?.onBackPressed()
              return true
            }
          }
        }
        return false
      }
    })
  }

  //region LIFECYCLE

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    viewModel.apply {
      requireContext().observeAndRunUiCommands(viewLifecycleOwner, this)
      findNavController().observeAndRunNavCommands(viewLifecycleOwner, this)

      queue.observe(viewLifecycleOwner, Observer {
        queueAdapter.onChanged(it)
        updatePager("onQueueChanged")
      })

      currentQueueId.observe(viewLifecycleOwner, Observer {
        updatePager("onCurrentQueueIdChanged")
      })

      albumArt.observe(viewLifecycleOwner, Observer {
        imgCoverArt.setImageDrawable(it)
        (view?.parent as? ViewGroup)?.doOnPreDraw {
          startPostponedEnterTransition()
        }
      })
    }
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

    returnTransition = transitionInflater
      .inflateTransition(R.transition.slide_bottom)
      .setDuration(animationDuration)
      .excludeTarget(imgCoverArt, true)

    sharedElementEnterTransition = transitionInflater
      .inflateTransition(android.R.transition.move)
      .setDuration(animationDuration)

    sharedElementReturnTransition = transitionInflater
      .inflateTransition(android.R.transition.move)
      .setDuration(animationDuration)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    FragmentPlayerBinding.inflate(inflater, container, false).also {
      binding = it
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
      setupWithNavController(findNavController(), drawerLayout)
      setNavigationIcon(R.drawable.ic_chevron_down)

      setOnMenuItemClickListener { item ->
        when (item?.itemId) {
          R.id.menuPlaylist -> {
            drawerLayout.openDrawer(GravityCompat.END)
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
        it.postDelayed(500) { updatePager("setupPagerWithDelay") }
      }

      setOnTouchListener { v, event ->
        mGestureDetector.onTouchEvent(event) || v.onTouchEvent(event)
      }
    }

    snapHelper = RVPagerSnapHelperListenable().attachToRecyclerView(rvQueue, object : RVPagerStateListener {

      override fun onPageScroll(pagesState: List<VisiblePageState>) {

        if (scrollState == RecyclerView.SCROLL_STATE_IDLE && pagesState.size == 1) {
          viewModel.currentTheme.value?.let { applyTheme(it) }
        }

        // Blend colors while scrolling
        if (pagesState.size == 2) {  // between 2 pages -> blend colors
          val leftHolder = rvQueue.findContainingViewHolder(pagesState.first().view) as QueueItemHolder
          val rightHolder = rvQueue.findContainingViewHolder(pagesState.last().view) as QueueItemHolder

          val leftTheme = leftHolder.model?.theme
          val rightTheme = rightHolder.model?.theme

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

            binding.backgroundColor = backgroundColor
            binding.foregroundColor = foregroundColor
            activity?.window?.setBackgroundDrawable(ColorDrawable(backgroundColor))

          }
        }
      }

      override fun onScrollStateChanged(state: Int) {
        scrollState = state

        if (state == RecyclerView.SCROLL_STATE_IDLE) {
          // check if item position is different from the now playing position
          val queueId = viewModel.getCurrentQueueId()
          val pagerPosition = snapHelper.findSnapPosition(rvQueue.layoutManager)
          val visibleId = rvQueue.adapter?.getItemId(pagerPosition) ?: RecyclerView.NO_ID

          Log.d(LOG_TAG, "onScrollStateChanged($state) queueId=$queueId visibleId=$visibleId")

          // Skip to visible item in queue
          if (queueId != visibleId) {
            viewModel.skipToQueueItem(visibleId)
          } else {
            // Make RecyclerView visible after scrolling
            showRecyclerView()
          }
        }
      }
    })
  }

  private fun updatePager(caller: String) {
    // Scroll pager to current item
    val visiblePosition = snapHelper.findSnapPosition(rvQueue.layoutManager)

    // Skip if Pager is not ready yet
    if (visiblePosition < 0) {
      rvQueue.postDelayed(300) { updatePager("updatePagerDelayed: visiblePosition < 0") }
      return
    }

    val visibleId = rvQueue.adapter?.getItemId(visiblePosition) ?: RecyclerView.NO_ID

    // If Metadata has changed, then PlaybackState should have changed as well.
    val queueId = viewModel.getCurrentQueueId()
    val queuePosition = queueAdapter.getItemPositionById(queueId)

    Log.v(
      LOG_TAG,
      "updatePager(" +
          "calledFrom=$caller, " +
          "visiblePosition=$visiblePosition, " +
          "visibleId=$visibleId, " +
          "queuePosition=$queuePosition, " +
          "queueId=$queueId) " +
          "ScrollState=$scrollState"
    )

    if (scrollState == RecyclerView.SCROLL_STATE_IDLE) {
      when {
        visibleId == queueId ->
          // Make RecyclerView visible (no need to scroll)
          showRecyclerView()
        queuePosition == RecyclerView.NO_POSITION ->
          // Try again after the adapter settles?
          rvQueue.postDelayed(300) { updatePager("updatePagerDelayed: SCROLL_STATE_IDLE") }
        else -> {
          // Scroll to now playing song
          showRecyclerView()
          if (abs(queuePosition - visiblePosition) > 1) {
            Log.i(LOG_TAG, "Scroll to position: $visiblePosition -> $queuePosition")
            rvQueue.scrollToPosition(queuePosition)
          } else {
            Log.i(LOG_TAG, "Smooth scroll to position: $visiblePosition -> $queuePosition")
            rvQueue.smoothScrollToPosition(queuePosition)
          }
        }
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

  private fun showRecyclerView() {
    if (rvQueue.alpha < 1f)
      ViewCompat.animate(rvQueue)
        .alpha(1f)
        .setDuration(context?.resources?.getInteger(R.integer.preferredAnimationDuration)?.toLong() ?: 300L)
        .withStartAction { Log.i(LOG_TAG, "Making RecyclerView visible...") }
        .withEndAction { imgCoverArt?.alpha = 0.0f }
        .start()
  }

  private fun applyTheme(it: Theme) {
    Log.d(LOG_TAG, "Apply theme: $it")
    binding.backgroundColor = it.primaryBackgroundColor
    binding.foregroundColor = it.primaryForegroundColor
  }

  companion object {
    private const val LOG_TAG = "PlayerFragment"
  }

}
