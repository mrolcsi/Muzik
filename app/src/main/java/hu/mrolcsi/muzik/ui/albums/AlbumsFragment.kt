package hu.mrolcsi.muzik.ui.albums

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.library.baseAdapters.BR
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.uber.autodispose.android.lifecycle.autoDispose
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.databinding.FragmentAlbumsBinding
import hu.mrolcsi.muzik.databinding.ListItemAlbumVerticalBinding
import hu.mrolcsi.muzik.ui.common.BoundMVVMViewHolder
import hu.mrolcsi.muzik.ui.common.MVVMListAdapter
import hu.mrolcsi.muzik.ui.common.glide.GlideApp
import hu.mrolcsi.muzik.ui.common.glide.toSingle
import hu.mrolcsi.muzik.ui.common.observeAndRunNavCommands
import hu.mrolcsi.muzik.ui.common.observeAndRunUiCommands
import hu.mrolcsi.muzik.ui.library.SortingMode
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_albums.*
import kotlinx.android.synthetic.main.list_item_album_content.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class AlbumsFragment : Fragment() {

  private val viewModel: AlbumsViewModel by viewModel<AlbumsViewModelImpl>()

  private val albumsAdapter by lazy {
    MVVMListAdapter(
      itemIdSelector = AlbumItem::id,
      viewHolderFactory = { parent, _ ->
        BoundMVVMViewHolder<AlbumItem>(
          parent = parent,
          layoutId = R.layout.list_item_album_vertical,
          onItemClickListener = { model, holder ->
            viewModel.onAlbumClick(model, holder.itemView.imgCoverArt)
          },
          onModelChange = { model ->
            (this as ListItemAlbumVerticalBinding).incAlbumContent.imgCoverArt.let { imgCoverArt ->
              GlideApp.with(imgCoverArt)
                .asBitmap()
                .load(model.albumArtUri)
                .toSingle()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(imgCoverArt::setImageBitmap)
                .flatMap(viewModel.themeService::createTheme)
                .autoDispose(viewLifecycleOwner)
                .subscribe(
                  {
                    setVariable(BR.theme, it)
                    requireView().post(this::executePendingBindings)
                  },
                  { Timber.e(it) }
                )
            }

            this.root.setOnLongClickListener { showSortingMenu(it); true }
          }
        )
      }
    )
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
    FragmentAlbumsBinding.inflate(inflater, container, false).also {
      it.viewModel = viewModel
      it.theme = viewModel.currentTheme
      it.lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    viewModel.apply {
      requireContext().observeAndRunUiCommands(viewLifecycleOwner, this)
      findNavController().observeAndRunNavCommands(viewLifecycleOwner, this)

      items.observe(viewLifecycleOwner, albumsAdapter)
    }

    rvAlbums.adapter = albumsAdapter
    fastScroller.attachRecyclerView(rvAlbums)
  }

  override fun onDestroyView() {
    super.onDestroyView()

    rvAlbums.adapter = null
    fastScroller.detachRecyclerView()
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