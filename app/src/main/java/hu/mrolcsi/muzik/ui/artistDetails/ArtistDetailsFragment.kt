package hu.mrolcsi.muzik.ui.artistDetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.databinding.library.baseAdapters.BR
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.uber.autodispose.android.lifecycle.autoDispose
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.databinding.FragmentArtistDetailsBinding
import hu.mrolcsi.muzik.databinding.ListItemAlbumHorizontalBinding
import hu.mrolcsi.muzik.ui.albums.AlbumItem
import hu.mrolcsi.muzik.ui.common.BoundMVVMViewHolder
import hu.mrolcsi.muzik.ui.common.HideViewOnOffsetChangedListener
import hu.mrolcsi.muzik.ui.common.MVVMListAdapter
import hu.mrolcsi.muzik.ui.common.ThemedViewHolder
import hu.mrolcsi.muzik.ui.common.observeAndRunNavCommands
import hu.mrolcsi.muzik.ui.common.observeAndRunUiCommands
import hu.mrolcsi.muzik.ui.common.toSingle
import hu.mrolcsi.muzik.ui.songs.SongItem
import kotlinx.android.synthetic.main.fragment_artist_details.*
import kotlinx.android.synthetic.main.fragment_artist_details_content.*
import kotlinx.android.synthetic.main.fragment_artist_details_header.*
import kotlinx.android.synthetic.main.list_item_album_content.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ArtistDetailsFragment : Fragment() {

  private val args: ArtistDetailsFragmentArgs by navArgs()

  private val viewModel: ArtistDetailsViewModel by viewModel<ArtistDetailsViewModelImpl>()

  private val albumsAdapter by lazy {
    MVVMListAdapter(
      itemIdSelector = AlbumItem::id,
      viewHolderFactory = { parent, _ ->
        BoundMVVMViewHolder(
          parent = parent,
          layoutId = R.layout.list_item_album_horizontal,
          onItemClickListener = { model, holder ->
            viewModel.onAlbumClick(model, holder.itemView.imgCoverArt)
          },
          onModelChange = { model ->
            // Load album art
            (this as ListItemAlbumHorizontalBinding).incAlbumContent.imgCoverArt.let { imgCoverArt ->
              Picasso.get()
                .load(model.albumArtUri)
                .toSingle()
                .doOnSuccess(imgCoverArt::setImageBitmap)
                .flatMap(viewModel.themeService::createTheme)
                .autoDispose(viewLifecycleOwner)
                .subscribe(
                  {
                    setVariable(BR.theme, it)
                    requireView().post(this::executePendingBindings)
                  },
                  { Timber.e(it) }
                )
            }
          }
        )
      }
    )
  }

  private val songsAdapter by lazy {
    MVVMListAdapter(
      itemIdSelector = SongItem::id,
      viewHolderFactory = { parent, _ ->
        ThemedViewHolder(
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

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    viewModel.setArgument(args.artistId)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
    FragmentArtistDetailsBinding.inflate(inflater, container, false).also {
      it.viewModel = viewModel
      it.theme = viewModel.currentTheme
      it.lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    viewModel.apply {

      requireContext().observeAndRunUiCommands(viewLifecycleOwner, this)
      findNavController().observeAndRunNavCommands(viewLifecycleOwner, this)

      artistAlbums.observe(viewLifecycleOwner, albumsAdapter)
      artistSongs.observe(viewLifecycleOwner, songsAdapter)

      artistPicture.observe(viewLifecycleOwner, { uri ->
        Picasso.get()
          .load(uri)
          .into(imgArtist, object : Callback {
            override fun onSuccess() {
              appBar.setExpanded(true, true)
            }

            override fun onError(e: Exception?) {
              appBar.setExpanded(false)
            }
          })
      })
    }

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

  override fun onDestroyView() {
    super.onDestroyView()

    rvAlbums.adapter = null
    rvSongs.adapter = null

    Picasso.get().cancelRequest(imgArtist)
  }
}