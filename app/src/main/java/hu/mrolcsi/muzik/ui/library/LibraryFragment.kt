package hu.mrolcsi.muzik.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.tbruyelle.rxpermissions2.RxPermissions
import hu.mrolcsi.muzik.databinding.FragmentLibraryBinding
import hu.mrolcsi.muzik.ui.common.ConfigurableFragmentPagerAdapter
import hu.mrolcsi.muzik.ui.common.observeAndRunNavCommands
import hu.mrolcsi.muzik.ui.common.setupIcons
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_library.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class LibraryFragment : Fragment() {

  private val rxPermissions: RxPermissions by inject { parametersOf(this) }

  private val viewModel: LibraryViewModel by viewModel<LibraryViewModelImpl> { parametersOf(this) }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
    FragmentLibraryBinding.inflate(inflater).also {
      it.viewModel = viewModel
      it.theme = viewModel.currentTheme
      it.lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    viewModel.requestPermissionEvent.observe(viewLifecycleOwner, Observer {
      rxPermissions.requestEachCombined(*it)
        .subscribeBy { permission ->
          if (permission.granted) viewModel.onPermissionGranted()
          else viewModel.onPermissionDenied(permission.shouldShowRequestPermissionRationale)
        }
    })

    val adapter = ConfigurableFragmentPagerAdapter(childFragmentManager).also {
      libraryPager.adapter = it
    }

    viewModel.pages.observe(viewLifecycleOwner, Observer {
      adapter.onChanged(it)
      libraryTabs.setupIcons(adapter)
    })

    libraryTabs.setupWithViewPager(libraryPager)

    findNavController().observeAndRunNavCommands(viewLifecycleOwner, viewModel)
  }
}