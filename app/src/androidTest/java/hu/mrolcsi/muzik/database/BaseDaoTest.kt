package hu.mrolcsi.muzik.database

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import hu.mrolcsi.muzik.data.local.MuzikDatabase
import io.reactivex.observers.TestObserver
import org.junit.After
import org.junit.Before
import org.junit.Rule
import java.io.IOException

abstract class BaseDaoTest {

  @get:Rule
  val instantTaskExecutorRule = InstantTaskExecutorRule()

  protected lateinit var db: MuzikDatabase

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(
      context, MuzikDatabase::class.java
    ).build()
  }

  @After
  @Throws(IOException::class)
  fun closeDb() {
    db.close()
  }

  fun <T> TestObserver<T>.awaitAndAssertValuesOnly(vararg values: T): TestObserver<T> =
    awaitCount(values.size).assertValuesOnly(*values)
}