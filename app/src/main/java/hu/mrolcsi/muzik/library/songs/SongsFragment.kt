package hu.mrolcsi.muzik.library.songs

import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.ColoredDividerItemDecoration
import hu.mrolcsi.muzik.common.fastscroller.AutoHidingFastScrollerTouchListener
import hu.mrolcsi.muzik.common.fastscroller.SimpleSectionIndicator
import hu.mrolcsi.muzik.extensions.OnItemClickListener
import hu.mrolcsi.muzik.extensions.applyForegroundColor
import hu.mrolcsi.muzik.library.SortingMode
import hu.mrolcsi.muzik.service.exoplayer.ExoPlayerHolder
import hu.mrolcsi.muzik.service.extensions.media.MediaType
import hu.mrolcsi.muzik.service.extensions.media.addQueueItems
import hu.mrolcsi.muzik.service.extensions.media.clearQueue
import hu.mrolcsi.muzik.service.extensions.media.playFromDescription
import hu.mrolcsi.muzik.service.extensions.media.type
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_songs.*

class SongsFragment : Fragment() {

  private lateinit var mSongsModel: SongsViewModel

  private lateinit var mVisibleSongs: List<MediaBrowserCompat.MediaItem>

  private val mDivider by lazy {
    ColoredDividerItemDecoration(requireContext(), LinearLayout.VERTICAL).apply {
      setDrawable(resources.getDrawable(R.drawable.list_divider_inset, requireContext().theme))
    }
  }

  private val mSongsAdapter by lazy {
    SongsAdapter(requireContext(), OnItemClickListener { item, holder, position, id ->
      Log.d(LOG_TAG, "onItemClicked($item, $holder, $position, $id)")

      val controller = MediaControllerCompat.getMediaController(requireActivity())

      if (item.description.type == MediaType.MEDIA_OTHER) {
        // Shuffle All
        controller.transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
        controller.clearQueue()

        val description = mVisibleSongs
          .filter { it.isPlayable }
          .map { it.description }

        controller.addQueueItems(description)
        controller.transportControls.play()

      } else {
        // Immediately start the song that was clicked on
        controller.transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
        controller.transportControls.playFromDescription(
          item.description,
          bundleOf(ExoPlayerHolder.EXTRA_DESIRED_QUEUE_POSITION to position - 1)
        )

        AsyncTask.execute {
          Log.d(LOG_TAG, "onItemClicked() Collecting descriptions...")

          // Add songs to queue
          val descriptions = mVisibleSongs.filterIndexed { index, item ->
            item.isPlayable && index != position
          }.map {
            it.description
          }

          Log.d(LOG_TAG, "onItemClicked() Sending items to queue...")

          controller.addQueueItems(descriptions)
        }
      }
    })
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    setHasOptionsMenu(true)

    activity?.run {
      mSongsModel = ViewModelProviders.of(this).get(SongsViewModel::class.java).apply {
        songs.observe(viewLifecycleOwner, Observer { songs ->
          mSongsAdapter.submitList(songs)
          mVisibleSongs = songs
        })
        sorting.observe(viewLifecycleOwner, Observer {
          // Update adapter
          mSongsAdapter.sorting = it

          if (it == SortingMode.SORT_BY_DATE) {
            sectionIndicator.setIndicatorTextSize(18)
          } else {
            sectionIndicator.setIndicatorTextSize(SimpleSectionIndicator.DEFAULT_TEXT_SIZE)
          }
        })
      }
    }

    ThemeManager.getInstance(requireContext()).currentTheme.observe(viewLifecycleOwner, Observer {
      // Tell adapter to reload its views
      mSongsAdapter.notifyDataSetChanged()

      // Apply colors to dividers
      mDivider.setTint(it.secondaryForegroundColor)

      // Apply colors to FastScroller
      fastScroller.applyForegroundColor(requireContext(), it.secondaryForegroundColor)

      // Apply colors to SectionIndicator
      sectionIndicator.setIndicatorBackgroundColor(it.secondaryForegroundColor)
      sectionIndicator.setIndicatorTextColor(it.secondaryBackgroundColor)
    })
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_songs, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvSongs.apply {
      adapter = mSongsAdapter
      addItemDecoration(mDivider)

      fastScroller.setRecyclerView(this)
      fastScroller.setOnTouchListener(AutoHidingFastScrollerTouchListener(fastScroller).also {
        addOnScrollListener(it.autoHideOnScrollListener)
      })

      fastScroller.sectionIndicator = sectionIndicator
    }
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.menu_songs, menu)

    // Apply theme to items
    val color = ThemeManager.getInstance(requireContext()).currentTheme.value?.primaryForegroundColor ?: Color.WHITE
    menu.forEach {
      it.icon.setTint(color)
    }
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)

    when (mSongsModel.sorting.value) {
      SortingMode.SORT_BY_ARTIST -> menu.findItem(R.id.menuSortByArtist).isChecked = true
      SortingMode.SORT_BY_TITLE -> menu.findItem(R.id.menuSortByTitle).isChecked = true
      SortingMode.SORT_BY_DATE -> menu.findItem(R.id.menuSortByDate).isChecked = true
      else -> {
        // nothing
      }
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    item.isChecked = true
    return when (item.itemId) {
      R.id.menuSortByArtist -> {
        mSongsModel.sorting.value = SortingMode.SORT_BY_ARTIST
        true
      }
      R.id.menuSortByTitle -> {
        mSongsModel.sorting.value = SortingMode.SORT_BY_TITLE
        true
      }
      R.id.menuSortByDate -> {
        mSongsModel.sorting.value = SortingMode.SORT_BY_DATE
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  companion object {
    private const val LOG_TAG = "SongsFragment"
  }
}