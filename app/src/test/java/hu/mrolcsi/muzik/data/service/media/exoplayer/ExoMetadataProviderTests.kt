package hu.mrolcsi.muzik.data.service.media.exoplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaControllerCompat
import androidx.core.os.bundleOf
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.base.BaseTest
import hu.mrolcsi.muzik.data.MediaStoreWrapper
import hu.mrolcsi.muzik.util.MediaMetadataCompatMatcher
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExoMetadataProviderTests : BaseTest<ExoMetadataProvider>() {

  @MockK
  private lateinit var mediaStore: MediaStoreWrapper

  @MockK
  private lateinit var mediaController: MediaControllerCompat

  @MockK
  private lateinit var mediaSessionConnector: MediaSessionConnector

  override val testModule = module(override = true) {
    single { mediaStore }
    single { mediaController }
    single { mediaSessionConnector }
  }

  override fun createSut() = ExoMetadataProvider()

  @Before
  fun setUp() {
    every { mediaSessionConnector.invalidateMediaSessionMetadata() } just Runs
  }

  @Test
  fun `GIVEN a metadata is already cached, WHEN getMetadata is called, THEN return cached metadata`() {
    withSut {
      val (bitmap, mockPlayer) = prepareMetadata()

      val expected = MediaMetadataCompat.Builder()
        // Player
        .putLong(MediaMetadataCompat.METADATA_KEY_ADVERTISEMENT, 1)
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 9999)
        // Description
        .putText(MediaMetadataCompat.METADATA_KEY_TITLE, "Title")
        .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "Title")
        .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "Subtitle")
        .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, "Description")
        .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bitmap)
        .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, "http://example.com/icon.png")
        .putText(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "/path/to/media.mp3")
        .putText(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, "file://path/to/media.mp3")
        .putText(MediaStore.Audio.Media._ID, "123")
        .putText(MediaStore.Audio.Media.ALBUM_ID, "234")
        .putText("STRING", "StringExtra")
        .putLong("LONG", 1234L)
        .putLong("INT", 456)
        .putBitmap("BITMAP", bitmap)
        .putRating("RATING", RatingCompat.newHeartRating(true))
        // Album Art
        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
        .build()

      // first call returns a placeholder
      getMetadata(mockPlayer)

      verify { mediaSessionConnector.invalidateMediaSessionMetadata() }

      val actual = getMetadata(mockPlayer)

      assertEquals(expected.description.toString(), actual.description.toString())
      assertTrue { MediaMetadataCompatMatcher(expected).match(actual) }
    }
  }

  @Test
  fun `GIVEN a metadata is not cached yet, WHEN getMetadata is called, THEN return placeholder metadata AND create metadata in the background`() {
    withSut {
      val (_, player) = prepareMetadata()

      val expected = MediaMetadataCompat.Builder().build()
      val actual = getMetadata(player)

      assertTrue { MediaMetadataCompatMatcher(expected).match(actual) }
    }
  }

  private fun prepareMetadata(): Pair<Bitmap, Player> {
    val bitmap = BitmapFactory.decodeResource(context<Context>().resources, R.drawable.ic_launcher)

    val description = MediaDescriptionCompat.Builder()
      .setTitle("Title")
      .setSubtitle("Subtitle")
      .setDescription("Description")
      .setIconBitmap(bitmap)
      .setIconUri(Uri.parse("http://example.com/icon.png"))
      .setMediaId("/path/to/media.mp3")
      .setMediaUri(Uri.parse("file://path/to/media.mp3"))
      .setExtras(
        bundleOf(
          MediaStore.Audio.Media._ID to "123",
          MediaStore.Audio.Media.ALBUM_ID to "234",
          "STRING" to "StringExtra",
          "LONG" to 1234L,
          "INT" to 456,
          "BITMAP" to bitmap,
          "RATING" to RatingCompat.newHeartRating(true)
        )
      )
      .build()

    val mockPlayer = mockk<Player> {
      every { currentTimeline.isEmpty } returns false
      every { currentTag } returns description
      every { isPlayingAd } returns true
      every { duration } returns 9999
    }

    val albumId = description.extras!!.getString(MediaStore.Audio.Media.ALBUM_ID)!!.toLong()
    val albumArtUri = Uri.parse("content://media/external/audio/albumart/$albumId")
    every { mediaStore.getBitmap(albumArtUri) } returns bitmap

    return Pair(bitmap, mockPlayer)
  }
}