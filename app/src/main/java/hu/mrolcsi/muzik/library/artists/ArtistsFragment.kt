package hu.mrolcsi.muzik.library.artists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import hu.mrolcsi.muzik.common.MediaItemListAdapter
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunNavCommands
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunUiCommands
import hu.mrolcsi.muzik.databinding.FragmentArtistsBinding
import hu.mrolcsi.muzik.databinding.ListItemArtistBinding
import kotlinx.android.synthetic.main.fragment_artists.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class ArtistsFragment : Fragment() {

  private val viewModel: ArtistsViewModel by viewModel<ArtistsViewModelImpl>()

  private val artistAdapter by lazy {
    MediaItemListAdapter(requireContext()) { parent, _ ->
      ArtistHolder(
        ListItemArtistBinding.inflate(
          LayoutInflater.from(parent.context),
          parent,
          false
        ).apply {
          lifecycleOwner = viewLifecycleOwner
          theme = viewModel.currentTheme
        }
      ).apply {
        itemView.setOnClickListener {
          model?.let {
            viewModel.onSelect(it)
          }
        }
      }
    }
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    viewModel.apply {

      requireContext().observeAndRunUiCommands(viewLifecycleOwner, this)
      findNavController().observeAndRunNavCommands(viewLifecycleOwner, this)

      items.observe(viewLifecycleOwner, artistAdapter)
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
    FragmentArtistsBinding.inflate(inflater, container, false).also { binding ->
      binding.lifecycleOwner = viewLifecycleOwner
      binding.theme = viewModel.currentTheme
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvArtists.adapter = artistAdapter
  }
}