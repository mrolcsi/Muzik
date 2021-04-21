package hu.mrolcsi.muzik.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import hu.mrolcsi.muzik.databinding.FragmentDashboardBinding
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModelImpl
import org.koin.androidx.viewmodel.ext.android.viewModel

class DashboardFragment : Fragment() {

  private val viewModel: ThemedViewModel by viewModel<ThemedViewModelImpl>()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
    FragmentDashboardBinding.inflate(inflater, container, false).also {
      it.lifecycleOwner = viewLifecycleOwner
      it.theme = viewModel.currentTheme
    }.root
}