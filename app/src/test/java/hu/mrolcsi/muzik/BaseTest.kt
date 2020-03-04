package hu.mrolcsi.muzik

import android.os.Build
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphNavigator
import androidx.navigation.Navigation
import androidx.navigation.NavigatorProvider
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.plugins.RxJavaPlugins
import org.junit.Before
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.module.Module
import org.koin.test.AutoCloseKoinTest
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
abstract class BaseTest : AutoCloseKoinTest() {

  private val mockNavController = mockk<NavController>(relaxed = true)

  protected abstract val testModule: Module

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
  }

  protected fun <T : Fragment> T.useMockNavController() = apply {
    viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
      viewLifecycleOwner?.let {
        Navigation.setViewNavController(requireView(), mockNavController)
      }
    }
  }

}