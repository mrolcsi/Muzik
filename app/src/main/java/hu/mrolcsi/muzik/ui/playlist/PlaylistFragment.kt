package hu.mrolcsi.muzik.ui.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.databinding.FragmentPlaylistBinding
import hu.mrolcsi.muzik.ui.common.MVVMListAdapter
import hu.mrolcsi.muzik.ui.common.ThemedViewHolder
import kotlinx.android.synthetic.main.fragment_playlist.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class PlaylistFragment : Fragment() {

  private val viewModel: PlaylistViewModel by viewModel<PlaylistViewModelImpl>()

  private val playlistAdapter = MVVMListAdapter(
    itemIdSelector = { it.id },
    viewHolderFactory = { parent, _ ->
      ThemedViewHolder<PlaylistItem>(
        parent,
        R.layout.list_item_playlist,
        viewLifecycleOwner,
        viewModel.currentTheme
      ) { model, _ ->
        viewModel.onSelect(model)
      }
    }
  )

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    viewModel.items.observe(viewLifecycleOwner, playlistAdapter)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
    FragmentPlaylistBinding.inflate(inflater, container, false).also {
      it.viewModel = viewModel
      it.theme = viewModel.currentTheme
      it.lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvPlaylist.adapter = playlistAdapter
  }
}