package hu.mrolcsi.muzik.ui.player

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.BaseTest
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.ui.playlist.PlaylistViewModelImpl
import hu.mrolcsi.muzik.data.model.theme.Theme
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.android.synthetic.main.content_player.*
import org.junit.Before
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class PlayerDialogFragmentTest : BaseTest() {

  private val mockViewModel: PlayerViewModel = mockk<PlayerViewModelImpl>(relaxed = true)

  private val testModule = module(override = true) {
    viewModel { mockViewModel as PlayerViewModelImpl }
    viewModel {
      mockk<PlaylistViewModelImpl>(relaxed = true) {
        every { currentTheme } returns MutableLiveData<Theme>(mockk(relaxed = true))
      }
    }
  }

  private fun withSut(action: PlayerDialogFragment.() -> Unit) =
    launchFragmentInContainer(themeResId = R.style.FluxTheme) {
      PlayerDialogFragment().useMockNavController()
    }.onFragment(action)

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    loadKoinModules(testModule)
  }

  @Test
  fun `When Play-Pause button is pressed, call onPlayPauseClicked`() {
    withSut {
      btnPlayPause.performClick()

      verify { mockViewModel.onPlayPauseClick() }
    }
  }
}