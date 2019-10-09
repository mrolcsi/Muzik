package hu.mrolcsi.muzik.library.artists.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.DaggerFragment
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.MediaItemListAdapter
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.glide.onLoadFailed
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
        itemView.setOnClickListener { view ->
          model?.let {
            viewModel.onAlbumClick(it, view.findViewById(R.id.imgCoverArt))
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

      setArgument(args.artistId)

      artistAlbums.observe(viewLifecycleOwner, albumsAdapter)
      artistSongs.observe(viewLifecycleOwner, songsAdapter)

      artistPicture.observe(viewLifecycleOwner, Observer { uri ->
        GlideApp.with(this@ArtistDetailsFragment)
          .asBitmap()
          .load(uri)
          .onLoadFailed { appBar.setExpanded(false); false }
          .onResourceReady { appBar.setExpanded(true, true) }
          .into(imgArtist)
      })
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
    FragmentArtistDetailsBinding.inflate(inflater, container, false).also {
      it.viewModel = viewModel
      it.theme = viewModel.currentTheme
      it.lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvAlbums.adapter = albumsAdapter
    rvSongs.adapter = songsAdapter
    collapsingToolbar.setupWithNavController(artistDetailsToolbar, findNavController())
  }

}