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
import org.koin.core.KoinComponent
import org.koin.core.inject

class LibraryPagerViewModelImpl constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  ThemedViewModel by themedViewModel,
  LibraryPagerViewModel,
  KoinComponent {

  private val context: Context by inject()
  private val mediaRepo: MediaRepository by inject()
  private val mediaService: MediaService by inject()

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