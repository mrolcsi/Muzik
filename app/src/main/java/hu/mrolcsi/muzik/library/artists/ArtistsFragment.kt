package hu.mrolcsi.muzik.library.artists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import dagger.android.support.DaggerFragment
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.ColoredDividerItemDecoration
import hu.mrolcsi.muzik.common.MediaItemListAdapter
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunNavCommands
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunUiCommands
import hu.mrolcsi.muzik.databinding.FragmentArtistsBinding
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_artists.*
import javax.inject.Inject

class ArtistsFragment : DaggerFragment() {

  @Inject lateinit var viewModel: ArtistsViewModel

  private val artistAdapter by lazy {
    MediaItemListAdapter(requireContext()) { parent, _ ->
      ArtistHolder(
        LayoutInflater
          .from(parent.context)
          .inflate(R.layout.list_item_artist, parent, false)
      ).apply {
        itemView.setOnClickListener {
          model?.let {
            viewModel.onSelect(it)
          }
        }
      }
    }
  }

  private val divider by lazy {
    ColoredDividerItemDecoration(requireContext(), LinearLayout.VERTICAL)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    viewModel.apply {

      requireContext().observeAndRunUiCommands(viewLifecycleOwner, this)
      findNavController().observeAndRunNavCommands(viewLifecycleOwner, this)

      items.observe(viewLifecycleOwner, Observer { artists ->
        artistAdapter.submitList(artists)
      })
    }

    ThemeManager.getInstance(requireContext()).currentTheme.observe(viewLifecycleOwner, Observer {
      // Tell adapter to reload its views
      artistAdapter.notifyDataSetChanged()

      divider.setTint(it.secondaryForegroundColor)

      rvArtists.fastScroller.apply {
        setTrackColor(it.secondaryForegroundColor)
        setHandleColor(it.secondaryForegroundColor)
        setBubbleColor(it.secondaryForegroundColor)

        setBubbleTextColor(it.secondaryBackgroundColor)
      }
    })
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return FragmentArtistsBinding.inflate(inflater, container, false).also { binding ->
      binding.lifecycleOwner = viewLifecycleOwner
      binding.viewModel = viewModel
    }.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvArtists.setAdapter(artistAdapter)
    rvArtists.recyclerView.apply {
      addItemDecoration(divider)
    }
  }
}