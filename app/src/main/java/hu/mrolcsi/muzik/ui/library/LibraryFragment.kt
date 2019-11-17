package hu.mrolcsi.muzik.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import hu.mrolcsi.muzik.ui.common.ConfigurableFragmentPagerAdapter
import hu.mrolcsi.muzik.ui.common.observeAndRunNavCommands
import hu.mrolcsi.muzik.databinding.FragmentLibraryBinding
import kotlinx.android.synthetic.main.fragment_library.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class LibraryFragment : Fragment() {

  private val viewModel: LibraryViewModel by viewModel<LibraryViewModelImpl> { parametersOf(this) }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
    FragmentLibraryBinding.inflate(inflater).also {
      it.viewModel = viewModel
      it.theme = viewModel.currentTheme
      it.lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    viewModel.pages.observe(viewLifecycleOwner, ConfigurableFragmentPagerAdapter(
      childFragmentManager
    ).also {
      libraryPager.adapter = it
    })

    libraryTabs.setupWithViewPager(libraryPager)
    libraryPagerToolbar.setupWithNavController(findNavController())
    findNavController().observeAndRunNavCommands(viewLifecycleOwner, viewModel)
  }
}