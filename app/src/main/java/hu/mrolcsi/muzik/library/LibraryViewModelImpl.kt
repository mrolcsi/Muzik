package hu.mrolcsi.muzik.library

import android.content.Context
import android.support.v4.media.MediaDescriptionCompat
import androidx.lifecycle.MutableLiveData
import com.tbruyelle.rxpermissions2.RxPermissions
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.albums.AlbumsFragment
import hu.mrolcsi.muzik.artists.ArtistsFragment
import hu.mrolcsi.muzik.common.view.Page
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ObservableImpl
import hu.mrolcsi.muzik.common.viewmodel.RxPermissionViewModel
import hu.mrolcsi.muzik.di.REQUIRED_PERMISSIONS
import hu.mrolcsi.muzik.media.MediaRepository
import hu.mrolcsi.muzik.media.MediaService
import hu.mrolcsi.muzik.songs.SongsFragment
import hu.mrolcsi.muzik.theme.ThemedViewModel
import hu.mrolcsi.muzik.theme.ThemedViewModelImpl
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
  private val mediaService: MediaService by inject()

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