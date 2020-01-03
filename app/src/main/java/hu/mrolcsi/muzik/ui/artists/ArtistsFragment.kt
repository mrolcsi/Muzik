package hu.mrolcsi.muzik.ui.artists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.databinding.FragmentArtistsBinding
import hu.mrolcsi.muzik.ui.common.MVVMListAdapter
import hu.mrolcsi.muzik.ui.common.ThemedViewHolder
import hu.mrolcsi.muzik.ui.common.observeAndRunNavCommands
import hu.mrolcsi.muzik.ui.common.observeAndRunUiCommands
import kotlinx.android.synthetic.main.fragment_artists.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class ArtistsFragment : Fragment() {

  private val viewModel: ArtistsViewModel by viewModel<ArtistsViewModelImpl>()

  private val artistAdapter by lazy {
    MVVMListAdapter(
      itemIdSelector = { it.id },
      viewHolderFactory = { parent, _ ->
        ThemedViewHolder<ArtistItem>(
          parent = parent,
          layoutId = R.layout.list_item_artist,
          viewLifecycleOwner = viewLifecycleOwner,
          theme = viewModel.currentTheme
        ) { model, _ ->
          viewModel.onSelect(model)
        }
      }
    )
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