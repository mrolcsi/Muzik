package hu.mrolcsi.muzik.library.artists.details

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.glide.MuzikGlideModule
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.artist_details_header.*
import kotlinx.android.synthetic.main.fragment_artist_details.*

class ArtistDetailsFragment : Fragment() {

  private val args: ArtistDetailsFragmentArgs by navArgs()

  private lateinit var mModel: ArtistDetailsViewModel

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    activity?.let { activity ->
      mModel = ViewModelProviders.of(this, ArtistDetailsViewModel.Factory(activity.application, args.artistItem))
        .get(ArtistDetailsViewModel::class.java)
        .apply {

          loadHeader(artistItem)

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
    // TODO: Albums and Songs

    ThemeManager.getInstance(requireContext()).currentTheme.observe(viewLifecycleOwner, object : Observer<Theme> {
      private var initialLoad = true

      override fun onChanged(it: Theme) {
        if (initialLoad) {
          applyThemeStatic(it)
          initialLoad = false
        } else {
          applyThemeAnimated(it)
        }
      }
    })
  }

  private fun loadHeader(artistItem: MediaBrowserCompat.MediaItem) {
    collapsingToolbar.title = artistItem.description.title
  }

  private fun applyThemeStatic(theme: Theme) {
    appBar.setBackgroundColor(theme.primaryBackgroundColor)
    collapsingToolbar.setContentScrimColor(theme.primaryBackgroundColor)

    imgProtectionScrim.setImageDrawable(
      GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(Color.TRANSPARENT, theme.secondaryBackgroundColor)
      )
    )

    collapsingToolbar.setCollapsedTitleTextColor(theme.primaryForegroundColor)
    collapsingToolbar.setExpandedTitleColor(theme.secondaryForegroundColor)

  }

  private fun applyThemeAnimated(theme: Theme) {
    // TODO: proper animation
    applyThemeStatic(theme)
  }
}