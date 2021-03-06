package hu.mrolcsi.muzik.ui.artistDetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.databinding.library.baseAdapters.BR
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.request.target.Target
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.databinding.FragmentArtistDetailsBinding
import hu.mrolcsi.muzik.ui.albums.AlbumItem
import hu.mrolcsi.muzik.ui.common.BoundMVVMViewHolder
import hu.mrolcsi.muzik.ui.common.HideViewOnOffsetChangedListener
import hu.mrolcsi.muzik.ui.common.MVVMListAdapter
import hu.mrolcsi.muzik.ui.common.ThemedViewHolder
import hu.mrolcsi.muzik.ui.common.glide.GlideApp
import hu.mrolcsi.muzik.ui.common.glide.onLoadFailed
import hu.mrolcsi.muzik.ui.common.glide.onResourceReady
import hu.mrolcsi.muzik.ui.common.observeAndRunNavCommands
import hu.mrolcsi.muzik.ui.common.observeAndRunUiCommands
import hu.mrolcsi.muzik.ui.songs.SongItem
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_artist_details.*
import kotlinx.android.synthetic.main.fragment_artist_details_content.*
import kotlinx.android.synthetic.main.fragment_artist_details_header.*
import kotlinx.android.synthetic.main.list_item_album_content.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class ArtistDetailsFragment : Fragment() {

  private val args: ArtistDetailsFragmentArgs by navArgs()

  private val viewModel: ArtistDetailsViewModel by viewModel<ArtistDetailsViewModelImpl>()

  private val albumsAdapter by lazy {
    MVVMListAdapter(
      itemIdSelector = { it.id },
      viewHolderFactory = { parent, _ ->
        BoundMVVMViewHolder<AlbumItem>(
          parent = parent,
          layoutId = R.layout.list_item_album_horizontal,
          onItemClickListener = { model, holder ->
            viewModel.onAlbumClick(model, holder.itemView.imgCoverArt)
          },
          onModelChange = { model ->
            // Load album art
            val imgCoverArt = this.root.findViewById<ImageView>(R.id.imgCoverArt)
            GlideApp.with(imgCoverArt)
              .asBitmap()
              .load(model.albumArtUri)
              .onResourceReady { albumArt ->
                viewModel.themeService
                  .createTheme(albumArt)
                  .subscribeBy {
                    setVariable(BR.theme, it)
                    executePendingBindings()
                  }
              }
              .into(imgCoverArt)
          }
        )
      }
    )
  }

  private val songsAdapter by lazy {
    MVVMListAdapter(
      itemIdSelector = { it.id },
      viewHolderFactory = { parent, _ ->
        ThemedViewHolder<SongItem>(
          parent = parent,
          layoutId = R.layout.list_item_song_cover_art,
          viewLifecycleOwner = viewLifecycleOwner,
          theme = viewModel.currentTheme
        ) { model, holder ->
          viewModel.onSongClick(model, holder.adapterPosition)
        }
      }
    )
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