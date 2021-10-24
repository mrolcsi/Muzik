package hu.mrolcsi.muzik.database.playqueue.daos

import hu.mrolcsi.muzik.data.local.playQueue.PlayQueueDao
import hu.mrolcsi.muzik.data.model.playQueue.LastPlayed
import hu.mrolcsi.muzik.data.model.playQueue.PlayQueueEntry
import hu.mrolcsi.muzik.database.BaseDaoTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayQueueDaoTest : BaseDaoTest() {

  private fun withSut(action: PlayQueueDao.() -> Unit) = db.getPlayQueueDao().apply(action)

  @Test
  fun playQueue_test() {
    withSut {
      val testObserver = observeQueue().test()
      testObserver.awaitAndAssertValuesOnly(emptyList())

      // Insert test data into db
      insertEntries(*TEST_QUEUE_ENTRIES.toTypedArray())
      assertEquals(TEST_QUEUE_ENTRIES, getQueue())
      testObserver.awaitAndAssertValuesOnly(
        emptyList(),
        TEST_QUEUE_ENTRIES
      )

      // Insert some items again (result should stay the same)
      insertEntries(
        TEST_QUEUE_ENTRIES[0],
        TEST_QUEUE_ENTRIES[2],
        TEST_QUEUE_ENTRIES[3]
      )
      assertEquals(TEST_QUEUE_ENTRIES, getQueue())
      testObserver.awaitAndAssertValuesOnly(
        emptyList(),
        TEST_QUEUE_ENTRIES,
        TEST_QUEUE_ENTRIES
      )

      // Remove single item (id = 0)
      removeEntries(TEST_QUEUE_ENTRIES[0])
      val expectedMinus0 = TEST_QUEUE_ENTRIES - listOf(TEST_QUEUE_ENTRIES[0])
      assertEquals(expectedMinus0, getQueue())
      testObserver.awaitAndAssertValuesOnly(
        emptyList(),
        TEST_QUEUE_ENTRIES,
        TEST_QUEUE_ENTRIES,
        expectedMinus0
      )

      // Remove item at position
      removeEntry(1)
      val expectedMinus1 = expectedMinus0 - listOf(TEST_QUEUE_ENTRIES[1])
      assertEquals(expectedMinus1, getQueue())
      testObserver.awaitAndAssertValuesOnly(
        emptyList(),
        TEST_QUEUE_ENTRIES,
        TEST_QUEUE_ENTRIES,
        expectedMinus0,
        expectedMinus1
      )

      // Remove items in range
      removeEntriesInRange(2, 4)
      val expectedMinus2To4 = expectedMinus1 - listOf(
        TEST_QUEUE_ENTRIES[2],
        TEST_QUEUE_ENTRIES[3],
        TEST_QUEUE_ENTRIES[4]
      )
      assertEquals(expectedMinus2To4, getQueue())
      testObserver.awaitAndAssertValuesOnly(
        emptyList(),
        TEST_QUEUE_ENTRIES,
        TEST_QUEUE_ENTRIES,
        expectedMinus0,
        expectedMinus1,
        expectedMinus2To4
      )

      // Clear items
      db.getPlayQueueDao().clearQueue()
      assertEquals(emptyList<PlayQueueEntry>(), getQueue())
      testObserver.awaitAndAssertValuesOnly(
        emptyList(),
        TEST_QUEUE_ENTRIES,
        TEST_QUEUE_ENTRIES,
        expectedMinus0,
        expectedMinus1,
        expectedMinus2To4,
        emptyList()
      )
    }
  }

  @Test
  fun lastPlayed_test() {
    withSut {
      // Insert first item
      saveLastPlayed(TEST_LAST_PLAYED_1)
      assertEquals(TEST_LAST_PLAYED_1, getLastPlayed())

      // Insert second item (should overwrite previous item)
      saveLastPlayed(TEST_LAST_PLAYED_2)
      assertEquals(TEST_LAST_PLAYED_2, getLastPlayed())
    }
  }

  companion object {

    private val TEST_QUEUE_ENTRIES = listOf(
      PlayQueueEntry(
        0,
        "/music/song1.mp3",
        123,
        "My Favourite Artist",
        159,
        "My Favourite Album",
        974,
        "My Favourite Song",
        123456
      ),
      PlayQueueEntry(
        1,
        "/music/song2.mp3",
        234,
        "Your Favourite Artist",
        741,
        "Your Favourite Album",
        956,
        "Your Favourite Song",
        234567
      ),
      PlayQueueEntry(
        2,
        "/music/song3.mp3",
        345,
        "His Favourite Artist",
        987,
        "His Favourite Album",
        743,
        "His Favourite Song",
        345678
      ),
      PlayQueueEntry(
        3,
        "/music/song4.mp3",
        456,
        "Her Favourite Artist",
        931,
        "Her Favourite Album",
        912,
        "Her Favourite Song",
        456789
      ),
      PlayQueueEntry(
        4,
        "/music/song5.mp3",
        567,
        "Old Favourite Artist",
        741,
        "Old Favourite Album",
        458,
        "Old Favourite Song",
        567890
      ),
      PlayQueueEntry(
        5,
        "/music/song6.mp3",
        678,
        "New Favourite Artist",
        122,
        "New Favourite Album",
        631,
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