package hu.mrolcsi.muzik.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import hu.mrolcsi.muzik.database.playqueue.PlayQueueDatabase
import hu.mrolcsi.muzik.database.playqueue.PlayQueueMigrations
import hu.mrolcsi.muzik.database.playqueue.daos.PlayQueueDao
import javax.inject.Singleton

@Module
class DatabaseModule {

  @Provides
  @Singleton
  fun provideAppDatabase(context: Context) =
    Room.databaseBuilder(context, PlayQueueDatabase::class.java, PlayQueueDatabase.DATABASE_NAME)
      .addMigrations(
        PlayQueueMigrations.MIGRATION_1_2,
        PlayQueueMigrations.MIGRATION_2_3,
        PlayQueueMigrations.MIGRATION_3_4,
        PlayQueueMigrations.MIGRATION_2_5,
        PlayQueueMigrations.MIGRATION_4_5
      ).build()

  @Provides
  @Singleton
  fun providePlayQueueDao(db: PlayQueueDatabase): PlayQueueDao = db.getPlayQueueDao()

}