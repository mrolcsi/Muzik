package hu.mrolcsi.muzik.ui.library

import androidx.lifecycle.LiveData
import hu.mrolcsi.muzik.ui.common.Page
import hu.mrolcsi.muzik.ui.common.NavCommandSource
import hu.mrolcsi.muzik.ui.base.PermissionViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModel

interface LibraryViewModel : PermissionViewModel, ThemedViewModel, NavCommandSource {

  val pages: LiveData<List<Page>>

  fun onShuffleAllClicked()

}