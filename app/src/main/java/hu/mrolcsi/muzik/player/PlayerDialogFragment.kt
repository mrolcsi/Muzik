package hu.mrolcsi.muzik.player

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.addCallback
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.postDelayed
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.pager.PagerSnapHelperVerbose
import hu.mrolcsi.muzik.common.pager.RVPagerSnapHelperListenable
import hu.mrolcsi.muzik.common.pager.RVPagerStateListener
import hu.mrolcsi.muzik.common.pager.VisiblePageState
import hu.mrolcsi.muzik.common.view.FullScreenDialogFragment
import hu.mrolcsi.muzik.common.view.MVVMListAdapter
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunNavCommands
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunUiCommands
import hu.mrolcsi.muzik.databinding.FragmentPlayerBinding
import hu.mrolcsi.muzik.databinding.ListItemPlaylistBinding
import hu.mrolcsi.muzik.extensions.updateNavigationIcons
import hu.mrolcsi.muzik.extensions.updateStatusBarIcons
import hu.mrolcsi.muzik.player.playlist.PlaylistItem
import hu.mrolcsi.muzik.player.playlist.PlaylistItemHolder
import hu.mrolcsi.muzik.player.playlist.PlaylistViewModel
import hu.mrolcsi.muzik.player.playlist.PlaylistViewModelImpl
import hu.mrolcsi.muzik.service.extensions.media.albumId
import hu.mrolcsi.muzik.service.extensions.media.artistId
import kotlinx.android.synthetic.main.content_player.*
import kotlinx.android.synthetic.main.fragment_player.*
import kotlinx.android.synthetic.main.fragment_playlist.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.abs

class PlayerDialogFragment : FullScreenDialogFragment() {

  private val playerViewModel: PlayerViewModel by viewModel<PlayerViewModelImpl>()
  private val playlistViewModel: PlaylistViewModel by viewModel<PlaylistViewModelImpl>()

  private lateinit var binding: FragmentPlayerBinding

  private lateinit var snapHelper: PagerSnapHelperVerbose
  private var scrollState: Int = RecyclerView.SCROLL_STATE_IDLE

  private val queueAdapter = MVVMListAdapter(
    diffCallback = ThemedQueueItem.DiffCallback,
    itemIdSelector = { it.queueItem.queueId },
    viewHolderFactory = { parent, _ ->
      QueueItemHolder(parent).apply {
        itemView.findViewById<TextView>(R.id.tvArtist).setOnClickListener {
          model?.queueItem?.description?.artistId?.let {
            playerViewModel.onArtistClick(it)
          }
        }
        itemView.findViewById<TextView>(R.id.tvAlbum).setOnClickListener {
          model?.queueItem?.description?.albumId?.let {
            playerViewModel.onAlbumClick(it)
          }
        }
      }
    }
  )

  private val playlistAdapter = MVVMListAdapter(
    diffCallback = PlaylistItem.DIFF_CALLBACK,
    itemIdSelector = { it.entry._id },
    viewHolderFactory = { parent, _ ->
      PlaylistItemHolder(
        ListItemPlaylistBinding.inflate(
          LayoutInflater.from(parent.context),
          parent,
          false
        ).apply {
          theme = playlistViewModel.currentTheme
          lifecycleOwner = viewLifecycleOwner
        }.root
      ).apply {
        itemView.setOnClickListener {
          model?.let { playlistViewModel.onSelect(it) }
        }
      }
    }
  )

  //region LIFECYCLE

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    activity?.onBackPressedDispatcher?.addCallback(this) {
      if (drawerLayout.isDrawerOpen(GravityCompat.END))
        drawerLayout.closeDrawer(GravityCompat.END)
      else
        findNavController().navigateUp()
    }

