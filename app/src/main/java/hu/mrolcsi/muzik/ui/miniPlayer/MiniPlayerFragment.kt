package hu.mrolcsi.muzik.ui.miniPlayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.databinding.FragmentMiniplayerBinding
import hu.mrolcsi.muzik.ui.common.OnSwipeTouchListener
import hu.mrolcsi.muzik.ui.common.observeAndRunNavCommands
import hu.mrolcsi.muzik.ui.common.observeAndRunUiCommands
import kotlinx.android.synthetic.main.fragment_miniplayer.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class MiniPlayerFragment : Fragment() {

  private val viewModel: MiniPlayerViewModel by viewModel<MiniPlayerViewModelImpl>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    postponeEnterTransition()
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
    FragmentMiniplayerBinding.inflate(inflater, container, false).also {
      it.viewModel = viewModel
      it.theme = viewModel.currentTheme
      it.lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    viewModel.apply {
      requireContext().observeAndRunUiCommands(viewLifecycleOwner, this)
      (requireActivity()
        .supportFragmentManager
        .findFragmentById(R.id.mainNavHost) as NavHostFragment)
        .navController
        .observeAndRunNavCommands(viewLifecycleOwner, this)

      coverArtUri.observe(viewLifecycleOwner, {
        Picasso.get()
          .load(it)
          .into(imgCoverArt, object : Callback {
            override fun onSuccess() {
              startPostponedEnterTransition()
            }

            override fun onError(e: Exception?) {}
          })
      })
    }

    view.setOnClickListener {
      viewModel.openPlayer()
    }
    view.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {
      override fun onSwipeUp() {
        viewModel.openPlayer()
      }
    })
  }

  override fun onDestroyView() {
    super.onDestroyView()
    Picasso.get().cancelRequest(imgCoverArt)
  }
}