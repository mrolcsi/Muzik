package hu.mrolcsi.muzik.data.service.media.exoplayer

import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import hu.mrolcsi.muzik.base.BaseTest
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Test
import org.koin.dsl.module
import kotlin.test.assertEquals

class ExoPlayerAdapterImplTests : BaseTest<ExoPlayerAdapter>() {

  @RelaxedMockK
  private lateinit var exoPlayer: Player

  @MockK
  private lateinit var metadataProvider: ExoMetadataProvider

  @RelaxedMockK
  private lateinit var mediaSessionConnector: MediaSessionConnector

  override val testModule = module(override = true) {
    single { exoPlayer }
    single { metadataProvider }
    single { mediaSessionConnector }
  }

  override fun createSut() = ExoPlayerAdapterImpl()

  @Test
  fun `WHEN adapter is initialized, THEN set up MediaSessionConnector`() {
    withSut {
      verify {
        mediaSessionConnector.setPlayer(exoPlayer)

        // TODO: verify additional setters, capture listeners if needed
      }
    }
  }

  @Test
  fun `When release is called, THEN release ExoPlayer AND disconnect MediaSessionConnector`() {
    withSut {
      release()

      verify {
        exoPlayer.release()
        mediaSessionConnector.setPlayer(null)
      }
    }
  }

  @Test
  fun `WHEN loadQueue is called, THEN clear previous queue AND add new items`() {
    TODO()
  }

  @Test
  fun `WHEN loadLastPlayed is called, THEN do something`() {
    TODO()
  }

  @Test
  fun `WHEN isPlaying is called, THEN return if the player is playing`() {
    withSut {
      listOf(true, false).forEach { isPlaying ->
        every { exoPlayer.playWhenReady } returns isPlaying

        assertEquals(isPlaying, isPlaying())
      }
    }
  }
}