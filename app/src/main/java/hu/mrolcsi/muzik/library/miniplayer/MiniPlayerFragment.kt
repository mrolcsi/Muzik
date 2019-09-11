package hu.mrolcsi.muzik.library.miniplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment
import dagger.android.support.DaggerFragment
import hu.mrolcsi.muzik.common.view.OnSwipeTouchListener
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunNavCommands
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunUiCommands
import hu.mrolcsi.muzik.databinding.FragmentMiniplayerBinding
import kotlinx.android.synthetic.main.fragment_miniplayer.*
import javax.inject.Inject

class MiniPlayerFragment : DaggerFragment() {

  @Inject lateinit var viewModel: MiniPlayerViewModel

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    viewModel.apply {
      requireContext().observeAndRunUiCommands(viewLifecycleOwner, this)
      NavHostFragment.findNavController(requireParentFragment()).observeAndRunNavCommands(viewLifecycleOwner, this)
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
    FragmentMiniplayerBinding.inflate(inflater, container, false).also {
      it.viewModel = viewModel
      it.theme = viewModel.currentTheme
      it.lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    view.setOnClickListener {
      viewModel.openPlayer(imgCoverArt)
    }
    view.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {
      override fun onSwipeDown() {
        viewModel.openPlayer(imgCoverArt)
      }
    })
  }
}