package hu.mrolcsi.muzik.library.artists.details

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.DaggerFragment
import hu.mrolcsi.muzik.common.MediaItemListAdapter
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.glide.onResourceReady
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunNavCommands
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunUiCommands
import hu.mrolcsi.muzik.databinding.FragmentArtistDetailsBinding
import hu.mrolcsi.muzik.databinding.ListItemSongBinding
import hu.mrolcsi.muzik.library.albums.AlbumHolder
import hu.mrolcsi.muzik.library.songs.SongHolder
import hu.mrolcsi.muzik.theme.ThemeService
import kotlinx.android.synthetic.main.artist_details_header.*
import kotlinx.android.synthetic.main.fragment_artist_details.*
import kotlinx.android.synthetic.main.fragment_artist_details_content.*
import kotlinx.android.synthetic.main.list_item_album_content.*
import javax.inject.Inject

class ArtistDetailsFragment : DaggerFragment() {

  private val args: ArtistDetailsFragmentArgs by navArgs()

  @Inject lateinit var viewModel: ArtistDetailsViewModel
  @Inject lateinit var themeService: ThemeService

  private val albumsAdapter by lazy {
    MediaItemListAdapter(requireContext()) { parent, _ ->
      AlbumHolder(
        parent = parent,
        viewLifecycleOwner = viewLifecycleOwner,
        orientation = RecyclerView.HORIZONTAL,
        themeService = themeService
      ).apply {
        itemView.setOnClickListener {
          model?.let {
            viewModel.onAlbumClick(it, imgCoverArt)
          }
        }
      }
    }
  }

  private val songsAdapter by lazy {
    MediaItemListAdapter(requireContext()) { parent, _ ->
      SongHolder(
        itemView = ListItemSongBinding.inflate(
          LayoutInflater.from(parent.context),
          parent,
          false
        ).apply {
          theme = viewModel.currentTheme
          lifecycleOwner = viewLifecycleOwner
        }.root,
        showTrackNumber = false
      ).apply {
        itemView.setOnClickListener {
          model?.let {
            viewModel.onSongClick(it, adapterPosition)
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

      artistItem = args.artistItem
      artistItem?.let { loadHeader(it) }

      artistAlbums.observe(viewLifecycleOwner, Observer {
        // Hide Albums section when list is empty
        albumsGroup?.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE

        albumsAdapter.submitList(it)
      })
      artistSongs.observe(viewLifecycleOwner, Observer(songsAdapter::submitList))

      artistPicture.observe(viewLifecycleOwner, Observer { uri ->
        GlideApp.with(this@ArtistDetailsFragment)
          .asBitmap()
          .load(uri)
          .onResourceReady { appBar.setExpanded(true, true) }
          .into(imgArtist)
      })
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
    FragmentArtistDetailsBinding.inflate(inflater, container, false).also {
      it.theme = viewModel.currentTheme
      it.lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvAlbums.adapter = albumsAdapter
    rvSongs.adapter = songsAdapter
  }

  private fun loadHeader(artistItem: MediaBrowserCompat.MediaItem) {
    collapsingToolbar.title = artistItem.description.title
  }

}