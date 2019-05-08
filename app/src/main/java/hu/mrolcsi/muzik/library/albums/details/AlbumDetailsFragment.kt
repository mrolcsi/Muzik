package hu.mrolcsi.muzik.library.albums.details

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
import hu.mrolcsi.muzik.extensions.observeOnce
import hu.mrolcsi.muzik.service.extensions.media.MediaType
import hu.mrolcsi.muzik.service.extensions.media.addQueueItems
import hu.mrolcsi.muzik.service.extensions.media.album
import hu.mrolcsi.muzik.service.extensions.media.albumArtUri
import hu.mrolcsi.muzik.service.extensions.media.albumYear
import hu.mrolcsi.muzik.service.extensions.media.artist
import hu.mrolcsi.muzik.service.extensions.media.clearQueue
import hu.mrolcsi.muzik.service.extensions.media.numberOfSongs
import hu.mrolcsi.muzik.service.extensions.media.playFromMediaItems
import hu.mrolcsi.muzik.service.extensions.media.type
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

      if (item.description.type == MediaType.MEDIA_OTHER) {
        // Shuffle All
        mModel.songDescriptions.observeOnce(viewLifecycleOwner, Observer { descriptions ->
          controller.clearQueue()
          controller.transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
          controller.addQueueItems(descriptions)
          controller.transportControls.play()
        })
      } else {
        mModel.songsFromAlbum.value?.let { items ->
          controller.transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
          controller.playFromMediaItems(items, position)
        }
      }
    }).apply {
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

  override fun onStart() {
    super.onStart()
    mModel.connect()
  }

  override fun onStop() {
    super.onStop()
    mModel.disconnect()
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

    mSongsAdapter.theme = theme
  }

  companion object {
    private const val LOG_TAG = "AlbumDetailsFragment"
  }
}