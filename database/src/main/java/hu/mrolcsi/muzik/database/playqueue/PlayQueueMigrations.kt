package hu.mrolcsi.muzik.database.playqueue

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object PlayQueueMigrations {

  var MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
      // Added 'shuffleSeed' column to LastPlayed
      with(database) {

        execSQL("ALTER TABLE last_played ADD COLUMN shuffle_seed INTEGER NOT NULL DEFAULT 0")

      }
    }
  }

  var MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
      // Added 'media_id' column to PlayQueueEntry
      database.execSQL("ALTER TABLE play_queue ADD COLUMN media_id INTEGER NOT NULL DEFAULT 0")
    }

  }
}