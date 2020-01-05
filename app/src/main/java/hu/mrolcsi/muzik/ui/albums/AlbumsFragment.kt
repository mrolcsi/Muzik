package hu.mrolcsi.muzik.ui.albums

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.library.baseAdapters.BR
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.databinding.FragmentAlbumsBinding
import hu.mrolcsi.muzik.ui.common.BoundMVVMViewHolder
import hu.mrolcsi.muzik.ui.common.IndexedMVVMListAdapter
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
    IndexedMVVMListAdapter(
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

            this.root.setOnLongClickListener { showSortingMenu(it); true }
          }
        )
      },
      sectionTextSelector = { viewModel.getSectionText(it) }
    )
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

  private fun showSortingMenu(anchor: View) {
    PopupMenu(requireContext(), anchor).apply {
      inflate(R.menu.menu_albums)
      setOnMenuItemClickListener { item ->
        item.isChecked = true
        when (item.itemId) {
          R.id.menuSortByArtist -> {
            viewModel.sortingMode = SortingMode.SORT_BY_ARTIST
            true
          }
          R.id.menuSortByTitle -> {
            viewModel.sortingMode = SortingMode.SORT_BY_TITLE
            true
          }
          else -> super.onOptionsItemSelected(item)
        }
      }
      show()
    }
  }
}