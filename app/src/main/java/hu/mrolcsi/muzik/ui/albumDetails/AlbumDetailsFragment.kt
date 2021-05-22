package hu.mrolcsi.muzik.ui.albumDetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import com.squareup.picasso.Picasso
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.data.model.media.albumArtUri
import hu.mrolcsi.muzik.databinding.FragmentAlbumDetailsBinding
import hu.mrolcsi.muzik.ui.albums.DiscNumberItem
import hu.mrolcsi.muzik.ui.common.HideViewOnOffsetChangedListener
import hu.mrolcsi.muzik.ui.common.MVVMListAdapter
import hu.mrolcsi.muzik.ui.common.MVVMViewHolder
import hu.mrolcsi.muzik.ui.common.ThemedViewHolder
import hu.mrolcsi.muzik.ui.common.extensions.applySharedElementTransition
import hu.mrolcsi.muzik.ui.common.extensions.updateStatusBarIcons
import hu.mrolcsi.muzik.ui.songs.SongItem
import kotlinx.android.synthetic.main.fragment_album_details.*
import kotlinx.android.synthetic.main.fragment_album_details_header.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class AlbumDetailsFragment : Fragment() {

  private val args by navArgs<AlbumDetailsFragmentArgs>()

  private val viewModel: AlbumDetailsViewModel by viewModel<AlbumDetailsViewModelImpl>()

  @Suppress("UNCHECKED_CAST")
  private val songsAdapter by lazy {
    MVVMListAdapter(
      itemIdSelector = { it.id },
      viewTypeSelector = { model ->
        when (model) {
          is DiscNumberItem -> R.layout.list_item_disc_number
          is SongItem -> R.layout.list_item_song_track_number
          else -> -1
        }
      },
      viewHolderFactory = { parent, viewType ->
        when (viewType) {
          R.layout.list_item_disc_number -> {
            ThemedViewHolder<DiscNumberItem>(
              parent = parent,
              layoutId = R.layout.list_item_disc_number,
              viewLifecycleOwner = viewLifecycleOwner,
              theme = viewModel.albumTheme
            ) as MVVMViewHolder<AlbumDetailItem>
          }
          R.layout.list_item_song_track_number -> {
            ThemedViewHolder<SongItem>(
              parent = parent,
              layoutId = R.layout.list_item_song_track_number,
              viewLifecycleOwner = viewLifecycleOwner,
              theme = viewModel.albumTheme,
              onItemClickListener = { item, _ -> viewModel.onSongClick(item) }
            ) as MVVMViewHolder<AlbumDetailItem>
          }
          else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
      }
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    viewModel.setArgument(args.albumId)

    applySharedElementTransition(
      R.transition.cover_art_transition,
      requireContext().resources.getInteger(R.integer.preferredAnimationDuration).toLong()
    )
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
    FragmentAlbumDetailsBinding.inflate(inflater, container, false).also {
      it.lifecycleOwner = viewLifecycleOwner
      it.viewModel = viewModel
      it.theme = viewModel.albumTheme
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    viewModel.apply {
      items.observe(viewLifecycleOwner, songsAdapter)

      albumItem.observe(viewLifecycleOwner, {
        Picasso.get()
          .load(it.description.albumArtUri)
          .into(imgCoverArt)
      })

      albumTheme.observe(viewLifecycleOwner, {
        activity?.window?.updateStatusBarIcons(it.backgroundColor)
      })
    }

    rvSongs.adapter = songsAdapter

    albumDetailsToolbar.setupWithNavController(findNavController())

    // Fix height of AppBarLayout with fitsSystemWindows="true"
    ViewCompat.setOnApplyWindowInsetsListener(appBar) { _, insets ->
      (albumDetailsToolbar.layoutParams as ViewGroup.MarginLayoutParams).topMargin = insets.systemWindowInsetTop
      insets.consumeSystemWindowInsets()
    }

    appBar.addOnOffsetChangedListener(HideViewOnOffsetChangedListener(tvAlbumTitle))
  }

  override fun onDestroyView() {
    super.onDestroyView()

    rvSongs.adapter = null
  }
}