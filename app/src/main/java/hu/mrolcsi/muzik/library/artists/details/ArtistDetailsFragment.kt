package hu.mrolcsi.muzik.library.artists.details

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.Observer
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.android.support.DaggerFragment
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.ColoredDividerItemDecoration
import hu.mrolcsi.muzik.common.MediaItemListAdapter
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.glide.MuzikGlideModule
import hu.mrolcsi.muzik.extensions.observeOnce
import hu.mrolcsi.muzik.library.albums.AlbumHolder
import hu.mrolcsi.muzik.library.songs.SongHolder
import hu.mrolcsi.muzik.service.extensions.media.MediaType
import hu.mrolcsi.muzik.service.extensions.media.addQueueItems
import hu.mrolcsi.muzik.service.extensions.media.artist
import hu.mrolcsi.muzik.service.extensions.media.clearQueue
import hu.mrolcsi.muzik.service.extensions.media.playFromMediaItems
import hu.mrolcsi.muzik.service.extensions.media.setQueueTitle
import hu.mrolcsi.muzik.service.extensions.media.type
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.artist_details_header.*
import kotlinx.android.synthetic.main.fragment_artist_details.*
import kotlinx.android.synthetic.main.fragment_artist_details_content.*
import kotlinx.android.synthetic.main.list_item_album_content.view.*
import javax.inject.Inject

class ArtistDetailsFragment : DaggerFragment() {

  private val args: ArtistDetailsFragmentArgs by navArgs()

  @Inject lateinit var viewModel: ArtistDetailsViewModel

  private val albumsAdapter by lazy {
    MediaItemListAdapter(requireContext()) { parent, _ ->
      AlbumHolder(
        LayoutInflater
          .from(parent.context)
          .inflate(R.layout.list_item_album_horizontal, parent, false)
      ).apply {
        itemView.setOnClickListener { view ->
          model?.let {
            if (it.description.type == MediaType.MEDIA_ALBUM) {
              findNavController().navigate(
                R.id.navigation_albumDetails,
                ArtistDetailsFragmentArgs(it).toBundle(),
                null,
                FragmentNavigatorExtras(view.imgCoverArt to "coverArt")
              )
            }
          }
        }
      }
    }
  }

  private val mSongsAdapter by lazy {
    MediaItemListAdapter(requireContext()) { parent, _ ->
      SongHolder(
        LayoutInflater
          .from(parent.context)
          .inflate(R.layout.list_item_song, parent, false),
        false
      ).apply {
        itemView.setOnClickListener {
          model?.let {
            val controller = MediaControllerCompat.getMediaController(requireActivity())

            viewModel.artistItem?.description?.artist?.let { controller.setQueueTitle(it) }

            if (model?.description?.type == MediaType.MEDIA_OTHER) {
              // Shuffle All
              viewModel.songDescriptions.observeOnce(viewLifecycleOwner, Observer { descriptions ->
                controller.clearQueue()
                controller.transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
                controller.addQueueItems(descriptions)
                controller.transportControls.play()
              })
            } else {
              viewModel.artistSongs.value?.let { items ->
                controller.transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
                controller.playFromMediaItems(items, adapterPosition)
              }
            }
          }
        }
      }
    }
  }

  private val divider by lazy {
    ColoredDividerItemDecoration(requireContext(), LinearLayout.VERTICAL).apply {
      setDrawable(resources.getDrawable(R.drawable.list_divider_inset, requireContext().theme))
    }
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    viewModel.apply {

      artistItem = args.artistItem

      artistItem?.let { loadHeader(it) }

      artistAlbums.observe(viewLifecycleOwner, Observer {
        // Hide Albums section when list is empty
        albumsGroup?.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE

        albumsAdapter.submitList(it)
      })
      artistSongs.observe(viewLifecycleOwner, Observer(mSongsAdapter::submitList))

      artistPicture.observe(viewLifecycleOwner, Observer { uri ->
        GlideApp.with(this@ArtistDetailsFragment)
          .asBitmap()
          .load(uri)
          .addListener(object : MuzikGlideModule.SimpleRequestListener<Bitmap> {
            override fun onResourceReady(resource: Bitmap?) {
              appBar.setExpanded(true, true)
            }
          })
          .into(imgArtist)
      })
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_artist_details, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

    rvAlbums.apply {
      adapter = albumsAdapter
      isNestedScrollingEnabled = false
    }

    rvSongs.apply {
      adapter = mSongsAdapter
      isNestedScrollingEnabled = true
      addItemDecoration(divider)
    }


    ThemeManager.getInstance(requireContext()).currentTheme.observe(viewLifecycleOwner, object : Observer<Theme> {
      private var initialLoad = true

      override fun onChanged(it: Theme) {
        mSongsAdapter.notifyDataSetChanged()

        if (initialLoad) {
          applyThemeStatic(it)
          initialLoad = false
        } else {
          applyThemeAnimated(it)
        }
      }
    })
  }

  override fun onStart() {
    super.onStart()
    viewModel.connect()
  }

  override fun onStop() {
    super.onStop()
    viewModel.disconnect()
  }

  private fun loadHeader(artistItem: MediaBrowserCompat.MediaItem) {
    collapsingToolbar.title = artistItem.description.title
  }

  private fun applyPrimaryBackgroundColor(color: Int) {
    appBar.setBackgroundColor(color)
    collapsingToolbar.setContentScrimColor(color)
  }

  private fun applyPrimaryForegroundColor(color: Int) {
    collapsingToolbar.setCollapsedTitleTextColor(color)
  }

  private fun applySecondaryBackgroundColor(color: Int) {
    imgProtectionScrim.setImageDrawable(
      GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(Color.TRANSPARENT, color)
      )
    )
  }

  private fun applySecondaryForegroundColor(color: Int) {
    collapsingToolbar.setExpandedTitleColor(color)

    divider.setTint(color)

    imgAlbums.drawable.setTint(color)
    lblAlbums.setTextColor(color)
    dividerAlbums.background.setTint(color)

    imgSongs.drawable.setTint(color)
    lblSongs.setTextColor(color)
    dividerSongs.background.setTint(color)
  }

  private fun applyThemeStatic(theme: Theme) {
    applyPrimaryBackgroundColor(theme.primaryBackgroundColor)
    applyPrimaryForegroundColor(theme.primaryForegroundColor)
    applySecondaryBackgroundColor(theme.secondaryBackgroundColor)
    applySecondaryForegroundColor(theme.secondaryForegroundColor)
  }

  private fun applyThemeAnimated(theme: Theme) {

    val currentTheme = ThemeManager.getInstance(requireContext()).currentTheme.value

    val animationDuration = context?.resources?.getInteger(R.integer.preferredAnimationDuration)?.toLong() ?: 300L

    ValueAnimator.ofArgb(
      currentTheme?.primaryBackgroundColor ?: Color.BLACK,
      theme.primaryBackgroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int

        applyPrimaryBackgroundColor(color)
      }
      start()
    }

    ValueAnimator.ofArgb(
      currentTheme?.primaryForegroundColor ?: Color.WHITE,
      theme.primaryForegroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int

        applyPrimaryForegroundColor(color)
      }
      start()
    }

    ValueAnimator.ofArgb(
      currentTheme?.secondaryBackgroundColor ?: Color.BLACK,
      theme.secondaryBackgroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int

        applySecondaryBackgroundColor(color)
      }
      start()
    }

    ValueAnimator.ofArgb(
      currentTheme?.secondaryForegroundColor ?: Color.WHITE,
      theme.secondaryForegroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int

        applySecondaryForegroundColor(color)
      }
      start()
    }
  }
}