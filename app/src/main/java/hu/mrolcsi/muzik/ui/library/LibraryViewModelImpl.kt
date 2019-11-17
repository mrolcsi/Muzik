package hu.mrolcsi.muzik.ui.library

import android.content.Context
import android.support.v4.media.MediaDescriptionCompat
import androidx.lifecycle.MutableLiveData
import com.tbruyelle.rxpermissions2.RxPermissions
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.ui.albums.AlbumsFragment
import hu.mrolcsi.muzik.ui.artists.ArtistsFragment
import hu.mrolcsi.muzik.ui.common.Page
import hu.mrolcsi.muzik.ui.common.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.ui.common.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.ui.common.ObservableImpl
import hu.mrolcsi.muzik.ui.base.RxPermissionViewModel
import hu.mrolcsi.muzik.injection.REQUIRED_PERMISSIONS
import hu.mrolcsi.muzik.data.repository.media.MediaRepository
import hu.mrolcsi.muzik.data.manager.media.MediaManager
import hu.mrolcsi.muzik.ui.songs.SongsFragment
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModelImpl
import io.reactivex.rxkotlin.subscribeBy
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.qualifier.named

class LibraryViewModelImpl constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl,
  rxPermissions: RxPermissions
) : RxPermissionViewModel(observable, uiCommandSource, navCommandSource, rxPermissions),
  ThemedViewModel by themedViewModel,
  LibraryViewModel,
  KoinComponent {

  private val context: Context by inject()
  private val mediaRepo: MediaRepository by inject()
  private val mediaService: MediaManager by inject()

  private val requiredPermissions: Array<String> by inject(qualifier = named(REQUIRED_PERMISSIONS))

  override val pages = MutableLiveData<List<Page>>()

  private var songDescriptions: List<MediaDescriptionCompat>? = null

  override fun onShuffleAllClicked() {
    songDescriptions?.let {
      mediaService.setQueueTitle(context.getString(R.string.playlist_allSongs))
      mediaService.playAllShuffled(it)
    }
  }

  init {
    requirePermissions(requiredPermissions) {
      mediaRepo.getSongs()
        .doOnNext { songs -> songDescriptions = songs.filter { it.isPlayable }.map { it.description } }
    }.subscribeBy(
      onNext = {
        pages.value = listOf(
          Page(context.getString(R.string.artists_title), ArtistsFragment()),
          Page(context.getString(R.string.albums_title), AlbumsFragment()),
          Page(context.getString(R.string.songs_title), SongsFragment())
        )
      },
      onError = { showError(this, it) }
    ).disposeOnCleared()
  }
}