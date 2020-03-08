package hu.mrolcsi.muzik.data.service.media

import android.app.Service
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.session.MediaButtonReceiver
import androidx.navigation.NavDeepLinkBuilder
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.TestData
import hu.mrolcsi.muzik.base.BaseTest
import hu.mrolcsi.muzik.data.local.playQueue.PlayQueueDao2
import hu.mrolcsi.muzik.data.model.playQueue.LastPlayed
import hu.mrolcsi.muzik.data.service.media.exoplayer.ExoPlayerAdapter
import hu.mrolcsi.muzik.data.service.media.exoplayer.NotificationEvent
import hu.mrolcsi.muzik.data.service.media.exoplayer.PlayerEvent
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.android.controller.ServiceController
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MuzikPlayerServiceTests : BaseTest<MuzikPlayerService>() {

  private val mediaSession = spyk(MediaSessionCompat(context(), "MuzikPlayerService"))

  @MockK
  private lateinit var playQueueDao: PlayQueueDao2

  @MockK(relaxUnitFun = true)
  private lateinit var exoPlayerAdapter: ExoPlayerAdapter

  override val testModule = module(override = true) {
    single { mediaSession }
    single { playQueueDao }
    single { exoPlayerAdapter }
  }

  private lateinit var serviceController: ServiceController<MuzikPlayerService>

  override fun createSut(): MuzikPlayerService =
    Robolectric.buildService(MuzikPlayerService::class.java).also { serviceController = it }.create().get()

  private val playerEventSubject = PublishSubject.create<PlayerEvent>()
  private val notificationEventSubject = PublishSubject.create<NotificationEvent>()

  @Before
  fun setUp() {
    every { playQueueDao.getPlayQueue() } returns Single.just(emptyList())
    every { playQueueDao.getLastPlayed() } returns Single.just(LastPlayed())

    every { exoPlayerAdapter.loadQueue(any()) } returns Completable.complete()
    every { exoPlayerAdapter.loadLastPlayed(any()) } returns Completable.complete()
    every { exoPlayerAdapter.playerEvents } returns playerEventSubject
    every { exoPlayerAdapter.notificationEvents } returns notificationEventSubject
  }

  @Test
  fun `WHEN the service receives a start command, THEN handle intent through MediaButtonReceiver`() {
    mockkStatic(MediaButtonReceiver::class)

    withSut {
      val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
      assertEquals(Service.START_STICKY, onStartCommand(intent, 0, 0))

      verify { MediaButtonReceiver.handleIntent(mediaSession, intent) }
    }

    unmockkStatic(MediaButtonReceiver::class)
  }

  @Test
  fun `WHEN the service is created, THEN initialize the media session`() {
    withSut {
      // Session Token is set
      assertEquals(mediaSession.sessionToken, sessionToken)

      // Flags are set
      verify { mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS) }

      // SessionActivity is set
      val pi = NavDeepLinkBuilder(context())
        .setGraph(R.navigation.main_navigation)
        .setDestination(R.id.navPlayer)
        .createPendingIntent()
      val expectedIntent = Shadows.shadowOf(pi).savedIntent
      verify {
        mediaSession.setSessionActivity(match {
          val actualIntent = Shadows.shadowOf(it).savedIntent
          expectedIntent.extras?.toString() == actualIntent.extras?.toString()
        })
      }
    }
  }

  @Test
  fun `GIVEN there is a saved PlayQueue, WHEN the service is created, THEN load last played queue into Player`() {
    val playQueue = listOf(
      TestData.createPlayQueueEntry(
        id = 11,
        filePath = "/media/music/song1.mp3",
        mediaId = 12,
        artist = "Daft Punk",
        artistId = 13,
        album = "Discovery",
        albumId = 14,
        title = "One More Time",
        duration = 1000
      ),
      TestData.createPlayQueueEntry(
        id = 21,
        filePath = "/media/music/song2.mp3",
        mediaId = 22,
        artist = "The Chemical Brothers",
        artistId = 23,
        album = "Further",
        albumId = 24,
        title = "Swoon",
        duration = 2000
      ),
      TestData.createPlayQueueEntry(
        id = 31,
        filePath = "/media/music/song3.mp3",
        mediaId = 22,
        artist = "Foals",
        artistId = 23,
        album = "Everything Not Saved Will Be Lost: Part 1",
        albumId = 24,
        title = "Exits",
        duration = 3000
      )
    )
    every { playQueueDao.getPlayQueue() } returns Single.just(playQueue)

    withSut {
      val expected = playQueue.map { it.toDescription() }

      verify {
        exoPlayerAdapter.loadQueue(match { actual ->
          expected.joinToString() == actual.joinToString()
        })
      }
    }
  }

  @Test
  fun `GIVEN there is a saved LastPlayed, WHEN the service is created, THEN load last played settings into Player`() {
    val lastPlayed = LastPlayed(
      queuePosition = 1,
      trackPosition = 1234,
      shuffleMode = PlaybackStateCompat.SHUFFLE_MODE_ALL,
      repeatMode = PlaybackStateCompat.REPEAT_MODE_ALL,
      shuffleSeed = 123456,
      queueTitle = "Last Played Songs"
    )
    every { playQueueDao.getLastPlayed() } returns Single.just(lastPlayed)

    withSut {
      verify {
        exoPlayerAdapter.loadLastPlayed(lastPlayed)
      }
    }
  }

  @Test
  fun `GIVEN there is no saved LastPlayed, WHEN the service is created, THEN load default settings`() {
    every { playQueueDao.getLastPlayed() } returns Single.error(NoSuchElementException())

    withSut {
      verify { exoPlayerAdapter.loadLastPlayed(LastPlayed()) }
    }
  }

  @Test
  fun `WHEN the service is destroyed, THEN release player and other resources`() {
    createSut()
    serviceController.destroy()

    verify {
      mediaSession.isActive = false
      mediaSession.release()

      exoPlayerAdapter.release()
    }
  }

  @Test
  fun `GIVEN the service is in foreground, WHEN playback stops, THEN remove service from foreground`() {
    withSut {
      makeServiceForeground()

      playerEventSubject.onNext(PlayerEvent.PlayerStateChanged(false, 0))

      assertFalse { isForeground }
    }
  }

  @Test
  fun `GIVEN the service is not in foreground AND player is playing, WHEN notification gets posted, THEN start the service foreground`() {
    every { exoPlayerAdapter.isPlaying() } returns true

    withSut {
      val event = NotificationEvent.NotificationPosted(123, mockk(), false)
      notificationEventSubject.onNext(event)

      assertTrue { isForeground }
    }
  }

  @Test
  fun `GIVEN the service is in foreground AND player is not playing, WHEN notification gets posted, THEN remove service from foreground`() {
    withSut {
      makeServiceForeground()

      every { exoPlayerAdapter.isPlaying() } returns false
      notificationEventSubject.onNext(NotificationEvent.NotificationPosted(123, mockk(), false))

      assertFalse { isForeground }
    }
  }

  @Test
  fun `WHEN notification is canceled, THEN stop service`() {
    withSut {
      makeServiceForeground()

      notificationEventSubject.onNext(NotificationEvent.NotificationCanceled(123, true))

      assertFalse { isForeground }
    }
  }

  private fun makeServiceForeground() {
    every { exoPlayerAdapter.isPlaying() } returns true
    notificationEventSubject.onNext(NotificationEvent.NotificationPosted(0, mockk(), false))
  }
}