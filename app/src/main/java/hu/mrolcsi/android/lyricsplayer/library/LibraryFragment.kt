package hu.mrolcsi.android.lyricsplayer.library

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import hu.mrolcsi.android.lyricsplayer.R
import hu.mrolcsi.android.lyricsplayer.extensions.applyColorToNavigationBarIcons
import hu.mrolcsi.android.lyricsplayer.extensions.applyColorToStatusBarIcons
import hu.mrolcsi.android.lyricsplayer.library.albums.AlbumsFragmentArgs
import hu.mrolcsi.android.lyricsplayer.library.songs.SongsFragmentArgs
import hu.mrolcsi.android.lyricsplayer.theme.Theme
import hu.mrolcsi.android.lyricsplayer.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_library.*

class LibraryFragment : Fragment() {

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    inflater.inflate(R.layout.fragment_library, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    libraryToolbar.setTitle(R.string.library_title)

    setupNavBar(requireActivity().findNavController(R.id.library_nav_host))

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

  override fun onResume() {
    super.onResume()

    // Apply StatusBar and NavigationBar colors again
    val themeManager = ThemeManager.getInstance(requireContext())
    activity?.applyColorToStatusBarIcons(
      themeManager.currentTheme.value?.primaryBackgroundColor
        ?: Color.BLACK
    )
    activity?.applyColorToNavigationBarIcons(
      themeManager.currentTheme.value?.primaryBackgroundColor
        ?: Color.BLACK
    )
  }

  private fun setupNavBar(navController: NavController) {
    val appBarConfig = AppBarConfiguration.Builder(
      R.id.navigation_artists,
      R.id.navigation_albums,
      R.id.navigation_songs
    ).build()

    NavigationUI.setupWithNavController(libraryToolbar, navController, appBarConfig)
    NavigationUI.setupWithNavController(navigation_bar, navController)

    navController.addOnDestinationChangedListener { _, destination, arguments ->
      when (destination.id) {
        R.id.navigation_artists -> {
          libraryToolbar.subtitle = null
        }
        R.id.navigation_albums -> {
          libraryToolbar.subtitle = null
        }
        R.id.navigation_albumsByArtist -> {
          if (arguments != null) {
            val args = AlbumsFragmentArgs.fromBundle(arguments)
            libraryToolbar.subtitle = getString(R.string.albums_byArtist_subtitle, args.artistName)
          }
        }
        R.id.navigation_songs -> {
          libraryToolbar.subtitle = null
        }
        R.id.navigation_songsFromAlbum -> {
          if (arguments != null) {
            val args = SongsFragmentArgs.fromBundle(arguments)
            if (args.albumKey != null) {
              libraryToolbar.subtitle = getString(R.string.songs_fromAlbum_subtitle, args.albumTitle)
            } else if (args.artistKey != null) {
              libraryToolbar.subtitle = getString(R.string.albums_byArtist_subtitle, args.artistName)
            }
          }
        }
      }
    }
  }

  private fun applyThemeStatic(theme: Theme) {
    Log.d(LOG_TAG, "Applying theme (static)...")

    theme.primaryBackgroundColor.also { color ->
      // Status Bar Icons
      activity?.applyColorToStatusBarIcons(color)
      // Navigation Bar Icons
      activity?.applyColorToNavigationBarIcons(color)

      // Status Bar
      activity?.window?.statusBarColor = color
      // Navigation Bar
      activity?.window?.navigationBarColor = color
      // Toolbar Background
      libraryToolbar.setBackgroundColor(color)
    }

    theme.secondaryBackgroundColor.also { color ->
      // BottomNavigation Background
      navigation_bar.setBackgroundColor(color)
    }

    theme.tertiaryBackgroundColor.also { color ->
      // Window background
      activity?.window?.decorView?.setBackgroundColor(color)
    }

    theme.primaryForegroundColor.also { color ->
      // Toolbar Icon
      libraryToolbar.navigationIcon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
      // Title and Subtitle
      libraryToolbar.setTitleTextColor(color)
      libraryToolbar.setSubtitleTextColor(color)
    }

    theme.secondaryForegroundColor.also { color ->
      // BottomNavigation Selected Colors
      val navigationTintList = ColorStateList(
        arrayOf(
          intArrayOf(android.R.attr.state_checked),
          intArrayOf()
        ),
        intArrayOf(
          color,
          ColorUtils.setAlphaComponent(color, Theme.DISABLED_OPACITY)
        )
      )
      navigation_bar.itemIconTintList = navigationTintList
      navigation_bar.itemTextColor = navigationTintList
    }

    (navigation_bar.itemBackground as RippleDrawable).setTint(theme.primaryForegroundColor)
  }

  private fun applyThemeAnimated(theme: Theme) {
    Log.d(LOG_TAG, "Applying theme (animated)...")

    val previousTheme = ThemeManager.getInstance(requireContext()).previousTheme
    val animationDuration = context?.resources?.getInteger(R.integer.preferredAnimationDuration)?.toLong() ?: 300L

    ValueAnimator.ofArgb(
      previousTheme?.primaryBackgroundColor ?: Color.BLACK,
      theme.primaryBackgroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int

        // Status Bar Icons
        activity?.applyColorToStatusBarIcons(color)
        // Navigation Bar Icons
        activity?.applyColorToNavigationBarIcons(color)

        // Status Bar
        activity?.window?.statusBarColor = color
        // Navigation Bar
        activity?.window?.navigationBarColor = color
        // Toolbar Background
        libraryToolbar.setBackgroundColor(color)
      }
      start()
    }

    ValueAnimator.ofArgb(
      previousTheme?.secondaryBackgroundColor ?: Color.BLACK,
      theme.secondaryBackgroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int

        // BottomNavigation Background
        navigation_bar.setBackgroundColor(color)
      }
      start()
    }

    ValueAnimator.ofArgb(
      previousTheme?.tertiaryBackgroundColor ?: Color.BLACK,
      theme.tertiaryBackgroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int

        // Window background
        activity?.window?.decorView?.setBackgroundColor(color)
      }
      start()
    }

    ValueAnimator.ofArgb(
      previousTheme?.primaryForegroundColor ?: Color.WHITE,
      theme.primaryForegroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int

        // Toolbar Icon
        libraryToolbar.navigationIcon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        // Title and Subtitle
        libraryToolbar.setTitleTextColor(color)
        libraryToolbar.setSubtitleTextColor(color)
      }
      start()
    }

    ValueAnimator.ofArgb(
      previousTheme?.secondaryForegroundColor ?: Color.WHITE,
      theme.secondaryForegroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int

        // BottomNavigation Selected Colors
        val navigationTintList = ColorStateList(
          arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
          ),
          intArrayOf(
            color,
            ColorUtils.setAlphaComponent(color, Theme.DISABLED_OPACITY)
          )
        )
        navigation_bar.itemIconTintList = navigationTintList
        navigation_bar.itemTextColor = navigationTintList
      }
      start()
    }

    (navigation_bar.itemBackground as RippleDrawable).setTint(theme.primaryForegroundColor)
  }

  companion object {
    private const val LOG_TAG = "LibraryFragment"
  }
}