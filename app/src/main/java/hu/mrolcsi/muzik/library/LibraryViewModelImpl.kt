package hu.mrolcsi.muzik.library

import androidx.navigation.navOptions
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.NavCommandSource
import hu.mrolcsi.muzik.extensions.ParcelableNavDirections
import hu.mrolcsi.muzik.theme.ThemeService
import hu.mrolcsi.muzik.theme.ThemedViewModelImpl
import javax.inject.Inject
import kotlin.properties.Delegates

class LibraryViewModelImpl @Inject constructor(
  navCommandSource: ExecuteOnceNavCommandSource,
  themeService: ThemeService
) : ThemedViewModelImpl(themeService),
  NavCommandSource by navCommandSource,
  LibraryViewModel {

  private var navigationHandled = false

  override var navDirection: ParcelableNavDirections? by Delegates.observable(null) { _, old: ParcelableNavDirections?, new: ParcelableNavDirections? ->
    if (old != new && new != null) {
      navigationHandled = false
      sendNavCommand {
        navigate(
          new.actionId,
          new.arguments,
          navOptions {
            popUpTo(R.id.library_navigation) {
              inclusive = true
            }
            launchSingleTop = true
          }
        )
        navigationHandled = true
      }
    }
  }
}