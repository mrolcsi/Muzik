package hu.mrolcsi.muzik.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunNavCommands
import hu.mrolcsi.muzik.databinding.FragmentLibraryBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class LibraryFragment : Fragment() {

  private val args: LibraryFragmentArgs by navArgs()

  private val viewModel: LibraryViewModel by viewModel<LibraryViewModelImpl>()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
    FragmentLibraryBinding.inflate(inflater, container, false).apply {
      theme = viewModel.currentTheme
      lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    requireActivity()
      .findNavController(R.id.libraryNavHost)
      .observeAndRunNavCommands(viewLifecycleOwner, viewModel)

    viewModel.navDirection = args.navDirections
  }

  companion object {
    private const val LOG_TAG = "LibraryFragment"
  }
}