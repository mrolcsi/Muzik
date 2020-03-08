package hu.mrolcsi.muzik.base

import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphNavigator
import androidx.navigation.Navigation
import androidx.navigation.NavigatorProvider
import hu.mrolcsi.muzik.R
import io.mockk.every
import io.mockk.mockk
import kotlin.random.Random

abstract class BaseFragmentTest<SUT : Fragment> : BaseTest<SUT>() {

  private val mockNavController = mockk<NavController>(relaxed = true)

  @Suppress("UNCHECKED_CAST")
  override fun withSut(action: SUT.() -> Unit) {
    launchFragmentInContainer<Fragment>(themeResId = R.style.FluxTheme) {
      createSut().useMockNavController()
    }.onFragment {
      (it as SUT).apply(action)
    }
  }

  override fun baseSetUp() {
    super.baseSetUp()
    prepareMockNavController()
  }

  private fun prepareMockNavController() {
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