package hu.mrolcsi.muzik

import android.content.Context
import android.os.Build
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphNavigator
import androidx.navigation.Navigation
import androidx.navigation.NavigatorProvider
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.plugins.RxJavaPlugins
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.module.Module
import org.koin.test.AutoCloseKoinTest
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import timber.log.Timber
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
abstract class BaseTest : AutoCloseKoinTest() {

  private val mockNavController = mockk<NavController>(relaxed = true)

  protected abstract val testModule: Module

  private val printlnTree = object : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
      println("[$priority] ${tag?.let { "$it: " } ?: ""}$message")
      t?.let { println(it) }
    }
  }

  @Before
  fun baseSetUp() {
    RxJavaPlugins.setIoSchedulerHandler { AndroidSchedulers.mainThread() }
    RxJavaPlugins.setComputationSchedulerHandler { AndroidSchedulers.mainThread() }

    MockKAnnotations.init(this)
    loadKoinModules(testModule)

    val stubGraph = NavGraph(NavGraphNavigator(NavigatorProvider())).apply {
      val stubDestination = NavDestination("StubDestination").apply {
        id = Random.nextInt()
      }
      addDestination(stubDestination)
      startDestination = stubDestination.id
    }
    every { mockNavController.graph } returns stubGraph

    Timber.plant(printlnTree)
  }

  @After
  fun baseTearDown() {
    Timber.uproot(printlnTree)
  }

  protected fun <T : Fragment> T.useMockNavController() = apply {
    viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
      viewLifecycleOwner?.let {
        Navigation.setViewNavController(requireView(), mockNavController)
      }
    }
  }

  protected fun <T : Context> context(): T = ApplicationProvider.getApplicationContext<T>()
}