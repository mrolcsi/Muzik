package hu.mrolcsi.muzik.library.pager

import android.content.Context
import android.support.v4.media.MediaDescriptionCompat
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.viewmodel.DataBindingViewModel
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ObservableImpl
import hu.mrolcsi.muzik.media.MediaRepository
import hu.mrolcsi.muzik.media.MediaService
import hu.mrolcsi.muzik.theme.ThemedViewModel
import hu.mrolcsi.muzik.theme.ThemedViewModelImpl
import javax.inject.Inject

class LibraryPagerViewModelImpl @Inject constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl,
  mediaRepo: MediaRepository,
  private val context: Context,
  private val mediaService: MediaService
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  ThemedViewModel by themedViewModel,
  LibraryPagerViewModel {

  private var songDescriptions: List<MediaDescriptionCompat>? = null

  override fun onShuffleAllClick() {
    songDescriptions?.let {
      mediaService.setQueueTitle(context.getString(R.string.playlist_allSongs))
      mediaService.playAllShuffled(it)
    }
  }

  init {
    mediaRepo.getSongs()
      .map { songs -> songDescriptions = songs.filter { it.isPlayable }.map { it.description } }
      .subscribe()
      .disposeOnCleared()
  }

}