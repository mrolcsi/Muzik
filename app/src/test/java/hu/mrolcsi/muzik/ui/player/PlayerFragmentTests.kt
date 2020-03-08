package hu.mrolcsi.muzik.ui.player

import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.base.BaseFragmentTest
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

class PlayerDialogFragmentTest : BaseFragmentTest<PlayerDialogFragment>() {

  @RelaxedMockK
  private lateinit var mockViewModel: PlayerViewModelImpl

  override val testModule = module(override = true) {
    viewModel { mockViewModel }
    viewModel {
      mockk<PlaylistViewModelImpl>(relaxed = true) {
        every { currentTheme } returns MutableLiveData(Theme.DEFAULT_THEME)
      }
    }
  }

  override fun createSut() = PlayerDialogFragment()

  @Before
  fun setUp() {
    every { mockViewModel.currentTheme } returns MutableLiveData(Theme.DEFAULT_THEME)
  }

  @Test
  fun `When Play-Pause button is pressed, call onPlayPauseClicked`() {
    withSut {
      btnPlayPause.performClick()

      verify { mockViewModel.onPlayPauseClick() }
    }
  }
}