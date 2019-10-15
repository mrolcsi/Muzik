package hu.mrolcsi.muzik.database.playqueue

import android.provider.MediaStore
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object PlayQueueMigrations {

  val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
      // Added 'shuffleSeed' column to LastPlayed
      with(database) {

        execSQL("ALTER TABLE last_played ADD COLUMN shuffle_seed INTEGER NOT NULL DEFAULT 0")

      }
    }
  }

  val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
      // Added 'media_id' column to PlayQueueEntry
      database.execSQL("ALTER TABLE play_queue ADD COLUMN media_id INTEGER NOT NULL DEFAULT 0")
    }
  }

  val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
      with(database) {
        execSQL("ALTER TABLE play_queue ADD COLUMN ${MediaStore.Audio.Media.ARTIST_ID} INTEGER NOT NULL DEFAULT 0")
        execSQL("ALTER TABLE play_queue ADD COLUMN ${MediaStore.Audio.Media.ALBUM_ID} INTEGER NOT NULL DEFAULT 0")
      }
    }
  }

  val MIGRATION_2_5 = object : Migration(2, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
      database.execSQL("ALTER TABLE last_played ADD COLUMN queue_title TEXT NOT NULL DEFAULT ''")
    }
  }

  val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
      database.execSQL("ALTER TABLE last_played ADD COLUMN queue_title TEXT NOT NULL DEFAULT ''")
    }
  }
}