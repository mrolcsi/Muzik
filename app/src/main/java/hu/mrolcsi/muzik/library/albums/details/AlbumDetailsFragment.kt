package hu.mrolcsi.muzik.library.albums.details

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionInflater
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.ColoredDividerItemDecoration
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.glide.MuzikGlideModule
import hu.mrolcsi.muzik.extensions.OnItemClickListener
import hu.mrolcsi.muzik.service.exoplayer.ExoPlayerHolder
import hu.mrolcsi.muzik.service.extensions.media.addQueueItems
import hu.mrolcsi.muzik.service.extensions.media.album
import hu.mrolcsi.muzik.service.extensions.media.albumArtUri
import hu.mrolcsi.muzik.service.extensions.media.albumYear
import hu.mrolcsi.muzik.service.extensions.media.artist
import hu.mrolcsi.muzik.service.extensions.media.numberOfSongs
import hu.mrolcsi.muzik.service.extensions.media.playFromDescription
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.album_details_header.*
import kotlinx.android.synthetic.main.fragment_album_details.*

class AlbumDetailsFragment : Fragment() {

  private val args: AlbumDetailsFragmentArgs by navArgs()

  private lateinit var mModel: AlbumDetailsViewModel

  private val mSongsAdapter by lazy {
    AlbumSongsAdapter(requireContext(), OnItemClickListener { item, holder, position, id ->
      Log.d(LOG_TAG, "onItemClicked($item, $holder, $position, $id)")

      val controller = MediaControllerCompat.getMediaController(requireActivity())

      // Immediately start the song that was clicked on
      controller.transportControls.playFromDescription(
        item.description,
        bundleOf(ExoPlayerHolder.EXTRA_DESIRED_QUEUE_POSITION to position)
      )

      AsyncTask.execute {
        mModel.songsFromAlbum.value?.let { items ->
          Log.d(LOG_TAG, "onItemClicked() Collecting descriptions...")

          // Add songs to queue
          val descriptions = items.filterIndexed { index, _ ->
            index != position
          }.map {
            it.description
          }

          Log.d(LOG_TAG, "onItemClicked() Sending items to queue...")

          controller.addQueueItems(descriptions)
        }
      }
    }
    ).apply {
      showTrackNumber = true
    }
  }

  private val mDivider by lazy {
    ColoredDividerItemDecoration(requireContext(), LinearLayout.VERTICAL)
  }

  private val onCoverArtReady = object : MuzikGlideModule.SimpleRequestListener<Bitmap> {
    override fun onLoadFailed() {
      startPostponedEnterTransition()
    }

    override fun onResourceReady(resource: Bitmap?) {
      startPostponedEnterTransition()

      resource?.let { bitmap ->
        AsyncTask.execute {
          // Generate theme from resource
          val theme = ThemeManager.getInstance(requireContext()).createFromBitmap(bitmap)
          view?.post {
            applyTheme(theme)
          }
        }
      }
    }
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

    activity?.let { activity ->
      mModel = ViewModelProviders.of(this, AlbumDetailsViewModel.Factory(activity.application, args.albumItem))
        .get(AlbumDetailsViewModel::class.java)
        .apply {

          loadHeader(albumItem)

          songsFromAlbum.observe(viewLifecycleOwner, Observer {
            Log.d(LOG_TAG, "Got items from LiveData: $it")
            mSongsAdapter.submitList(it)
          })
        }
    }

  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    inflater.inflate(R.layout.fragment_album_details, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    rvSongs.apply {
      adapter = mSongsAdapter
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
      .addListener(onCoverArtReady)
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
      currentTheme?.tertiaryBackgroundColor ?: Color.BLACK,
      theme.tertiaryBackgroundColor
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
      currentTheme?.tertiaryForegroundColor ?: Color.WHITE,
      theme.tertiaryForegroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int

        mDivider.setTint(color)
      }
      start()
    }

    mSongsAdapter.theme = theme
  }

  companion object {
    private const val LOG_TAG = "AlbumDetailsFragment"
  }
}