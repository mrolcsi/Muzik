package hu.mrolcsi.muzik.player.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import hu.mrolcsi.muzik.common.view.MVVMListAdapter
import hu.mrolcsi.muzik.databinding.FragmentPlaylistBinding
import hu.mrolcsi.muzik.databinding.ListItemPlaylistBinding
import kotlinx.android.synthetic.main.fragment_playlist.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class PlaylistFragment : Fragment() {

  private val viewModel: PlaylistViewModel by viewModel<PlaylistViewModelImpl>()

  private val playlistAdapter = MVVMListAdapter(
    diffCallback = PlaylistItem.DIFF_CALLBACK,
    itemIdSelector = { it.entry._id },
    viewHolderFactory = { parent, _ ->
      PlaylistItemHolder(
        ListItemPlaylistBinding.inflate(
          LayoutInflater.from(parent.context),
          parent,
          false
        ).apply {
          theme = viewModel.currentTheme
          lifecycleOwner = viewLifecycleOwner
        }.root
      ).apply {
        itemView.setOnClickListener {
          model?.let { viewModel.onSelect(it) }
        }
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