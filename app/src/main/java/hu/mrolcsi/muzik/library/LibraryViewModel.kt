package hu.mrolcsi.muzik.library

import hu.mrolcsi.muzik.common.viewmodel.NavCommandSource
import hu.mrolcsi.muzik.extensions.ParcelableNavDirections
import hu.mrolcsi.muzik.theme.ThemedViewModel

interface LibraryViewModel : ThemedViewModel, NavCommandSource {

  var navDirection: ParcelableNavDirections?

}