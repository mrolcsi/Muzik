package hu.mrolcsi.muzik.library.albums.details

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
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
import hu.mrolcsi.muzik.common.MediaItemListAdapter
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.view.MVVMViewHolder
import hu.mrolcsi.muzik.databinding.FragmentAlbumDetailsBinding
import hu.mrolcsi.muzik.databinding.ListItemDiscNumberBinding
import hu.mrolcsi.muzik.databinding.ListItemSongBinding
import hu.mrolcsi.muzik.extensions.applySharedElementTransition
import hu.mrolcsi.muzik.library.songs.SongHolder
import hu.mrolcsi.muzik.service.extensions.media.albumArtUri
import kotlinx.android.synthetic.main.album_details_header.*
import kotlinx.android.synthetic.main.fragment_album_details.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class AlbumDetailsFragment : Fragment() {

  private val args by navArgs<AlbumDetailsFragmentArgs>()

  private val viewModel: AlbumDetailsViewModel by viewModel<AlbumDetailsViewModelImpl>()

  private val songsAdapter by lazy {
    object : MediaItemListAdapter<MVVMViewHolder<MediaItem>>(requireContext(), { parent, viewType ->
      if (viewType == DiscHeaderHolder.VIEW_TYPE) {
        DiscHeaderHolder(
          ListItemDiscNumberBinding.inflate(layoutInflater, parent, false).apply {
            theme = viewModel.albumTheme
            lifecycleOwner = viewLifecycleOwner
          }.root
        )
      } else {
        SongHolder(
          itemView = ListItemSongBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
          ).apply {
            theme = viewModel.albumTheme
            lifecycleOwner = viewLifecycleOwner
          }.root,
          showTrackNumber = true
        ).apply {
          itemView.setOnClickListener {
            model?.let {
              viewModel.onSongClick(it, adapterPosition)
            }
          }
        }
      }
    }) {
      override fun getItemViewType(position: Int) =
        if (getItem(position).isBrowsable) DiscHeaderHolder.VIEW_TYPE else SongHolder.VIEW_TYPE
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
  }
}