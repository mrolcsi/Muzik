package hu.mrolcsi.muzik.library.albums.details

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionInflater
import dagger.android.support.DaggerFragment
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.ColoredDividerItemDecoration
import hu.mrolcsi.muzik.common.MediaItemListAdapter
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.glide.onLoadFailed
import hu.mrolcsi.muzik.common.glide.onResourceReady
import hu.mrolcsi.muzik.library.songs.SongHolder
import hu.mrolcsi.muzik.service.extensions.media.album
import hu.mrolcsi.muzik.service.extensions.media.albumArtUri
import hu.mrolcsi.muzik.service.extensions.media.albumYear
import hu.mrolcsi.muzik.service.extensions.media.artist
import hu.mrolcsi.muzik.service.extensions.media.numberOfSongs
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.album_details_header.*
import kotlinx.android.synthetic.main.fragment_album_details.*
import javax.inject.Inject

class AlbumDetailsFragment : DaggerFragment() {

  private val args by navArgs<AlbumDetailsFragmentArgs>()

  @Inject lateinit var viewModel: AlbumDetailsViewModel

  private val songsAdapter by lazy {
    MediaItemListAdapter(requireContext()) { parent, _ ->
      SongHolder(
        LayoutInflater
          .from(parent.context)
          .inflate(R.layout.list_item_song, parent, false),
        true
      ).apply {
        itemView.setOnClickListener {
          model?.let {
            viewModel.onSongClick(it, adapterPosition)
          }
        }
      }
    }
  }

  private val mDivider by lazy {
    ColoredDividerItemDecoration(requireContext(), LinearLayout.VERTICAL)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    postponeEnterTransition()

    val animationDuration = context?.resources?.getInteger(R.integer.preferredAnimationDuration)?.toLong() ?: 300L

    enterTransition = TransitionInflater
      .from(requireContext())
      .inflateTransition(android.R.transition.move)
      .setDuration(animationDuration)
      .excludeTarget(imgCoverArt, true)

    returnTransition = TransitionInflater
      .from(requireContext())
      .inflateTransition(android.R.transition.move)
      .setDuration(animationDuration)
      .excludeTarget(imgCoverArt, true)

    sharedElementEnterTransition = TransitionInflater
      .from(requireContext())
      .inflateTransition(android.R.transition.move)
      .setDuration(animationDuration)

    sharedElementReturnTransition = TransitionInflater
      .from(requireContext())
      .inflateTransition(android.R.transition.move)
      .setDuration(animationDuration)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    viewModel.apply {

      albumItem = args.albumItem

      items.observe(viewLifecycleOwner, Observer {
        Log.d(LOG_TAG, "Got items from LiveData: $it")
        songsAdapter.submitList(it)
      })

      albumDetails.observe(viewLifecycleOwner, Observer {
        loadHeader(it)
      })
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    inflater.inflate(R.layout.fragment_album_details, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvSongs.apply {
      adapter = songsAdapter
      addItemDecoration(mDivider)
    }
  }

  private fun loadHeader(albumItem: MediaBrowserCompat.MediaItem) {

    albumDetailsToolbar.title = albumItem.description.album

    tvArtist.text = albumItem.description.artist

    if (albumItem.description.albumYear == null) {
      tvYear.visibility = View.GONE
    } else {
      tvYear.visibility = View.VISIBLE
      tvYear.text = albumItem.description.albumYear
    }

    tvYear.text = albumItem.description.albumYear

    val numberOfSong = albumItem.description.numberOfSongs
    tvNumSongs.text = resources.getQuantityString(R.plurals.artists_numberOfSongs, numberOfSong, numberOfSong)

    GlideApp.with(imgCoverArt)
      .asBitmap()
      .load(albumItem.description.albumArtUri)
      .onResourceReady { startPostponedEnterTransition() }
      .onLoadFailed { startPostponedEnterTransition(); true }
      .into(imgCoverArt)
  }

  private fun applyTheme(theme: Theme) {

    val currentTheme = ThemeManager.getInstance(requireContext()).currentTheme.value

    // Primary: Toolbar
    // Tertiary: RecyclerView

    val animationDuration = context?.resources?.getInteger(R.integer.preferredAnimationDuration)?.toLong() ?: 300L

    ValueAnimator.ofArgb(
      currentTheme?.primaryBackgroundColor ?: Color.BLACK,
      theme.primaryBackgroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int

        appBar.setBackgroundColor(color)
        collapsingToolbar.setContentScrimColor(color)
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

        collapsingToolbar.setExpandedTitleColor(color)
        collapsingToolbar.setCollapsedTitleTextColor(color)

        tvArtist.setTextColor(color)
        tvYear.setTextColor(color)
        tvNumSongs.setTextColor(color)
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

        // Window background
        view?.setBackgroundColor(color)
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

        mDivider.setTint(color)
      }
      start()
    }
  }

  companion object {
    private const val LOG_TAG = "AlbumDetailsFragment"
  }
}