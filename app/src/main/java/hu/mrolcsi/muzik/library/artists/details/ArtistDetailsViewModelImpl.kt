package hu.mrolcsi.muzik.library.artists.details

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
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.viewmodel.DataBindingViewModel
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ObservableImpl
import hu.mrolcsi.muzik.discogs.DiscogsService
import hu.mrolcsi.muzik.library.albums.details.AlbumDetailsFragmentArgs
import hu.mrolcsi.muzik.library.songs.applyNowPlaying
import hu.mrolcsi.muzik.media.MediaRepository
import hu.mrolcsi.muzik.media.MediaService
import hu.mrolcsi.muzik.service.extensions.media.MediaType
import hu.mrolcsi.muzik.service.extensions.media.artist
import hu.mrolcsi.muzik.service.extensions.media.id
import hu.mrolcsi.muzik.service.extensions.media.mediaId
import hu.mrolcsi.muzik.service.extensions.media.titleKey
import hu.mrolcsi.muzik.service.extensions.media.type
import hu.mrolcsi.muzik.theme.ThemedViewModel
import hu.mrolcsi.muzik.theme.ThemedViewModelImpl
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class ArtistDetailsViewModelImpl @Inject constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl,
  private val mediaRepo: MediaRepository,
  private val discogsService: DiscogsService,
  private val mediaService: MediaService
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  ThemedViewModel by themedViewModel,
  ArtistDetailsViewModel {

  private val artistSubject = PublishSubject.create<MediaItem>()

  override val artistSongs = MutableLiveData<List<MediaItem>>()

  override val artistAlbums = MutableLiveData<List<MediaItem>>()

  override var artistName: String? by boundStringOrNull(BR.artistName)

  override val artistPicture = MutableLiveData<Uri>()

  override var isAlbumsVisible: Boolean by boundBoolean(BR.albumsVisible, false)

  private var songDescriptions: List<MediaDescriptionCompat>? = null

  override fun setArguments(artistItem: MediaItem) {
    artistSubject.onNext(artistItem)
  }

  override fun onAlbumClick(albumItem: MediaItem, transitionedView: View) {
    if (albumItem.description.type == MediaType.MEDIA_ALBUM) {
      val transitionName = ViewCompat.getTransitionName(transitionedView)!!
      sendNavCommand {
        navigate(
          R.id.navAlbumDetails,
          AlbumDetailsFragmentArgs(albumItem, transitionName).toBundle(),
          null,
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
    val publishedArtistSubject = artistSubject
      .filter { it.mediaId != null }
      .doOnNext { artistName = it.description.artist }
      .observeOn(AndroidSchedulers.mainThread())
      .publish()

    // Get Albums
    publishedArtistSubject
      .switchMap { mediaRepo.getAlbumsByArtist(it.description.id) }
      .doOnNext { isAlbumsVisible = it.isNotEmpty() }
      .subscribeBy(
        onNext = { artistAlbums.value = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()

    // Get Songs
    Observables.combineLatest(
      publishedArtistSubject.switchMap { mediaRepo.getSongsByArtist(it.description.id) },
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

    // Get URL for Artist picture
    publishedArtistSubject
      .switchMapMaybe { discogsService.getArtistPictureUrl(it) }
      .observeOn(AndroidSchedulers.mainThread())
      .doOnNext { Log.d("ArtistDetailsVM", "Got uri: $it") }
      .subscribeBy(
        onNext = { artistPicture.value = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()

    publishedArtistSubject.connect()
  }
}