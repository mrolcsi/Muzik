package hu.mrolcsi.muzik.main

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import hu.mrolcsi.muzik.R
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
    setContentView(R.layout.activity_main)

    viewModel.currentTheme.observe(this, Observer { theme ->
      window.setBackgroundDrawable(ColorDrawable(theme.primaryBackgroundColor))
      updateStatusBarIcons(theme.primaryBackgroundColor)
      updateNavigationIcons(theme.primaryBackgroundColor)
    })
  }

}