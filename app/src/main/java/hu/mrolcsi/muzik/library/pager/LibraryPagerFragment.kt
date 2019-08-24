package hu.mrolcsi.muzik.library.pager

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import dagger.android.support.DaggerFragment
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.library.albums.AlbumsFragment
import hu.mrolcsi.muzik.library.artists.ArtistsFragment
import hu.mrolcsi.muzik.library.songs.SongsFragment
import hu.mrolcsi.muzik.theme.Theme
import kotlinx.android.synthetic.main.fragment_library_pager.*
import javax.inject.Inject

class LibraryPagerFragment : DaggerFragment() {

  @Inject lateinit var viewModel: LibraryPagerViewModel

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_library_pager, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    libraryPager.adapter = LibraryPagerAdapter(requireContext(), childFragmentManager)
    libraryTabs.setupWithViewPager(libraryPager)

    viewModel.currentTheme.observe(
      viewLifecycleOwner,
      object : Observer<Theme> {
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

  private fun applyThemeStatic(theme: Theme) {
    theme.primaryBackgroundColor.also { color ->
      // Tabs
      libraryTabs.setBackgroundColor(color)
    }

    theme.primaryForegroundColor.also { color ->
      // Tabs
      libraryTabs.setTabTextColors(
        ColorUtils.setAlphaComponent(color, Theme.DISABLED_OPACITY),
        color
      )
      libraryTabs.setSelectedTabIndicatorColor(color)
    }
  }

  private fun applyThemeAnimated(theme: Theme) {

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

        // Tabs
        libraryTabs.setBackgroundColor(color)
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

        // Tabs
        libraryTabs.setTabTextColors(
          ColorUtils.setAlphaComponent(color, Theme.DISABLED_OPACITY),
          color
        )
        libraryTabs.setSelectedTabIndicatorColor(color)
      }
      start()
    }
  }

  class LibraryPagerAdapter(private val context: Context, fm: FragmentManager) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
      return when (position) {
        0 -> ArtistsFragment()
        1 -> AlbumsFragment()
        2 -> SongsFragment()
        else -> throw IllegalArgumentException()
      }
    }

    override fun getCount(): Int = 3

    override fun getPageTitle(position: Int): CharSequence? {
      return when (position) {
        0 -> context.getString(R.string.artists_title)
        1 -> context.getString(R.string.albums_title)
        2 -> context.getString(R.string.songs_title)
        else -> throw IllegalArgumentException()
      }
    }
  }
}