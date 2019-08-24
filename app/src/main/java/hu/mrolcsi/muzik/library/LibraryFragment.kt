package hu.mrolcsi.muzik.library

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import dagger.android.support.DaggerFragment
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.extensions.applyForegroundColor
import hu.mrolcsi.muzik.extensions.applyNavigationBarColor
import hu.mrolcsi.muzik.extensions.applyStatusBarColor
import hu.mrolcsi.muzik.theme.Theme
import kotlinx.android.synthetic.main.fragment_library.*
import javax.inject.Inject

class LibraryFragment : DaggerFragment() {

  @Inject lateinit var viewModel: LibraryViewModel

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
    inflater.inflate(R.layout.fragment_library, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    libraryToolbar.setTitle(R.string.library_title)

    setupNavigation(requireActivity().findNavController(R.id.libraryNavHost))

    viewModel.currentTheme.observe(viewLifecycleOwner, object : Observer<Theme> {

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
    activity?.applyStatusBarColor(
      viewModel.currentTheme.value?.primaryBackgroundColor ?: Color.BLACK
    )
    activity?.applyNavigationBarColor(
      viewModel.currentTheme.value?.primaryBackgroundColor ?: Color.BLACK
    )
  }

  private fun setupNavigation(navController: NavController) {
    val topLevelDestinations = setOf(
      R.id.navLibraryPager,
      R.id.navigation_artists,
      R.id.navigation_albums,
      R.id.navigation_songs
    )
    val appBarConfig = AppBarConfiguration.Builder(topLevelDestinations).build()
    (activity as? AppCompatActivity)?.setSupportActionBar(libraryToolbar)
    libraryToolbar.setupWithNavController(navController, appBarConfig)

    navController.addOnDestinationChangedListener { _, destination, _ ->
      when {
        destination.id in topLevelDestinations -> {
          // Nothing for now
        }
        else -> {
          // Apply color to back arrow
          val color = viewModel.currentTheme.value?.primaryForegroundColor ?: Color.WHITE

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

    theme.secondaryBackgroundColor.also { color ->
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

    val previousTheme = viewModel.previousTheme
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
      previousTheme?.secondaryBackgroundColor ?: Color.BLACK,
      theme.secondaryBackgroundColor
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