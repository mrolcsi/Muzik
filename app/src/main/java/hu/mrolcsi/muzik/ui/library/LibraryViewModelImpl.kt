package hu.mrolcsi.muzik.ui.library

import android.Manifest
import android.content.Context
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.content.ContextCompat
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.data.manager.media.MediaBrowserClient
import hu.mrolcsi.muzik.data.repository.media.MediaRepository
import hu.mrolcsi.muzik.ui.albums.AlbumsFragment
import hu.mrolcsi.muzik.ui.artists.ArtistsFragment
import hu.mrolcsi.muzik.ui.base.DataBindingViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModelImpl
import hu.mrolcsi.muzik.ui.common.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.ui.common.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.ui.common.LiveEvent
import hu.mrolcsi.muzik.ui.common.ObservableImpl
import hu.mrolcsi.muzik.ui.common.Page
import hu.mrolcsi.muzik.ui.songs.SongsFragment
import io.reactivex.rxkotlin.subscribeBy
import org.koin.core.KoinComponent
import org.koin.core.inject

class LibraryViewModelImpl constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  ThemedViewModel by themedViewModel,
  LibraryViewModel,
  KoinComponent {

  private val context: Context by inject()
  private val mediaRepo: MediaRepository by inject()
  private val mediaService: MediaBrowserClient by inject()

  override val pages = MutableLiveData<List<Page>>()

  private var songDescriptions: List<MediaDescriptionCompat>? = null

  override fun onShuffleAllClicked() {
    songDescriptions?.let {
      mediaService.setQueueTitle(context.getString(R.string.playlist_allSongs))
      mediaService.playAllShuffled(it)
    }
  }

  override var isPermissionRationaleVisible: Boolean by boundBoolean(BR.permissionRationaleVisible)

  override val requestPermissionEvent = LiveEvent<Array<String>>()

  override fun requestPermission() {
    requestPermissionEvent.value = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
  }

  override fun onPermissionGranted() {
    mediaRepo.getSongs()
      .doOnNext { songs -> songDescriptions = songs.filter { it.isPlayable }.map { it.description } }
      .subscribeBy(
        onNext = {
          isPermissionRationaleVisible = false

          pages.value = listOf(
            Page(
              context.getString(R.string.artists_title),
              ContextCompat.getDrawable(context, R.drawable.ic_artist),
              ArtistsFragment()
            ),
            Page(
              context.getString(R.string.albums_title),
              ContextCompat.getDrawable(context, R.drawable.ic_album),
              AlbumsFragment()
            ),
            Page(
              context.getString(R.string.songs_title),
              ContextCompat.getDrawable(context, R.drawable.ic_song),
              SongsFragment()
            )
          )
        },
        onError = {
          requestPermission()
        }
      ).disposeOnCleared()
  }

  override fun onPermissionDenied(shouldShowPermissionRationale: Boolean) {
    isPermissionRationaleVisible = shouldShowPermissionRationale
  }

  init {
    onPermissionGranted()
  }
}