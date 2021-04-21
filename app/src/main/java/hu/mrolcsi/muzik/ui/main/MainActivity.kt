package hu.mrolcsi.muzik.ui.main

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.databinding.ActivityMainBinding
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModelImpl
import hu.mrolcsi.muzik.ui.common.extensions.updateNavigationIcons
import hu.mrolcsi.muzik.ui.common.extensions.updateStatusBarIcons
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

  private val viewModel: ThemedViewModel by viewModel<ThemedViewModelImpl>()

  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.FluxTheme)

    super.onCreate(savedInstanceState)
    DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main).also { binding ->
      binding.theme = viewModel.currentTheme
      binding.lifecycleOwner = this

      (supportFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment).let { navHostFragment ->
        bottomNavigation.setupWithNavController(navHostFragment.navController)
      }
    }

    viewModel.currentTheme.observe(this) { theme ->
      window.apply {
        setBackgroundDrawable(ColorDrawable(theme.backgroundColor))
        updateStatusBarIcons(theme.backgroundColor)
        updateNavigationIcons(theme.backgroundColor)
      }
    }
  }
}