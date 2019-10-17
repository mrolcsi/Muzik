package hu.mrolcsi.muzik

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
import org.junit.Before
import org.junit.runner.RunWith
import org.koin.test.AutoCloseKoinTest
import org.robolectric.RobolectricTestRunner
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
open class BaseTest : AutoCloseKoinTest() {

  private val mockNavController = mockk<NavController>(relaxed = true)

  @Before
  fun baseSetUp() {
    MockKAnnotations.init(this)

    val stubGraph = NavGraph(NavGraphNavigator(NavigatorProvider())).apply {
      val stubDestination = NavDestination("StubDestination").apply {
        id = Random.nextInt()
      }
      addDestination(stubDestination)
      startDestination = stubDestination.id
    }
    every { mockNavController.graph } returns stubGraph
  }

  fun <T : Fragment> T.useMockNavController() = apply {
    viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
      viewLifecycleOwner?.let {
        Navigation.setViewNavController(requireView(), mockNavController)
      }
    }
  }

}