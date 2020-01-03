package hu.mrolcsi.muzik.ui.albums

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.forEach
import androidx.databinding.library.baseAdapters.BR
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.databinding.FragmentAlbumsBinding
import hu.mrolcsi.muzik.ui.common.BoundMVVMViewHolder
import hu.mrolcsi.muzik.ui.common.MVVMListAdapter
import hu.mrolcsi.muzik.ui.common.glide.GlideApp
import hu.mrolcsi.muzik.ui.common.glide.onResourceReady
import hu.mrolcsi.muzik.ui.common.observeAndRunNavCommands
import hu.mrolcsi.muzik.ui.common.observeAndRunUiCommands
import hu.mrolcsi.muzik.ui.library.SortingMode
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_albums.*
import kotlinx.android.synthetic.main.list_item_album_content.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class AlbumsFragment : Fragment() {

  private val viewModel: AlbumsViewModel by viewModel<AlbumsViewModelImpl>()

  private val albumsAdapter by lazy {
    MVVMListAdapter(
      itemIdSelector = { it.id },
      viewHolderFactory = { parent, _ ->
        BoundMVVMViewHolder<AlbumItem>(
          parent = parent,
          layoutId = R.layout.list_item_album_vertical,
          onItemClickListener = { model, holder ->
            viewModel.onAlbumClick(model, holder.itemView.imgCoverArt)
          },
          onModelChange = { model ->
            this.root.findViewById<ImageView>(R.id.imgCoverArt)?.let { imgCoverArt ->
              GlideApp.with(imgCoverArt)
                .asBitmap()
                .load(model.albumArtUri)
                .onResourceReady { albumArt ->
                  viewModel.themeService
                    .createTheme(albumArt)
                    .subscribeBy {
                      setVariable(BR.theme, it)
                      executePendingBindings()
                    }
                }
                .into(imgCoverArt)
            }
          }
        )
      }
    )
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

  // TODO: reintroduce sorting
//  override fun onOptionsItemSelected(item: MenuItem): Boolean {
//    item.isChecked = true
//    return when (item.itemId) {
//      R.id.menuSortByArtist -> {
//        viewModel.sortingMode = SortingMode.SORT_BY_ARTIST
//        albumsAdapter.sorting = SortingMode.SORT_BY_ARTIST
//        true
//      }
//      R.id.menuSortByTitle -> {
//        viewModel.sortingMode = SortingMode.SORT_BY_TITLE
//        albumsAdapter.sorting = SortingMode.SORT_BY_TITLE
//        true
//      }
//      else -> super.onOptionsItemSelected(item)
//    }
//  }
}