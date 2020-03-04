package hu.mrolcsi.muzik.ui.player

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.BaseTest
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.data.model.theme.Theme
import hu.mrolcsi.muzik.ui.playlist.PlaylistViewModelImpl
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.android.synthetic.main.fragment_player_content.*
import org.junit.Before
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

class PlayerDialogFragmentTest : BaseTest() {

  @RelaxedMockK
  private lateinit var mockViewModel: PlayerViewModelImpl

  override val testModule = module(override = true) {
    viewModel { mockViewModel }
    viewModel {
      mockk<PlaylistViewModelImpl>(relaxed = true) {
        every { currentTheme } returns MutableLiveData<Theme>(Theme.DEFAULT_THEME)
      }
    }
  }

  private fun withSut(action: PlayerDialogFragment.() -> Unit) =
    launchFragmentInContainer(themeResId = R.style.FluxTheme) {
      PlayerDialogFragment().useMockNavController()
    }.onFragment(action)

  @Before
  fun setUp() {
    every { mockViewModel.currentTheme } returns MutableLiveData<Theme>(Theme.DEFAULT_THEME)
  }

  @Test
  fun `When Play-Pause button is pressed, call onPlayPauseClicked`() {
    withSut {
      btnPlayPause.performClick()

      verify { mockViewModel.onPlayPauseClick() }
    }
  }
}