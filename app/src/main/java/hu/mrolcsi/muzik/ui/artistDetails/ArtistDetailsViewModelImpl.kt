package hu.mrolcsi.muzik.ui.artistDetails

import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.view.View
import androidx.core.view.ViewCompat
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.FragmentNavigatorExtras
import hu.mrolcsi.muzik.data.manager.media.MediaManager
import hu.mrolcsi.muzik.data.model.media.artist
import hu.mrolcsi.muzik.data.model.media.mediaId
import hu.mrolcsi.muzik.data.model.media.titleKey
import hu.mrolcsi.muzik.data.repository.media.MediaRepository
import hu.mrolcsi.muzik.data.service.discogs.DiscogsService
import hu.mrolcsi.muzik.ui.albums.AlbumItem
import hu.mrolcsi.muzik.ui.albums.asAlbumItems
import hu.mrolcsi.muzik.ui.base.DataBindingViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModelImpl
import hu.mrolcsi.muzik.ui.common.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.ui.common.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.ui.common.ObservableImpl
import hu.mrolcsi.muzik.ui.songs.SongItem
import hu.mrolcsi.muzik.ui.songs.asSongItems
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber

class ArtistDetailsViewModelImpl constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  ThemedViewModel by themedViewModel,
  ArtistDetailsViewModel,
  KoinComponent {

  private val context: Context by inject()
  private val mediaRepo: MediaRepository by inject()
  private val discogsService: DiscogsService by inject()
  private val mediaManager: MediaManager by inject()

  private val artistSubject = BehaviorSubject.create<Long>()

  override val artistSongs = MutableLiveData<List<SongItem>>()

  override val artistAlbums = MutableLiveData<List<AlbumItem>>()

  override var artistName: String? by boundStringOrNull(BR.artistName)

  override val artistPicture = MutableLiveData<Uri>()

  override var isAlbumsVisible: Boolean by boundBoolean(BR.albumsVisible, false)

  private var songDescriptions: List<MediaDescriptionCompat>? = null

  override fun setArgument(artistId: Long) {
    artistSubject.onNext(artistId)
  }

  override fun onAlbumClick(albumItem: AlbumItem, transitionedView: View) {
    val transitionName = ViewCompat.getTransitionName(transitionedView)!!
    sendNavCommand {
      navigate(
        ArtistDetailsFragmentDirections.actionToAlbumDetails(albumItem.id, transitionName),
        FragmentNavigatorExtras(transitionedView to transitionName)
      )
    }
  }

  override fun onSongClick(songItem: SongItem, position: Int) {
    artistName?.let { mediaManager.setQueueTitle(it) }
    songDescriptions?.let { mediaManager.playAll(it, position) }
  }

  override fun onShuffleAllClick() {
    artistName?.let { mediaManager.setQueueTitle(it) }
    songDescriptions?.let { mediaManager.playAllShuffled(it) }
  }

  init {
    // Get artist item and url
    artistSubject
      .switchMap { mediaRepo.getArtistById(it) }
      .doOnNext { artistName = it.description.artist }
      .switchMapMaybe { discogsService.getArtistPictureUrl(it.description.artist) }
      .doOnNext { Timber.d("Got uri: $it") }
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy(
        onNext = { artistPicture.value = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()

    // Get Albums
    artistSubject
      .switchMap { mediaRepo.getAlbumsByArtist(it) }
      .doOnNext { isAlbumsVisible = it.isNotEmpty() }
      .map { it.asAlbumItems(context) }
      .subscribeBy(
        onNext = { artistAlbums.value = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()

    // Get Songs
    Observables.combineLatest(
      artistSubject
        .switchMap { mediaRepo.getSongsByArtist(it) }
        .map { songs -> songs.sortedBy { it.description.titleKey } },
      mediaManager.mediaMetadata
        .distinctUntilChanged { t: MediaMetadataCompat -> t.mediaId }
        .filter { it.mediaId != null }
    )
      .doOnNext { (songs, _) -> songDescriptions = songs.filter { it.isPlayable }.map { it.description } }
      .map { (songs, metadata) -> songs.asSongItems(context, metadata.mediaId) }
      .subscribeBy(
        onNext = { artistSongs.value = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()
  }
}