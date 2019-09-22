package hu.mrolcsi.muzik.splash

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import hu.mrolcsi.muzik.common.viewmodel.observeAndRunNavCommands
import hu.mrolcsi.muzik.databinding.FragmentSplashBinding
import javax.inject.Inject

class SplashFragment : Fragment(), HasAndroidInjector {

  @Inject internal lateinit var androidInjector: DispatchingAndroidInjector<Any>

  @Inject lateinit var viewModel: SplashViewModel

  override fun androidInjector(): AndroidInjector<Any> = androidInjector

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AndroidSupportInjection.inject(this)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
    FragmentSplashBinding.inflate(inflater, container, false).also {
      it.viewModel = viewModel
      it.theme = viewModel.currentTheme
      it.lifecycleOwner = viewLifecycleOwner
    }.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    viewModel.apply {
      findNavController().observeAndRunNavCommands(viewLifecycleOwner, this)
      currentTheme.observe(viewLifecycleOwner, Observer {
        activity?.window?.setBackgroundDrawable(ColorDrawable(it.secondaryBackgroundColor))
      })
    }
  }
}
