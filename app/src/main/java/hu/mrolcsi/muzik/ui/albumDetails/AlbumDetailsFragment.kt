package hu.mrolcsi.muzik.ui.albumDetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.data.model.media.albumArtUri
import hu.mrolcsi.muzik.databinding.FragmentAlbumDetailsBinding
import hu.mrolcsi.muzik.ui.albums.DiscNumberItem
import hu.mrolcsi.muzik.ui.common.HideViewOnOffsetChangedListener
import hu.mrolcsi.muzik.ui.common.MVVMListAdapter
import hu.mrolcsi.muzik.ui.common.MVVMViewHolder
import hu.mrolcsi.muzik.ui.common.ThemedViewHolder
import hu.mrolcsi.muzik.ui.common.extensions.applySharedElementTransition
import hu.mrolcsi.muzik.ui.common.glide.GlideApp
import hu.mrolcsi.muzik.ui.songs.SongItem
import kotlinx.android.synthetic.main.fragment_album_details.*
import kotlinx.android.synthetic.main.fragment_album_details_header.*
import org.koin.androidx.viewmodel.ext.android.viewModel

const val VIEW_TYPE_DISC_NUMBER = 3482
const val VIEW_TYPE_SONG = 8664

class AlbumDetailsFragment : Fragment() {

  private val args by navArgs<AlbumDetailsFragmentArgs>()

  private val viewModel: AlbumDetailsViewModel by viewModel<AlbumDetailsViewModelImpl>()

  @Suppress("UNCHECKED_CAST")
  private val songsAdapter by lazy {
    object : MVVMListAdapter<AlbumDetailItem, MVVMViewHolder<AlbumDetailItem>>(
      itemIdSelector = { it.id },
      viewHolderFactory = { parent, viewType ->
        if (viewType == VIEW_TYPE_DISC_NUMBER) {
          ThemedViewHolder<DiscNumberItem>(
            parent,
            R.layout.list_item_disc_number,
            viewLifecycleOwner,
            viewModel.albumTheme
          ) as MVVMViewHolder<AlbumDetailItem>
        } else {
          ThemedViewHolder<SongItem>(
            parent,
            R.layout.list_item_song_track_number,
            viewLifecycleOwner,
            viewModel.albumTheme
          ) as MVVMViewHolder<AlbumDetailItem>
        }
      }
    ) {
      override fun getItemViewType(position: Int) =
        when (getItem(position)) {
          is DiscNumberItem -> VIEW_TYPE_DISC_NUMBER
          is SongItem -> VIEW_TYPE_SONG
          else -> -1
        }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    applySharedElementTransition(
      R.transition.cover_art_transition,
      requireContext().resources.getInteger(R.integer.preferredAnimationDuration).toLong()
    )
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    viewModel.apply {

      setArgument(args.albumId)

      items.observe(viewLifecycleOwner, songsAdapter)

      albumItem.observe(viewLifecycleOwner, Observer {
        GlideApp.with(imgCoverArt)
          .asBitmap()
          .load(it.description.albumArtUri)
          .into(imgCoverArt)
      })
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
    FragmentAlbumDetailsBinding.inflate(inflater, container, false).also {
      it.lifecycleOwner = viewLifecycleOwner
      it.viewModel = viewModel
      it.theme = viewModel.albumTheme
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvSongs.adapter = songsAdapter

    ViewCompat.setTransitionName(imgCoverArt, args.transitionName)

    albumDetailsToolbar.setupWithNavController(findNavController())

    // Fix height of AppBarLayout with fitsSystemWindows="true"
    ViewCompat.setOnApplyWindowInsetsListener(appBar) { _, insets ->
      (albumDetailsToolbar.layoutParams as ViewGroup.MarginLayoutParams).topMargin = insets.systemWindowInsetTop
      insets.consumeSystemWindowInsets()
    }

    appBar.addOnOffsetChangedListener(HideViewOnOffsetChangedListener(tvAlbumTitle))
  }
}