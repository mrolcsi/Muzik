package hu.mrolcsi.muzik.library.albums

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.MediaItemListAdapter
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunNavCommands
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunUiCommands
import hu.mrolcsi.muzik.databinding.FragmentAlbumsBinding
import hu.mrolcsi.muzik.library.SortingMode
import hu.mrolcsi.muzik.theme.ThemeService
import kotlinx.android.synthetic.main.fragment_albums.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class AlbumsFragment : Fragment() {

  private val viewModel: AlbumsViewModel by viewModel<AlbumsViewModelImpl>()
  private val themeService: ThemeService by inject()

  private val albumsAdapter by lazy {
    MediaItemListAdapter(requireContext()) { parent, _ ->
      AlbumHolder(
        parent = parent,
        viewLifecycleOwner = viewLifecycleOwner,
        orientation = RecyclerView.VERTICAL,
        themeService = themeService
      ).apply {
        itemView.setOnClickListener { view ->
          model?.let {
            viewModel.onAlbumClick(it, view.findViewById(R.id.imgCoverArt))
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

      requireContext().observeAndRunUiCommands(viewLifecycleOwner, this)
      findNavController().observeAndRunNavCommands(viewLifecycleOwner, this)

      items.observe(viewLifecycleOwner, albumsAdapter)
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
    FragmentAlbumsBinding.inflate(inflater, container, false).apply {
      theme = viewModel.currentTheme
      lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvAlbums.adapter = albumsAdapter
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.menu_albums, menu)

    // Apply theme to items
    val color = viewModel.currentTheme.value?.primaryForegroundColor ?: Color.WHITE
    menu.forEach {
      it.icon.setTint(color)
    }
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)

    when (viewModel.sortingMode) {
      SortingMode.SORT_BY_ARTIST -> menu.findItem(R.id.menuSortByArtist).isChecked = true
      SortingMode.SORT_BY_TITLE -> menu.findItem(R.id.menuSortByTitle).isChecked = true
      else -> {
        // nothing
      }
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    item.isChecked = true
    return when (item.itemId) {
      R.id.menuSortByArtist -> {
        viewModel.sortingMode = SortingMode.SORT_BY_ARTIST
        albumsAdapter.sorting = SortingMode.SORT_BY_ARTIST
        true
      }
      R.id.menuSortByTitle -> {
        viewModel.sortingMode = SortingMode.SORT_BY_TITLE
        albumsAdapter.sorting = SortingMode.SORT_BY_TITLE
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }
}