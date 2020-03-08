package hu.mrolcsi.muzik.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import hu.mrolcsi.muzik.data.local.playQueue.PlayQueueDao
import hu.mrolcsi.muzik.data.local.playQueue.PlayQueueDao2Impl
import hu.mrolcsi.muzik.data.model.playQueue.LastPlayed
import hu.mrolcsi.muzik.data.model.playQueue.PlayQueueEntry

@Database(
  entities = [PlayQueueEntry::class, LastPlayed::class],
  version = 5
)
abstract class MuzikDatabase : RoomDatabase() {

  @Deprecated("Use PlayQueueDao2!")
  abstract fun getPlayQueueDao(): PlayQueueDao
  abstract fun getPlayQueueDao2(): PlayQueueDao2Impl

  companion object {
    @Suppress("unused")
    private const val LOG_TAG = "MuzikDatabase"

    const val DATABASE_NAME = "playqueue.db"

    // For Singleton instantiation
    @Volatile private var instance: MuzikDatabase? = null

    @Deprecated("Use injection!")
    fun getInstance(context: Context): MuzikDatabase {
      return instance ?: synchronized(this) {
        instance
          ?: buildDatabase(context).also { instance = it }
      }
    }

    private fun buildDatabase(context: Context): MuzikDatabase {
      return Room.databaseBuilder(
          context, MuzikDatabase::class.java,
          DATABASE_NAME
        )
        .addMigrations(
          MuzikDatabaseMigrations.MIGRATION_1_2,
          MuzikDatabaseMigrations.MIGRATION_2_3,
          MuzikDatabaseMigrations.MIGRATION_3_4,
          MuzikDatabaseMigrations.MIGRATION_2_5,
          MuzikDatabaseMigrations.MIGRATION_4_5
        ).build()
    }
  }
}