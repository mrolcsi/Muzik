package hu.mrolcsi.muzik.base

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.plugins.RxJavaPlugins
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.module.Module
import org.koin.test.AutoCloseKoinTest
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import timber.log.Timber

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
abstract class BaseTest<SUT> : AutoCloseKoinTest() {

  protected abstract val testModule: Module

  protected abstract fun createSut(): SUT

  protected open fun withSut(action: SUT.() -> Unit) {
    createSut().apply(action)
  }

  @Before
  open fun baseSetUp() {
    RxJavaPlugins.setIoSchedulerHandler { AndroidSchedulers.mainThread() }
    RxJavaPlugins.setComputationSchedulerHandler { AndroidSchedulers.mainThread() }

    MockKAnnotations.init(this)
    loadKoinModules(testModule)
  }

  protected fun <T : Context> context(): T = ApplicationProvider.getApplicationContext()

  companion object {
    private val printlnTree = object : Timber.Tree() {
      override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        println("[$priority] ${tag?.let { "$it: " } ?: ""}$message")
        t?.let { println(it) }
      }
    }

    @JvmStatic
    @BeforeClass
    fun classSetUp() {
      Timber.plant(printlnTree)
    }

    @JvmStatic
    @AfterClass
    fun classTearDown() {
      Timber.uproot(printlnTree)
    }
  }
}