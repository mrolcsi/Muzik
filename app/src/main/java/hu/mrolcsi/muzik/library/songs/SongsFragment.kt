package hu.mrolcsi.muzik.library.songs

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.forEach
import androidx.lifecycle.Observer
import dagger.android.support.DaggerFragment
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.ColoredDividerItemDecoration
import hu.mrolcsi.muzik.common.MediaItemListAdapter
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunNavCommands
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunUiCommands
import hu.mrolcsi.muzik.library.SortingMode
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_songs.*
import javax.inject.Inject

class SongsFragment : DaggerFragment() {

  @Inject lateinit var viewModel: SongsViewModel

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
            viewModel.onSongClicked(it, adapterPosition)
          }
        }
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setHasOptionsMenu(true)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    viewModel.apply {

      observeAndRunUiCommands(this)
      observeAndRunNavCommands(this)

      items.observe(viewLifecycleOwner, Observer { songs ->
        songsAdapter.submitList(songs)
      })
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

    when (viewModel.sortingMode) {
      SortingMode.SORT_BY_ARTIST -> menu.findItem(R.id.menuSortByArtist).isChecked = true
      SortingMode.SORT_BY_TITLE -> menu.findItem(R.id.menuSortByTitle).isChecked = true
      SortingMode.SORT_BY_DATE -> menu.findItem(R.id.menuSortByDate).isChecked = true
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    item.isChecked = true
    return when (item.itemId) {
      R.id.menuSortByArtist -> {
        viewModel.sortingMode = SortingMode.SORT_BY_ARTIST
        songsAdapter.sorting = SortingMode.SORT_BY_ARTIST
        true
      }
      R.id.menuSortByTitle -> {
        viewModel.sortingMode = SortingMode.SORT_BY_TITLE
        songsAdapter.sorting = SortingMode.SORT_BY_TITLE
        true
      }
      R.id.menuSortByDate -> {
        viewModel.sortingMode = SortingMode.SORT_BY_DATE
        songsAdapter.sorting = SortingMode.SORT_BY_DATE
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }
}