package hu.mrolcsi.muzik.library.songs

import android.graphics.Color
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.ColoredDividerItemDecoration
import hu.mrolcsi.muzik.common.MediaItemListAdapter
import hu.mrolcsi.muzik.extensions.observeOnce
import hu.mrolcsi.muzik.library.SortingMode
import hu.mrolcsi.muzik.service.extensions.media.MediaType
import hu.mrolcsi.muzik.service.extensions.media.addQueueItems
import hu.mrolcsi.muzik.service.extensions.media.clearQueue
import hu.mrolcsi.muzik.service.extensions.media.playFromMediaItems
import hu.mrolcsi.muzik.service.extensions.media.setQueueTitle
import hu.mrolcsi.muzik.service.extensions.media.type
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_songs.*

class SongsFragment : Fragment() {

  private lateinit var viewModel: SongsViewModel

  private val divider by lazy {
    ColoredDividerItemDecoration(requireContext(), LinearLayout.VERTICAL).apply {
      setDrawable(resources.getDrawable(R.drawable.list_divider_inset, requireContext().theme))
    }
  }

  private val songsAdapter by lazy {
    MediaItemListAdapter(requireContext()) { parent, _ ->
      SongHolder(
        LayoutInflater
          .from(parent.context)
          .inflate(R.layout.list_item_song, parent, false),
        false
      ).apply {
        itemView.setOnClickListener {
          model?.let {
            val controller = MediaControllerCompat.getMediaController(requireActivity())

            controller.setQueueTitle(getString(R.string.playlist_allSongs))

            if (it.description.type == MediaType.MEDIA_OTHER) {
              // Shuffle All
              viewModel.songDescriptions.observeOnce(viewLifecycleOwner, Observer { descriptions ->
                controller.clearQueue()
                controller.transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
                controller.addQueueItems(descriptions)
                controller.transportControls.play()
              })
            } else {
              // Immediately start the song that was clicked on
              viewModel.songs.observeOnce(viewLifecycleOwner, Observer { items ->
                controller.playFromMediaItems(items, adapterPosition)
                controller.transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
              })
            }
          }
        }
      }
    }
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    setHasOptionsMenu(true)

    activity?.run {
      viewModel = ViewModelProviders.of(this).get(SongsViewModel::class.java).apply {
        songs.observe(viewLifecycleOwner, Observer { songs ->
          songsAdapter.submitList(songs)
          //mVisibleItems = songs
        })
        sorting.observe(viewLifecycleOwner, Observer {
          // Update adapter
          songsAdapter.sorting = it

          if (it == SortingMode.SORT_BY_DATE) {
            rvSongs.fastScroller.setBubbleTextSize(18)
          } else {
            rvSongs.fastScroller.setBubbleTextSize(22)
          }
        })
      }
    }

    ThemeManager.getInstance(requireContext()).currentTheme.observe(viewLifecycleOwner, Observer {
      // Tell adapter to reload its views
      songsAdapter.notifyDataSetChanged()

      // Apply colors to dividers
      divider.setTint(it.secondaryForegroundColor)

      rvSongs.fastScroller.apply {
        setTrackColor(it.secondaryForegroundColor)
        setHandleColor(it.secondaryForegroundColor)
        setBubbleColor(it.secondaryForegroundColor)

        setBubbleTextColor(it.secondaryBackgroundColor)
      }
    })
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_songs, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvSongs.setAdapter(songsAdapter)
    rvSongs.recyclerView.apply {
      addItemDecoration(divider)
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

    when (viewModel.sorting.value) {
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
        viewModel.sorting.value = SortingMode.SORT_BY_ARTIST
        true
      }
      R.id.menuSortByTitle -> {
        viewModel.sorting.value = SortingMode.SORT_BY_TITLE
        true
      }
      R.id.menuSortByDate -> {
        viewModel.sorting.value = SortingMode.SORT_BY_DATE
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onStart() {
    super.onStart()
    viewModel.connect()
  }

  override fun onStop() {
    super.onStop()
    viewModel.disconnect()
  }
}