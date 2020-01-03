package hu.mrolcsi.muzik.ui.albumDetails

import android.support.v4.media.MediaMetadataCompat
import hu.mrolcsi.muzik.BaseTest
import hu.mrolcsi.muzik.TestData
import hu.mrolcsi.muzik.data.manager.media.MediaManager
import hu.mrolcsi.muzik.data.model.media.coverArtUri
import hu.mrolcsi.muzik.data.repository.media.MediaRepository
import hu.mrolcsi.muzik.ui.albums.DiscNumberItem
import hu.mrolcsi.muzik.ui.base.ThemedViewModelImpl
import hu.mrolcsi.muzik.ui.common.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.ui.common.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.ui.common.ObservableImpl
import hu.mrolcsi.muzik.ui.songs.SongItem
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import kotlin.test.assertEquals

class AlbumDetailsViewModelImplTest : BaseTest() {

  @MockK
  private lateinit var mockMediaManager: MediaManager
  @MockK
  private lateinit var mockMediaRepo: MediaRepository

  private val testModule = module(override = true) {
    single { mockMediaManager }
    single { mockMediaRepo }
  }

  private fun withSut(action: AlbumDetailsViewModelImpl.() -> Unit) =
    AlbumDetailsViewModelImpl(
      ObservableImpl(),
      ExecuteOnceUiCommandSource(),
      ExecuteOnceNavCommandSource(),
      ThemedViewModelImpl()
    ).apply(action)

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    loadKoinModules(testModule)

    val metadata = mockk<MediaMetadataCompat> {
      every { getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) } returns "MEDIA_ID"
    }
    every { mockMediaManager.mediaMetadata } returns Observable.just(metadata)
  }

  @Test
  fun `GIVEN an album has a single disc, WHEN songs are loaded from the album, THEN do not add Disc Number indicators`() {
    val albumId = 1024L

    val albumItem = TestData.createAlbumMediaItem(albumId, "Test Album", "Artist")

    val songs = listOf(
      TestData.createSongMediaItem(1001, "MEDIA_ID_S1", "Disc 1 Song 1", "Artist", -1, 1, 11000),
      TestData.createSongMediaItem(1002, "MEDIA_ID_S2", "Disc 1 Song 2", "Artist", -1, 2, 12000)
    )

    every { mockMediaRepo.getAlbumById(albumId) } returns Observable.just(albumItem)
    every { mockMediaRepo.getSongsFromAlbum(albumId) } returns Observable.just(songs)

    withSut {
      setArgument(albumId)

      val expected = listOf(
        SongItem(1001, songs[0].description.coverArtUri, "1", false, "Artist", "Disc 1 Song 1", "00:11"),
        SongItem(1002, songs[1].description.coverArtUri, "2", false, "Artist", "Disc 1 Song 2", "00:12")
      )

      assertEquals(expected, items.value)
    }
  }

  @Test
  fun `GIVEN an album is on multiple discs, WHEN songs from the album are loaded, THEN add Disc Number indicators`() {
    val albumId = 1024L

    val albumItem = TestData.createAlbumMediaItem(albumId, "Test Album", "Artist")

    val songs = listOf(
      TestData.createSongMediaItem(1001, "MEDIA_ID_D1S1", "Disc 1 Song 1", "Artist", 1, 1, 11000),
      TestData.createSongMediaItem(1002, "MEDIA_ID_D1S2", "Disc 1 Song 2", "Artist", 1, 2, 12000),
      TestData.createSongMediaItem(2001, "MEDIA_ID_D2S1", "Disc 2 Song 1", "Artist", 2, 1, 21000),
      TestData.createSongMediaItem(2002, "MEDIA_ID_D2S2", "Disc 2 Song 2", "Artist", 2, 2, 22000)
    )

    every { mockMediaRepo.getAlbumById(albumId) } returns Observable.just(albumItem)
    every { mockMediaRepo.getSongsFromAlbum(albumId) } returns Observable.just(songs)

    val metadata = mockk<MediaMetadataCompat> {
      every { getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) } returns "MEDIA_ID"
    }

    every { mockMediaManager.mediaMetadata } returns Observable.just(metadata)

    withSut {
      setArgument(albumId)

      val expected = listOf(
        DiscNumberItem(1, "1"),
        SongItem(1001, songs[0].description.coverArtUri, "1", false, "Artist", "Disc 1 Song 1", "00:11"),
        SongItem(1002, songs[1].description.coverArtUri, "2", false, "Artist", "Disc 1 Song 2", "00:12"),
        DiscNumberItem(2, "2"),
        SongItem(2001, songs[2].description.coverArtUri, "1", false, "Artist", "Disc 2 Song 1", "00:21"),
        SongItem(2002, songs[3].description.coverArtUri, "2", false, "Artist", "Disc 2 Song 2", "00:22")
      )

      assertEquals(expected, items.value)
    }
  }
}