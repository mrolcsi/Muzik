package hu.mrolcsi.muzik.library

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import dagger.android.support.DaggerFragment
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunNavCommands
import hu.mrolcsi.muzik.databinding.FragmentLibraryBinding
import hu.mrolcsi.muzik.extensions.applyNavigationBarColor
import hu.mrolcsi.muzik.extensions.applyStatusBarColor
import hu.mrolcsi.muzik.theme.Theme
import javax.inject.Inject

class LibraryFragment : DaggerFragment() {

  private val args: LibraryFragmentArgs by navArgs()

  @Inject lateinit var viewModel: LibraryViewModel

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
    FragmentLibraryBinding.inflate(inflater, container, false).apply {
      theme = viewModel.currentTheme
      lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    viewModel.currentTheme.observe(viewLifecycleOwner, Observer<Theme> {
      applyPrimaryBackgroundColor(it.primaryBackgroundColor)
    })

    requireActivity()
      .findNavController(R.id.libraryNavHost)
      .observeAndRunNavCommands(viewLifecycleOwner, viewModel)

    viewModel.navDirection = args.navDirections
  }

  override fun onResume() {
    super.onResume()

    // Apply StatusBar and NavigationBar colors again
    viewModel.currentTheme.value?.let {
      applyPrimaryBackgroundColor(it.primaryBackgroundColor)
    }
  }

  private fun applyPrimaryBackgroundColor(color: Int) {
    Log.d(LOG_TAG, "Applying theme...")

    activity?.run {
      window?.setBackgroundDrawable(ColorDrawable(color))
      applyStatusBarColor(color)
      applyNavigationBarColor(color)
    }
  }

  companion object {
    private const val LOG_TAG = "LibraryFragment"
  }
}