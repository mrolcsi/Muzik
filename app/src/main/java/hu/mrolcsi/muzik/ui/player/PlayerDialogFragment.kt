package hu.mrolcsi.muzik.ui.player

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager2.widget.ViewPager2
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.databinding.FragmentPlayerBinding
import hu.mrolcsi.muzik.databinding.ListItemQueueBinding
import hu.mrolcsi.muzik.ui.base.FullScreenDialogFragment
import hu.mrolcsi.muzik.ui.common.BoundMVVMViewHolder
import hu.mrolcsi.muzik.ui.common.MVVMListAdapter
import hu.mrolcsi.muzik.ui.common.ThemedViewHolder
import hu.mrolcsi.muzik.ui.common.extensions.updateNavigationIcons
import hu.mrolcsi.muzik.ui.common.extensions.updateStatusBarIcons
import hu.mrolcsi.muzik.ui.common.observeAndRunNavCommands
import hu.mrolcsi.muzik.ui.common.observeAndRunUiCommands
import hu.mrolcsi.muzik.ui.playlist.PlaylistItem
import hu.mrolcsi.muzik.ui.playlist.PlaylistViewModel
import hu.mrolcsi.muzik.ui.playlist.PlaylistViewModelImpl
import kotlinx.android.synthetic.main.fragment_player.*
import kotlinx.android.synthetic.main.fragment_player_content.*
import kotlinx.android.synthetic.main.fragment_playlist.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import kotlin.math.abs

class PlayerDialogFragment : FullScreenDialogFragment() {

  private val playerViewModel: PlayerViewModel by viewModel<PlayerViewModelImpl>()
  private val playlistViewModel: PlaylistViewModel by viewModel<PlaylistViewModelImpl>()

  private lateinit var binding: FragmentPlayerBinding

  private val queueAdapter by lazy {
    MVVMListAdapter(
      itemIdSelector = { it.queueId },
      viewHolderFactory = { parent, _ ->
        BoundMVVMViewHolder<QueueItem>(
          parent = parent,
          layoutId = R.layout.list_item_queue,
          onModelChange = { model ->
            (this as? ListItemQueueBinding)?.let { binding ->
              binding.tvArtist.setOnClickListener(
                View.OnClickListener {
                  playerViewModel.onArtistClick(model.artistId!!)
                }.takeIf { model.artistId != null }
              )
              binding.tvAlbum.setOnClickListener(
                View.OnClickListener {
                  playerViewModel.onAlbumClick(model.albumId!!)
                }.takeIf { model.albumId != null }
              )
            }
          }
        )
      }
    )
  }

  private val playlistAdapter by lazy {
    MVVMListAdapter(
      itemIdSelector = { it.id },
      viewHolderFactory = { parent, _ ->
        ThemedViewHolder<PlaylistItem>(
          parent,
          R.layout.list_item_playlist,
          viewLifecycleOwner,
          playlistViewModel.currentTheme
        ) { model, _ ->
          playlistViewModel.onSelect(model)
        }
      }
    )
  }

  //region LIFECYCLE

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    activity?.onBackPressedDispatcher?.addCallback(this) {
      if (drawerLayout.isDrawerOpen(GravityCompat.END))
        drawerLayout.closeDrawer(GravityCompat.END)
      else
        findNavController().navigateUp()
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
    playerViewModel.apply {
      requireContext().observeAndRunUiCommands(viewLifecycleOwner, this)
      findNavController().observeAndRunNavCommands(viewLifecycleOwner, this)

      queueState.observe(viewLifecycleOwner, Observer { state ->
        Timber.d("onQueueStateChanged($state)")
        queueAdapter.onChanged(state.queue)
        updatePager(state.activeQueueId)
      })
    }

    playlistViewModel.items.observe(viewLifecycleOwner, Observer { playlist ->
      playlist
        .indexOfFirst { it.isPlaying }
        .takeUnless { it < 0 }
        ?.let { nowPlayingPosition ->
          rvPlaylist.scrollToPosition(nowPlayingPosition)
        }
    })

    setupToolbar()
    setupPager()
    setupControls()
    setupPlaylist()
  }

  override fun onDestroyView() {
    super.onDestroyView()

    queuePager.adapter = null
    rvPlaylist.adapter = null
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
    playerViewModel.currentTheme.observe(viewLifecycleOwner, Observer {
      applyTheme(it.backgroundColor, it.foregroundColor)
    })
    queuePager.adapter = queueAdapter

    queuePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
      override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        // Blend colors while scrolling

        val leftTheme = queueAdapter.currentList[position].theme
        val rightTheme = queueAdapter.currentList.getOrNull(position + 1)?.theme

        val backgroundColor = ColorUtils.blendARGB(
          leftTheme.backgroundColor,
          rightTheme?.backgroundColor ?: leftTheme.backgroundColor,
          positionOffset
        )
        val foregroundColor = ColorUtils.blendARGB(
          leftTheme.foregroundColor,
          rightTheme?.foregroundColor ?: leftTheme.foregroundColor,
          positionOffset
        )

        applyTheme(backgroundColor, foregroundColor)
      }

      override fun onPageScrollStateChanged(state: Int) {
        if (state == ViewPager2.SCROLL_STATE_IDLE) {
          // check if item position is different from the now playing position
          val queueId = playerViewModel.queueState.value?.activeQueueId
          val visibleId = queueAdapter.getItemId(queuePager.currentItem)

          Timber.d("onPageScrollStateChanged($state) queueId=$queueId visibleId=$visibleId")

          if (queueId != visibleId) playerViewModel.skipToQueueItem(visibleId)
        }
      }
    })
  }

  private fun updatePager(queueId: Long) {
    if (queueId < 0) {
      Timber.v("updatePager(queueId=$queueId) Update cancelled.")
      return
    }

    // Scroll pager to now playing item
    val visiblePosition = queuePager.currentItem

    val visibleId = queueAdapter.currentList[visiblePosition].queueId
    val queuePosition = queueAdapter.currentList.indexOfFirst { it.queueId == queueId }

    Timber.v(
      "updatePager(visiblePosition=$visiblePosition, visibleId=$visibleId, queuePosition=$queuePosition, queueId=$queueId) ScrollState=${queuePager.scrollState}"
    )

    if (queuePager.scrollState == ViewPager2.SCROLL_STATE_IDLE) {
      if (visiblePosition != queuePosition) {
        // Scroll to now playing song
        Timber.i("Scroll to position: $visiblePosition -> $queuePosition")
        queuePager.setCurrentItem(queuePosition, abs(queuePosition - visiblePosition) == 1)
      } else {
        // Pager is in the right position, make it visible
        showPager()
      }
    }
  }

  private fun setupControls() {
    btnPrevious.setOnClickListener {
      queuePager.currentItem = queuePager.currentItem - 1
    }

    btnNext.setOnClickListener {
      queuePager.currentItem = queuePager.currentItem + 1
    }
  }

  private fun setupPlaylist() {
    playlistViewModel.items.observe(viewLifecycleOwner, playlistAdapter)
    rvPlaylist.adapter = playlistAdapter
  }

  private fun showPager() {
    ViewCompat.animate(queuePager)
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
}