    playerViewModel.apply {
      requireContext().observeAndRunUiCommands(viewLifecycleOwner, this)
      findNavController().observeAndRunNavCommands(viewLifecycleOwner, this)

      queueState.observe(viewLifecycleOwner, Observer { state ->
        Log.d(LOG_TAG, "onQueueStateChanged($state)")
        queueAdapter.onChanged(state.queue)
        updatePager("onQueueStateChanged", state.activeQueueId)
      })
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    FragmentPlayerBinding.inflate(inflater, container, false).also {
      binding = it
      it.playerViewModel = playerViewModel
      it.playlistViewModel = playlistViewModel
      it.lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    setupToolbar()
    setupPager()
    setupControls()
    setupPlaylist()
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
    }

    snapHelper = RVPagerSnapHelperListenable().attachToRecyclerView(rvQueue, object : RVPagerStateListener {

      override fun onPageScroll(pagesState: List<VisiblePageState>) {

        if (scrollState == RecyclerView.SCROLL_STATE_IDLE && pagesState.size == 1) {
          playerViewModel.currentTheme.value?.let { applyTheme(it.primaryBackgroundColor, it.primaryForegroundColor) }
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

            applyTheme(backgroundColor, foregroundColor)
          }
        }
      }

      override fun onScrollStateChanged(state: Int) {
        scrollState = state

        if (state == RecyclerView.SCROLL_STATE_IDLE) {
          // check if item position is different from the now playing position
          val queueId = playerViewModel.queueState.value?.activeQueueId
          val pagerPosition = snapHelper.findSnapPosition(rvQueue.layoutManager)
          val visibleId = queueAdapter.getItemId(pagerPosition)

          Log.d(LOG_TAG, "onScrollStateChanged($state) queueId=$queueId visibleId=$visibleId")

          // Skip to visible item in queue
          if (queueId != visibleId) {
            playerViewModel.skipToQueueItem(visibleId)
          } else {
            // Make RecyclerView visible after scrolling
            showPager()
          }
        }
      }
    })
  }

  private fun updatePager(caller: String, queueId: Long) {
    Log.v(LOG_TAG, "updatePager(calledFrom = $caller, queueId = $queueId)")

    if (queueId < 0) {
      Log.v(LOG_TAG, "updatePager(queueId=$queueId) Update cancelled.")
      return
    }

    // Scroll pager to current item
    val visiblePosition = snapHelper.findSnapPosition(rvQueue.layoutManager)

    // Skip if Pager is not ready yet
    if (visiblePosition < 0) {
      rvQueue.postDelayed(300) { updatePager("updatePagerDelayed: visiblePosition < 0", queueId) }
      return
    }

    val visibleId = queueAdapter.getItemId(visiblePosition)
    val queuePosition = queueAdapter.currentList.indexOfFirst { it.queueItem.queueId == queueId }

    Log.v(
      LOG_TAG,
      "updatePager(" +
          "visiblePosition=$visiblePosition, " +
          "visibleId=$visibleId, " +
          "queuePosition=$queuePosition, " +
          "queueId=$queueId) " +
          "ScrollState=$scrollState"
    )

    if (scrollState == RecyclerView.SCROLL_STATE_IDLE) {
      when {
        visibleId == queueId -> {
          // Make RecyclerView visible (no need to scroll)
          showPager()
        }
        queuePosition == RecyclerView.NO_POSITION ->
          // Try again after the adapter settles?
          rvQueue.postDelayed(300) { updatePager("updatePagerDelayed: queuePosition = -1", queueId) }
        else -> {
          // Scroll to now playing song
          showPager()
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

  private fun setupPlaylist() {
    playlistViewModel.items.observe(viewLifecycleOwner, playlistAdapter)
    rvPlaylist.adapter = playlistAdapter
  }

  private fun showPager() {
    (view?.parent as? ViewGroup)?.doOnPreDraw { startPostponedEnterTransition() }
    ViewCompat.animate(rvQueue)
      .alpha(1f)
      .setDuration(context?.resources?.getInteger(R.integer.preferredAnimationDuration)?.toLong() ?: 300L)
      .start()
  }

  private fun applyTheme(@ColorInt backgroundColor: Int, @ColorInt foregroundColor: Int) {
    dialog?.window?.apply {
      setBackgroundDrawable(ColorDrawable(backgroundColor))
      updateStatusBarIcons(backgroundColor)
      updateNavigationIcons(backgroundColor)
    }
    binding.backgroundColor = backgroundColor
    binding.foregroundColor = foregroundColor
  }

  companion object {
    private const val LOG_TAG = "PlayerDialogFragment"
  }

}
