package hu.mrolcsi.muzik.library

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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.extensions.applyColorToNavigationBarIcons
import hu.mrolcsi.muzik.extensions.applyColorToStatusBarIcons
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_library.*

class LibraryFragment : Fragment() {

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    inflater.inflate(R.layout.fragment_library, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    libraryToolbar.setTitle(R.string.library_title)

    setupNavBar(requireActivity().findNavController(R.id.libraryNavHost))

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

    (activity as AppCompatActivity?)?.setSupportActionBar(libraryToolbar)

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
    NavigationUI.setupWithNavController(navigationBar, navController)

    navController.addOnDestinationChangedListener { _, destination, arguments ->
      when (destination.id) {
        R.id.navigation_artists, R.id.navigation_albums, R.id.navigation_songs -> {
          // Nothing for now
        }
        else -> {
          // Apply color to back arrow
          val theme = ThemeManager.getInstance(requireContext()).currentTheme.value
          val color = theme?.primaryForegroundColor ?: Color.WHITE

          libraryToolbar.navigationIcon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
          libraryToolbar.overflowIcon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)

          // Show AppBar, hide NavigationBar
          appBar.setExpanded(true, true)
          ViewCompat.animate(navigationBar).translationY(navigationBar.height.toFloat()).start()
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
      navigationBar.setBackgroundColor(color)
    }

    theme.tertiaryBackgroundColor.also { color ->
      // Window background
      activity?.window?.decorView?.setBackgroundColor(color)
    }

    theme.primaryForegroundColor.also { color ->
      // Toolbar Icons
      libraryToolbar.navigationIcon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
      libraryToolbar.overflowIcon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
      libraryToolbar.menu.forEach { it.icon.setTint(color) }

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
      navigationBar.itemIconTintList = navigationTintList
      navigationBar.itemTextColor = navigationTintList
    }

    (navigationBar.itemBackground as RippleDrawable).setTint(theme.primaryForegroundColor)
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
        navigationBar.setBackgroundColor(color)
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

        // Toolbar Icons
        libraryToolbar.navigationIcon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        libraryToolbar.overflowIcon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        libraryToolbar.menu.forEach { item -> item.icon.setTint(color) }

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
        navigationBar.itemIconTintList = navigationTintList
        navigationBar.itemTextColor = navigationTintList
      }
      start()
    }

    (navigationBar.itemBackground as RippleDrawable).setTint(theme.primaryForegroundColor)
  }

  companion object {
    private const val LOG_TAG = "LibraryFragment"
  }
}