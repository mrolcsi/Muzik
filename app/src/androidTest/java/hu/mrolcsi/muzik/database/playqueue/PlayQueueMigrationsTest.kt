package hu.mrolcsi.muzik.database.playqueue

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.provider.MediaStore
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import hu.mrolcsi.muzik.database.playqueue.PlayQueueMigrations.MIGRATION_1_2
import hu.mrolcsi.muzik.database.playqueue.PlayQueueMigrations.MIGRATION_2_3
import hu.mrolcsi.muzik.database.playqueue.entities.LastPlayed
import hu.mrolcsi.muzik.database.playqueue.entities.PlayQueueEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test migrations of the PlayQueue database.
 */
@RunWith(AndroidJUnit4::class)
class PlayQueueMigrationsTest {

  @get:Rule
  val mTestHelper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    PlayQueueDatabase::class.java.canonicalName,
    FrameworkSQLiteOpenHelperFactory()
  )

  @Test
  fun migration1To2_containsCorrectData() {

    // Create the database in version 1
    val db = mTestHelper.createDatabase(TEST_DB_NAME, 1)

    // Insert data
    db.insert("last_played", SQLiteDatabase.CONFLICT_REPLACE, ContentValues().apply {
      put("queue_position", TEST_LAST_PLAYED.queuePosition)
      put("track_position", TEST_LAST_PLAYED.trackPosition)
      put("shuffle_mode", TEST_LAST_PLAYED.shuffleMode)
      put("repeat_mode", TEST_LAST_PLAYED.repeatMode)
    })

    // Prepare for next version
    db.close()

    // Create and validate database with new schema.
    mTestHelper.runMigrationsAndValidate(
      TEST_DB_NAME, 2, true,
      MIGRATION_1_2
    )

    // Get data from db
    val dbLastPlayed = getMigratedRoomDatabase()
      .getPlayQueueDao()
      .getLastPlayed()

    // Check if data is still there
    assertNotNull(dbLastPlayed)

    // Check existing data
    assertEquals(TEST_LAST_PLAYED.queuePosition, dbLastPlayed?.queuePosition)
    assertEquals(TEST_LAST_PLAYED.trackPosition, dbLastPlayed?.trackPosition)
    assertEquals(TEST_LAST_PLAYED.shuffleMode, dbLastPlayed?.shuffleMode)
    assertEquals(TEST_LAST_PLAYED.repeatMode, dbLastPlayed?.repeatMode)

    // Check new column
    assertEquals(0L, dbLastPlayed?.shuffleSeed)
  }

  @Test
  fun migration2To3_containsCorrectData() {
    // Create the database in version 1
    val db = mTestHelper.createDatabase(TEST_DB_NAME, 2)

    // Insert data
    db.insert("play_queue", SQLiteDatabase.CONFLICT_REPLACE, ContentValues().apply {
      put(MediaStore.Audio.Media._ID, TEST_QUEUE_ENTRY._id)
      put(MediaStore.Audio.Media.DATA, TEST_QUEUE_ENTRY._data)
      put(MediaStore.Audio.Media.ARTIST, TEST_QUEUE_ENTRY.artist)
      put(MediaStore.Audio.Media.ALBUM, TEST_QUEUE_ENTRY.album)
      put(MediaStore.Audio.Media.TITLE, TEST_QUEUE_ENTRY.title)
      put(MediaStore.Audio.Media.DURATION, TEST_QUEUE_ENTRY.duration)
    })

    // Prepare for next version
    db.close()

    // Create and validate database with new schema
    mTestHelper.runMigrationsAndValidate(
      TEST_DB_NAME, 3, true,
      MIGRATION_2_3
    )

    // Get data from db
    val dbEntry = getMigratedRoomDatabase()
      .getPlayQueueDao()
      .getQueue()
      .first()

    // Check if data is still there
    assertNotNull(dbEntry)

    // Check existing columns
    assertEquals(TEST_QUEUE_ENTRY._id, dbEntry._id)
    assertEquals(TEST_QUEUE_ENTRY._data, dbEntry._data)
    assertEquals(TEST_QUEUE_ENTRY.artist, dbEntry.artist)
    assertEquals(TEST_QUEUE_ENTRY.album, dbEntry.album)
    assertEquals(TEST_QUEUE_ENTRY.title, dbEntry.title)
    assertEquals(TEST_QUEUE_ENTRY.duration, dbEntry.duration)

    // Check new column
    assertEquals(0L, dbEntry.mediaId)
  }

  // Helpers

  private fun getMigratedRoomDatabase(): PlayQueueDatabase {
    val database = Room.databaseBuilder(
      InstrumentationRegistry.getInstrumentation().targetContext,
      PlayQueueDatabase::class.java, TEST_DB_NAME
    ).addMigrations(
      MIGRATION_1_2,
      MIGRATION_2_3
    ).build()

    // close the database and release any stream resources when the test finishes
    mTestHelper.closeWhenFinished(database)
    return database
  }

  companion object {
    private const val TEST_DB_NAME = "playqueue-test.db"

    private val TEST_LAST_PLAYED = LastPlayed().apply {
      queuePosition = 15
      trackPosition = 136984
      shuffleMode = 1
      repeatMode = 0
    }

    private val TEST_QUEUE_ENTRY = PlayQueueEntry(
      0,
      "/music/path/song1.mp3",
      12234,
      "My Favourite Artist",
      "My Favourite Album",
      "My Favourite Song",
      456789
    )
  }
}