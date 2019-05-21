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
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.ColoredDividerItemDecoration
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.glide.MuzikGlideModule
import hu.mrolcsi.muzik.extensions.OnItemClickListener
import hu.mrolcsi.muzik.extensions.observeOnce
import hu.mrolcsi.muzik.library.albums.AlbumsAdapter
import hu.mrolcsi.muzik.library.songs.SongsAdapter
import hu.mrolcsi.muzik.service.extensions.media.MediaType
import hu.mrolcsi.muzik.service.extensions.media.addQueueItems
import hu.mrolcsi.muzik.service.extensions.media.clearQueue
import hu.mrolcsi.muzik.service.extensions.media.playFromMediaItems
import hu.mrolcsi.muzik.service.extensions.media.type
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.artist_details_header.*
import kotlinx.android.synthetic.main.fragment_artist_details.*
import kotlinx.android.synthetic.main.fragment_artist_details_content.*

class ArtistDetailsFragment : Fragment() {

  private val args: ArtistDetailsFragmentArgs by navArgs()

  private lateinit var mModel: ArtistDetailsViewModel

  private val mAlbumsAdapter by lazy { AlbumsAdapter(requireContext(), RecyclerView.HORIZONTAL) }

  private val mSongsAdapter by lazy {
    SongsAdapter(requireContext(), OnItemClickListener { item, _, position, _ ->
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
        mModel.artistSongs.value?.let { items ->
          controller.transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
          controller.playFromMediaItems(items, position)
        }
      }
    })
  }

  private val mDivider by lazy {
    ColoredDividerItemDecoration(requireContext(), LinearLayout.VERTICAL).apply {
      setDrawable(resources.getDrawable(R.drawable.list_divider_inset, requireContext().theme))
    }
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    activity?.let { activity ->
      mModel = ViewModelProviders.of(this, ArtistDetailsViewModel.Factory(activity.application, args.artistItem))
        .get(ArtistDetailsViewModel::class.java)
        .apply {

          loadHeader(artistItem)

          artistAlbums.observe(viewLifecycleOwner, Observer {
            // Hide Albums section when list is empty
            albumsGroup?.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE

            mAlbumsAdapter.submitList(it)
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
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_artist_details, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

    rvAlbums.apply {
      adapter = mAlbumsAdapter
      isNestedScrollingEnabled = false
    }

    rvSongs.apply {
      adapter = mSongsAdapter
      isNestedScrollingEnabled = true
      addItemDecoration(mDivider)
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
    mModel.connect()
  }

  override fun onStop() {
    super.onStop()
    mModel.disconnect()
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

    mDivider.setTint(color)

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