package hu.mrolcsi.muzik.library

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
import hu.mrolcsi.muzik.databinding.FragmentLibraryBinding
import hu.mrolcsi.muzik.extensions.applyNavigationBarColor
import hu.mrolcsi.muzik.extensions.applyStatusBarColor
import hu.mrolcsi.muzik.extensions.applyThemeAnimated
import hu.mrolcsi.muzik.theme.Theme
import kotlinx.android.synthetic.main.fragment_library.*
import javax.inject.Inject

class LibraryFragment : DaggerFragment() {

  @Inject lateinit var viewModel: LibraryViewModel

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
    FragmentLibraryBinding.inflate(inflater, container, false).apply {
      theme = viewModel.currentTheme
      lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    libraryToolbar.setTitle(R.string.library_title)

    setupNavigation(requireActivity().findNavController(R.id.libraryNavHost))

    viewModel.currentTheme.observe(viewLifecycleOwner, object : Observer<Theme> {

      private var initialLoad = true

      override fun onChanged(theme: Theme) {
        if (initialLoad) {
          applyPrimaryBackgroundColor(theme.primaryBackgroundColor)
          initialLoad = false
        } else {
          applyThemeAnimated(
            previousTheme = viewModel.previousTheme,
            newTheme = theme,
            applyPrimaryBackgroundColor = this@LibraryFragment::applyPrimaryBackgroundColor
          )
        }
      }
    })
  }

  override fun onResume() {
    super.onResume()

    // Apply StatusBar and NavigationBar colors again
    viewModel.currentTheme.value?.let {
      applyPrimaryBackgroundColor(it.primaryBackgroundColor)
    }
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
          // Show AppBar, hide NavigationBar
          appBar.setExpanded(true, true)
        }
      }
    }
  }

  private fun applyPrimaryBackgroundColor(color: Int) {
    Log.d(LOG_TAG, "Applying theme...")

    // Status Bar
    activity?.applyStatusBarColor(color)

    // Navigation
    activity?.applyNavigationBarColor(color)
  }

  companion object {
    private const val LOG_TAG = "LibraryFragment"
  }
}