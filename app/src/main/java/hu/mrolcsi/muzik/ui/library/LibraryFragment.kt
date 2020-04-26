package hu.mrolcsi.muzik.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.viewpager.widget.ViewPager
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.android.lifecycle.autoDispose
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.databinding.FragmentLibraryBinding
import hu.mrolcsi.muzik.ui.common.ConfigurableFragmentPagerAdapter
import hu.mrolcsi.muzik.ui.common.extensions.updateStatusBarIcons
import hu.mrolcsi.muzik.ui.common.observeAndRunNavCommands
import hu.mrolcsi.muzik.ui.common.setupIcons
import kotlinx.android.synthetic.main.fragment_library.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class LibraryFragment : Fragment() {

  private val rxPermissions: RxPermissions by inject { parametersOf(this) }

  private val viewModel: LibraryViewModel by viewModel<LibraryViewModelImpl> { parametersOf(this) }

  private val pagerAdapter by lazy {
    ConfigurableFragmentPagerAdapter(childFragmentManager)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
    FragmentLibraryBinding.inflate(inflater).also {
      it.viewModel = viewModel
      it.theme = viewModel.currentTheme
      it.lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    // TODO: Move to ViewModel
    viewModel.requestPermissionEvent.observe(viewLifecycleOwner, Observer {
      rxPermissions.requestEachCombined(*it)
        .autoDispose(viewLifecycleOwner)
        .subscribe(
          { permission ->
            if (permission.granted) viewModel.onPermissionGranted()
            else viewModel.onPermissionDenied(permission.shouldShowRequestPermissionRationale)
          },
          { e -> Timber.e(e) }
        )
    })

    viewModel.pages.observe(viewLifecycleOwner, Observer {
      pagerAdapter.onChanged(it)
      libraryTabs.setupIcons(pagerAdapter)
    })
    libraryPager.adapter = pagerAdapter
    libraryTabs.setupWithViewPager(libraryPager)

    libraryPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
      override fun onPageSelected(position: Int) {
        appBar.liftOnScrollTargetViewId = when (position) {
          0 -> R.id.rvArtists
          1 -> R.id.rvAlbums
          2 -> R.id.rvSongs
          else -> -1
        }
      }
    })

    findNavController().apply {
      observeAndRunNavCommands(viewLifecycleOwner, viewModel)
      addOnDestinationChangedListener { _, _, _ ->
        activity?.window?.apply {
          viewModel.currentTheme.value?.backgroundColor?.let {
            updateStatusBarIcons(it)
          }
        }
      }
    }
  }
}