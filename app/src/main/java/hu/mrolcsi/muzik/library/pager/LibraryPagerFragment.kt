package hu.mrolcsi.muzik.library.pager

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import dagger.android.support.DaggerFragment
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.databinding.FragmentLibraryPagerBinding
import hu.mrolcsi.muzik.library.albums.AlbumsFragment
import hu.mrolcsi.muzik.library.artists.ArtistsFragment
import hu.mrolcsi.muzik.library.songs.SongsFragment
import kotlinx.android.synthetic.main.fragment_library_pager.*
import javax.inject.Inject

class LibraryPagerFragment : DaggerFragment() {

  @Inject lateinit var viewModel: LibraryPagerViewModel

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
    FragmentLibraryPagerBinding.inflate(inflater).also {
      it.theme = viewModel.currentTheme
      it.lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    libraryPager.adapter = LibraryPagerAdapter(requireContext(), childFragmentManager)
    libraryTabs.setupWithViewPager(libraryPager)
    libraryPagerToolbar.setupWithNavController(findNavController())
  }

  class LibraryPagerAdapter(private val context: Context, fm: FragmentManager) :
    FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

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