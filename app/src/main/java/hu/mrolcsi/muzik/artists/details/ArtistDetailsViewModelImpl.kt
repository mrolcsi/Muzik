package hu.mrolcsi.muzik.artists.details

import android.net.Uri
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.view.View
import androidx.core.view.ViewCompat
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.FragmentNavigatorExtras
import com.google.android.exoplayer2.util.Log
import hu.mrolcsi.muzik.common.viewmodel.DataBindingViewModel
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ObservableImpl
import hu.mrolcsi.muzik.discogs.DiscogsService
import hu.mrolcsi.muzik.media.MediaRepository
import hu.mrolcsi.muzik.media.MediaService
import hu.mrolcsi.muzik.service.extensions.media.MediaType
import hu.mrolcsi.muzik.service.extensions.media.artist
import hu.mrolcsi.muzik.service.extensions.media.id
import hu.mrolcsi.muzik.service.extensions.media.mediaId
import hu.mrolcsi.muzik.service.extensions.media.titleKey
import hu.mrolcsi.muzik.service.extensions.media.type
import hu.mrolcsi.muzik.songs.applyNowPlaying
import hu.mrolcsi.muzik.theme.ThemedViewModel
import hu.mrolcsi.muzik.theme.ThemedViewModelImpl
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import org.koin.core.KoinComponent
import org.koin.core.inject

class ArtistDetailsViewModelImpl constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  ThemedViewModel by themedViewModel,
  ArtistDetailsViewModel,
  KoinComponent {

  private val mediaRepo: MediaRepository by inject()
  private val discogsService: DiscogsService by inject()
  private val mediaService: MediaService by inject()

  private val artistSubject = BehaviorSubject.create<Long>()

  override val artistSongs = MutableLiveData<List<MediaItem>>()

  override val artistAlbums = MutableLiveData<List<MediaItem>>()

  override var artistName: String? by boundStringOrNull(BR.artistName)

  override val artistPicture = MutableLiveData<Uri>()

  override var isAlbumsVisible: Boolean by boundBoolean(BR.albumsVisible, false)

  private var songDescriptions: List<MediaDescriptionCompat>? = null

  override fun setArgument(artistId: Long) {
    artistSubject.onNext(artistId)
  }

  override fun onAlbumClick(albumItem: MediaItem, transitionedView: View) {
    if (albumItem.description.type == MediaType.MEDIA_ALBUM) {
      val transitionName = ViewCompat.getTransitionName(transitionedView)!!
      sendNavCommand {
        navigate(
          ArtistDetailsFragmentDirections.actionToAlbumDetails(albumItem.description.id, transitionName),
          FragmentNavigatorExtras(transitionedView to transitionName)
        )
      }
    }
  }

  override fun onSongClick(songItem: MediaItem, position: Int) {
    artistName?.let { mediaService.setQueueTitle(it) }
    songDescriptions?.let { mediaService.playAll(it, position) }
  }

  override fun onShuffleAllClick() {
    artistName?.let { mediaService.setQueueTitle(it) }
    songDescriptions?.let { mediaService.playAllShuffled(it) }
  }

  init {
    // Get artist item and url
    artistSubject
      .switchMap { mediaRepo.getArtistById(it) }
      .doOnNext { artistName = it.description.artist }
      .switchMapMaybe { discogsService.getArtistPictureUrl(it.description.artist) }
      .doOnNext { Log.d("ArtistDetailsVM", "Got uri: $it") }
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy(
        onNext = { artistPicture.value = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()

    // Get Albums
    artistSubject
      .switchMap { mediaRepo.getAlbumsByArtist(it) }
      .doOnNext { isAlbumsVisible = it.isNotEmpty() }
      .subscribeBy(
        onNext = { artistAlbums.value = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()

    // Get Songs
    Observables.combineLatest(
      artistSubject.switchMap { mediaRepo.getSongsByArtist(it) },
      mediaService.mediaMetadata
        .distinctUntilChanged { t: MediaMetadataCompat -> t.mediaId }
        .filter { it.mediaId != null }
    )
      .map { (songs, metadata) -> songs.applyNowPlaying(metadata.mediaId).sortedBy { it.description.titleKey } }
      .doOnNext { songs -> songDescriptions = songs.filter { it.isPlayable }.map { it.description } }
      .subscribeBy(
        onNext = { artistSongs.value = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()
  }
}