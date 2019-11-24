package hu.mrolcsi.muzik.ui.artistDetails

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
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.target.Target
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.data.service.theme.ThemeService
import hu.mrolcsi.muzik.databinding.FragmentArtistDetailsBinding
import hu.mrolcsi.muzik.databinding.ListItemSongBinding
import hu.mrolcsi.muzik.ui.albums.AlbumHolder
import hu.mrolcsi.muzik.ui.common.HideViewOnOffsetChangedListener
import hu.mrolcsi.muzik.ui.common.MediaItemListAdapter
import hu.mrolcsi.muzik.ui.common.glide.GlideApp
import hu.mrolcsi.muzik.ui.common.glide.onLoadFailed
import hu.mrolcsi.muzik.ui.common.glide.onResourceReady
import hu.mrolcsi.muzik.ui.common.observeAndRunNavCommands
import hu.mrolcsi.muzik.ui.common.observeAndRunUiCommands
import hu.mrolcsi.muzik.ui.songs.SongHolder
import kotlinx.android.synthetic.main.fragment_artist_details.*
import kotlinx.android.synthetic.main.fragment_artist_details_content.*
import kotlinx.android.synthetic.main.fragment_artist_details_header.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ArtistDetailsFragment : Fragment() {

  private val args: ArtistDetailsFragmentArgs by navArgs()

  private val viewModel: ArtistDetailsViewModel by viewModel<ArtistDetailsViewModelImpl>()
  private val themeService: ThemeService by inject()

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
          .override(Target.SIZE_ORIGINAL)
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

    // Fix height of AppBarLayout with fitsSystemWindows="true"
    ViewCompat.setOnApplyWindowInsetsListener(appBar) { _, insets ->
      (artistDetailsToolbar.layoutParams as ViewGroup.MarginLayoutParams).topMargin = insets.systemWindowInsetTop
      insets.consumeSystemWindowInsets()
    }

    appBar.addOnOffsetChangedListener(HideViewOnOffsetChangedListener(tvArtistName))
  }

}