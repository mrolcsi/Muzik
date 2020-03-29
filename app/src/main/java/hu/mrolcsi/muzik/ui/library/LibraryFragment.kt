package hu.mrolcsi.muzik.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.tbruyelle.rxpermissions2.RxPermissions
import hu.mrolcsi.muzik.databinding.FragmentLibraryBinding
import hu.mrolcsi.muzik.ui.base.RxFragment
import hu.mrolcsi.muzik.ui.common.ConfigurableFragmentPagerAdapter
import hu.mrolcsi.muzik.ui.common.observeAndRunNavCommands
import hu.mrolcsi.muzik.ui.common.setupIcons
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_library.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class LibraryFragment : RxFragment() {

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
        .subscribeBy(
          onNext = { permission ->
            if (permission.granted) viewModel.onPermissionGranted()
            else viewModel.onPermissionDenied(permission.shouldShowRequestPermissionRationale)
          },
          onError = { e -> Timber.e(e) }
        ).disposeOnDestroy()
    })

    viewModel.pages.observe(viewLifecycleOwner, Observer {
      pagerAdapter.onChanged(it)
      libraryTabs.setupIcons(pagerAdapter)
    })
    libraryPager.adapter = pagerAdapter
    libraryTabs.setupWithViewPager(libraryPager)

    findNavController().observeAndRunNavCommands(viewLifecycleOwner, viewModel)
  }
}