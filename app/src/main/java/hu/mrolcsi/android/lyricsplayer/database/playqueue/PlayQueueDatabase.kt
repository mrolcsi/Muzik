package hu.mrolcsi.android.lyricsplayer.database.playqueue

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import hu.mrolcsi.android.lyricsplayer.database.playqueue.daos.PlayQueueDao
import hu.mrolcsi.android.lyricsplayer.database.playqueue.entities.LastPlayed
import hu.mrolcsi.android.lyricsplayer.database.playqueue.entities.PlayQueueEntry

@Database(
  entities = [PlayQueueEntry::class, LastPlayed::class],
  version = 2
)
abstract class PlayQueueDatabase : RoomDatabase() {

  abstract fun getPlayQueueDao(): PlayQueueDao

  companion object {
    @Suppress("unused")
    private const val LOG_TAG = "PlayQueueDatabase"

    private const val DATABASE_NAME = "playqueue.db"

    // For Singleton instantiation
    @Volatile private var instance: PlayQueueDatabase? = null

    fun getInstance(context: Context): PlayQueueDatabase {
      return instance ?: synchronized(this) {
        instance ?: buildDatabase(context).also { instance = it }
      }
    }

    private fun buildDatabase(context: Context): PlayQueueDatabase {
      return Room.databaseBuilder(context, PlayQueueDatabase::class.java, DATABASE_NAME)
        .addMigrations(
          PlayQueueMigrations.MIGRATION_1_2
        )
        .build()
    }
  }
}