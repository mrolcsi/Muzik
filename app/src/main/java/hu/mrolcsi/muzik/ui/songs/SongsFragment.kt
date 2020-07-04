package hu.mrolcsi.muzik.ui.songs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.databinding.FragmentSongsBinding
import hu.mrolcsi.muzik.ui.common.MVVMListAdapter
import hu.mrolcsi.muzik.ui.common.ThemedViewHolder
import hu.mrolcsi.muzik.ui.common.observeAndRunNavCommands
import hu.mrolcsi.muzik.ui.common.observeAndRunUiCommands
import hu.mrolcsi.muzik.ui.library.SortingMode
import kotlinx.android.synthetic.main.fragment_songs.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class SongsFragment : Fragment() {

  private val viewModel: SongsViewModel by viewModel<SongsViewModelImpl>()

  private val songsAdapter by lazy {
    MVVMListAdapter(
      itemIdSelector = { it.id },
      viewHolderFactory = { parent, _ ->
        ThemedViewHolder<SongItem>(
          parent = parent,
          layoutId = R.layout.list_item_song_cover_art,
          viewLifecycleOwner = viewLifecycleOwner,
          theme = viewModel.currentTheme,
          onItemClickListener = { model, holder ->
            viewModel.onSongClick(model, holder.bindingAdapterPosition)
          },
          onModelChange = {
            this.root.setOnLongClickListener { showSortingMenu(it); true }
          }
        )
      }
    )
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
    FragmentSongsBinding.inflate(inflater, container, false).also {
      it.viewModel = viewModel
      it.theme = viewModel.currentTheme
      it.lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    viewModel.apply {
      requireContext().observeAndRunUiCommands(viewLifecycleOwner, this)
      findNavController().observeAndRunNavCommands(viewLifecycleOwner, this)

      items.observe(viewLifecycleOwner, songsAdapter)
    }

    rvSongs.adapter = songsAdapter
    fastScroller.attachRecyclerView(rvSongs)
  }

  override fun onDestroyView() {
    super.onDestroyView()

    rvSongs.adapter = null
    fastScroller.detachRecyclerView()
  }

  private fun showSortingMenu(anchor: View) {
    PopupMenu(requireContext(), anchor).apply {
      inflate(R.menu.menu_songs)
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
          R.id.menuSortByDate -> {
            viewModel.sortingMode = SortingMode.SORT_BY_DATE
            true
          }
          else -> super.onOptionsItemSelected(item)
        }
      }
      show()
    }
  }
}