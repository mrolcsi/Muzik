package hu.mrolcsi.muzik.main

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.databinding.ActivityMainBinding
import hu.mrolcsi.muzik.extensions.updateNavigationIcons
import hu.mrolcsi.muzik.extensions.updateStatusBarIcons
import hu.mrolcsi.muzik.theme.ThemedViewModel
import hu.mrolcsi.muzik.theme.ThemedViewModelImpl
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

  private val viewModel: ThemedViewModel by viewModel<ThemedViewModelImpl>()

  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.FluxTheme)

    super.onCreate(savedInstanceState)
    DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main).also {
      it.theme = viewModel.currentTheme
      it.lifecycleOwner = this
    }

    viewModel.currentTheme.observe(this, Observer { theme ->
      window.apply {
        setBackgroundDrawable(ColorDrawable(theme.primaryBackgroundColor))
        updateStatusBarIcons(theme.primaryBackgroundColor)
        updateNavigationIcons(theme.primaryBackgroundColor)
      }
    })
  }

}