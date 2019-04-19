package hu.mrolcsi.muzik.database.playqueue.daos

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jraska.livedata.test
import hu.mrolcsi.muzik.database.playqueue.PlayQueueDatabase
import hu.mrolcsi.muzik.database.playqueue.entities.LastPlayed
import hu.mrolcsi.muzik.database.playqueue.entities.PlayQueueEntry
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayQueueDaoTest {

  @get:Rule
  val instantTaskExecutorRule = InstantTaskExecutorRule()

  private lateinit var db: PlayQueueDatabase

  @Before
  fun setUp() {
    // Create temporary database
    db = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext<Context>(),
      PlayQueueDatabase::class.java
    ).build()
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun playQueue_test() {

    // Insert test data into db
    db.getPlayQueueDao().insertEntries(*TEST_QUEUE_ENTRIES)
    db.getPlayQueueDao().getQueue().forEachIndexed { index, entry ->
      assertThat(entry, equalTo(TEST_QUEUE_ENTRIES[index]))
    }
    db.getPlayQueueDao().fetchQueue().test().awaitValue().value().forEachIndexed { index, entry ->
      assertThat(entry, equalTo(TEST_QUEUE_ENTRIES[index]))
    }

    // Insert some items again (result should stay the same)
    db.getPlayQueueDao().insertEntries(
      TEST_QUEUE_ENTRIES[0],
      TEST_QUEUE_ENTRIES[2],
      TEST_QUEUE_ENTRIES[3]
    )
    db.getPlayQueueDao().getQueue().forEachIndexed { index, entry ->
      assertThat(entry, equalTo(TEST_QUEUE_ENTRIES[index]))
    }
    db.getPlayQueueDao().fetchQueue().test().awaitValue().value().forEachIndexed { index, entry ->
      assertThat(entry, equalTo(TEST_QUEUE_ENTRIES[index]))
    }

    // Remove single item (id = 0)
    db.getPlayQueueDao().removeEntries(TEST_QUEUE_ENTRIES[0])
    db.getPlayQueueDao().getQueue().forEach {
      assertThat(it._id, not(0L))
    }
    db.getPlayQueueDao().fetchQueue().test().awaitValue().value().forEach {
      assertThat(it._id, not(0L))
    }

    // Remove item at position
    db.getPlayQueueDao().removeEntry(1)
    db.getPlayQueueDao().getQueue().forEach {
      assertThat(it._id, not(0L))
      assertThat(it._id, not(1L))
    }
    db.getPlayQueueDao().fetchQueue().test().awaitValue().value().forEach {
      assertThat(it._id, not(0L))
      assertThat(it._id, not(1L))
    }

    // Remove items in range
    db.getPlayQueueDao().removeEntriesInRange(2, 4)
    db.getPlayQueueDao().getQueue().forEach {
      assertThat(it._id, not(0L))
      assertThat(it._id, not(1L))
      assertThat(it._id, not(2L))
      assertThat(it._id, not(3L))
      assertThat(it._id, not(4L))
    }
    db.getPlayQueueDao().fetchQueue().test().awaitValue().value().forEach {
      assertThat(it._id, not(0L))
      assertThat(it._id, not(1L))
      assertThat(it._id, not(2L))
      assertThat(it._id, not(3L))
      assertThat(it._id, not(4L))
    }

    // Clear items
    db.getPlayQueueDao().clearQueue()
    db.getPlayQueueDao().getQueue().forEach {
      assertThat(it._id, not(0L))
      assertThat(it._id, not(1L))
      assertThat(it._id, not(2L))
      assertThat(it._id, not(3L))
      assertThat(it._id, not(4L))
      assertThat(it._id, not(5L))
    }
    db.getPlayQueueDao().fetchQueue().test().awaitValue().value().forEach {
      assertThat(it._id, not(0L))
      assertThat(it._id, not(1L))
      assertThat(it._id, not(2L))
      assertThat(it._id, not(3L))
      assertThat(it._id, not(4L))
      assertThat(it._id, not(5L))
    }
  }

  @Test
  fun lastPlayed_test() {
    // Insert first item
    db.getPlayQueueDao().saveLastPlayed(TEST_LAST_PLAYED_1)
    assertThat(db.getPlayQueueDao().getLastPlayed(), equalTo(TEST_LAST_PLAYED_1))
    assertThat(db.getPlayQueueDao().fetchLastPlayed().test().awaitValue().value(), equalTo(TEST_LAST_PLAYED_1))

    // Insert second item (should overwrite previous item)
    db.getPlayQueueDao().saveLastPlayed(TEST_LAST_PLAYED_2)
    assertThat(db.getPlayQueueDao().getLastPlayed(), equalTo(TEST_LAST_PLAYED_2))
    assertThat(db.getPlayQueueDao().fetchLastPlayed().test().awaitValue().value(), equalTo(TEST_LAST_PLAYED_2))
  }

  companion object {

    private val TEST_QUEUE_ENTRIES = arrayOf(
      PlayQueueEntry(
        0,
        "/music/song1.mp3",
        "My Favourite Artist",
        "My Favourite Album",
        "My Favourite Song",
        123456
      ),
      PlayQueueEntry(
        1,
        "/music/song2.mp3",
        "Your Favourite Artist",
        "Your Favourite Album",
        "Your Favourite Song",
        234567
      ),
      PlayQueueEntry(
        2,
        "/music/song3.mp3",
        "His Favourite Artist",
        "His Favourite Album",
        "His Favourite Song",
        345678
      ),
      PlayQueueEntry(
        3,
        "/music/song4.mp3",
        "Her Favourite Artist",
        "Her Favourite Album",
        "Her Favourite Song",
        456789
      ),
      PlayQueueEntry(
        4,
        "/music/song5.mp3",
        "Old Favourite Artist",
        "Old Favourite Album",
        "Old Favourite Song",
        567890
      ),
      PlayQueueEntry(
        5,
        "/music/song6.mp3",
        "New Favourite Artist",
        "New Favourite Album",
        "New Favourite Song",
        678901
      )
    )

    private val TEST_LAST_PLAYED_1 = LastPlayed().apply {
      queuePosition = 12
      trackPosition = 123456
      shuffleMode = 0
      repeatMode = 1
      shuffleSeed = 234567
    }

    private val TEST_LAST_PLAYED_2 = LastPlayed().apply {
      queuePosition = 34
      trackPosition = 345678
      shuffleMode = 1
      repeatMode = 0
      shuffleSeed = 456789
    }
  }
}