package hu.mrolcsi.muzik.library

import androidx.lifecycle.LiveData
import hu.mrolcsi.muzik.common.view.Page
import hu.mrolcsi.muzik.common.viewmodel.NavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.PermissionViewModel
import hu.mrolcsi.muzik.theme.ThemedViewModel

interface LibraryViewModel : PermissionViewModel, ThemedViewModel, NavCommandSource {

  val pages: LiveData<List<Page>>

  fun onShuffleAllClicked()

}