package hu.mrolcsi.muzik.miniplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.glide.onResourceReady
import hu.mrolcsi.muzik.common.view.OnSwipeTouchListener
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunNavCommands
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunUiCommands
import hu.mrolcsi.muzik.databinding.FragmentMiniplayerBinding
import kotlinx.android.synthetic.main.fragment_miniplayer.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class MiniPlayerFragment : Fragment() {

  private val viewModel: MiniPlayerViewModel by viewModel<MiniPlayerViewModelImpl>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    postponeEnterTransition()
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    viewModel.apply {
      requireContext().observeAndRunUiCommands(viewLifecycleOwner, this)
      requireActivity().findNavController(R.id.mainNavHost).observeAndRunNavCommands(viewLifecycleOwner, this)

      coverArtUri.observe(viewLifecycleOwner, Observer {
        GlideApp.with(imgCoverArt)
          .load(it)
          .onResourceReady { startPostponedEnterTransition() }
          .into(imgCoverArt)
      })
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
      override fun onSwipeUp() {
        viewModel.openPlayer(imgCoverArt)
      }
    })
  }
}