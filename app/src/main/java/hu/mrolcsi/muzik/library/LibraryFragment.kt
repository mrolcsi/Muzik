package hu.mrolcsi.muzik.library

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.extensions.applyForegroundColor
import hu.mrolcsi.muzik.extensions.applyNavigationBarColor
import hu.mrolcsi.muzik.extensions.applyStatusBarColor
import hu.mrolcsi.muzik.service.theme.Theme
import hu.mrolcsi.muzik.service.theme.ThemeManager
import kotlinx.android.synthetic.main.fragment_library.*


class LibraryFragment : Fragment() {

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    inflater.inflate(R.layout.fragment_library, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    libraryToolbar.setTitle(R.string.library_title)

    setupNavigation(requireActivity().findNavController(R.id.libraryNavHost))

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
    activity?.applyStatusBarColor(
      themeManager.currentTheme.value?.primaryBackgroundColor
        ?: Color.BLACK
    )
    activity?.applyNavigationBarColor(
      themeManager.currentTheme.value?.primaryBackgroundColor
        ?: Color.BLACK
    )
  }

  private fun setupNavigation(navController: NavController) {
    val topLevelDestinations = setOf(
      R.id.navigation_pager,
      R.id.navigation_artists,
      R.id.navigation_albums,
      R.id.navigation_songs
    )
    val appBarConfig = AppBarConfiguration.Builder(topLevelDestinations).build()
    NavigationUI.setupWithNavController(libraryToolbar, navController, appBarConfig)

    navController.addOnDestinationChangedListener { _, destination, _ ->
      when {
        destination.id in topLevelDestinations -> {
          // Nothing for now
        }
        else -> {
          // Apply color to back arrow
          val theme = ThemeManager.getInstance(requireContext()).currentTheme.value
          val color = theme?.primaryForegroundColor ?: Color.WHITE

          libraryToolbar.applyForegroundColor(color)

          // Show AppBar, hide NavigationBar
          appBar.setExpanded(true, true)
        }
      }
    }
  }

  private fun applyThemeStatic(theme: Theme) {
    Log.d(LOG_TAG, "Applying theme (static)...")

    theme.primaryBackgroundColor.also { color ->
      // Status Bar
      activity?.applyStatusBarColor(color)

      // Navigation
      activity?.applyNavigationBarColor(color)

      // Toolbar Background
      libraryToolbar.setBackgroundColor(color)
    }

    theme.tertiaryBackgroundColor.also { color ->
      // Window background
      activity?.window?.decorView?.setBackgroundColor(color)
    }

    theme.primaryForegroundColor.also { color ->
      // Toolbar
      libraryToolbar.applyForegroundColor(color)
    }

  }

  private fun applyThemeAnimated(theme: Theme) {
    Log.d(LOG_TAG, "Applying theme (animated)...")

    val previousTheme = ThemeManager.getInstance(requireContext()).previousTheme
    val animationDuration =
      context?.resources?.getInteger(R.integer.preferredAnimationDuration)?.toLong() ?: 300L

    ValueAnimator.ofArgb(
      previousTheme?.primaryBackgroundColor ?: Color.BLACK,
      theme.primaryBackgroundColor
    ).run {
      duration = animationDuration
      addUpdateListener {
        val color = it.animatedValue as Int

        // Status Bar
        activity?.applyStatusBarColor(color)

        // Navigation Bar
        activity?.applyNavigationBarColor(color)

        // Toolbar Background
        libraryToolbar.setBackgroundColor(color)
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

        libraryToolbar.applyForegroundColor(color)
      }
      start()
    }
  }

  companion object {
    private const val LOG_TAG = "LibraryFragment"
  }
}