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
import hu.mrolcsi.muzik.common.fastscroller.AutoHidingFastScrollerTouchListener
import hu.mrolcsi.muzik.extensions.applyForegroundColor
import hu.mrolcsi.muzik.library.artists.details.ArtistDetailsFragmentArgs
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
            findNavController().navigate(
              R.id.navigation_artistDetails,
              ArtistDetailsFragmentArgs(it).toBundle()
            )
          }
        }
      }
    }
  }

  private val mDivider by lazy {
    ColoredDividerItemDecoration(requireContext(), LinearLayout.VERTICAL)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    viewModel.apply {
      items.observe(viewLifecycleOwner, Observer { artists ->
        artistAdapter.submitList(artists)
      })
    }

    ThemeManager.getInstance(requireContext()).currentTheme.observe(viewLifecycleOwner, Observer {
      // Tell adapter to reload its views
      artistAdapter.notifyDataSetChanged()

      mDivider.setTint(it.secondaryForegroundColor)

      fastScroller.applyForegroundColor(requireContext(), it.secondaryForegroundColor)

      sectionIndicator.setIndicatorBackgroundColor(it.secondaryForegroundColor)
      sectionIndicator.setIndicatorTextColor(it.secondaryBackgroundColor)
    })
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_artists, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvArtists.apply {
      adapter = artistAdapter
      addItemDecoration(mDivider)

      fastScroller.setRecyclerView(this)
      fastScroller.setOnTouchListener(AutoHidingFastScrollerTouchListener(fastScroller).also {
        addOnScrollListener(it.autoHideOnScrollListener)
      })

      fastScroller.sectionIndicator = sectionIndicator
    }
  }
}