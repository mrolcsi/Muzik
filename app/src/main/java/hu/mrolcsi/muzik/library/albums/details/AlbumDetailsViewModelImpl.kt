package hu.mrolcsi.muzik.library.albums.details

import android.content.Context
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.core.os.bundleOf
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.common.glide.GlideApp
import hu.mrolcsi.muzik.common.glide.onResourceReady
import hu.mrolcsi.muzik.common.viewmodel.DataBindingViewModel
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.common.viewmodel.ObservableImpl
import hu.mrolcsi.muzik.library.songs.applyNowPlaying
import hu.mrolcsi.muzik.media.MediaRepository
import hu.mrolcsi.muzik.media.MediaService
import hu.mrolcsi.muzik.service.extensions.media.MediaType
import hu.mrolcsi.muzik.service.extensions.media.album
import hu.mrolcsi.muzik.service.extensions.media.albumArtUri
import hu.mrolcsi.muzik.service.extensions.media.albumYear
import hu.mrolcsi.muzik.service.extensions.media.artist
import hu.mrolcsi.muzik.service.extensions.media.id
import hu.mrolcsi.muzik.service.extensions.media.mediaId
import hu.mrolcsi.muzik.service.extensions.media.numberOfSongs
import hu.mrolcsi.muzik.service.extensions.media.titleKey
import hu.mrolcsi.muzik.service.extensions.media.trackNumber
import hu.mrolcsi.muzik.theme.Theme
import hu.mrolcsi.muzik.theme.ThemedViewModel
import hu.mrolcsi.muzik.theme.ThemedViewModelImpl
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import org.koin.core.KoinComponent
import org.koin.core.inject

class AlbumDetailsViewModelImpl constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  ThemedViewModel by themedViewModel,
  AlbumDetailsViewModel,
  KoinComponent {

  private val context: Context by inject()
  private val mediaService: MediaService by inject()
  private val mediaRepo: MediaRepository by inject()

  override val progressVisible: Boolean = false
  override val listViewVisible: Boolean = true
  override val emptyViewVisible: Boolean = false

  override val items = MutableLiveData<List<MediaItem>>()

  override var albumTitleText: String? by boundStringOrNull(BR.albumTitleText)
  override var artistText: String? by boundStringOrNull(BR.artistText)
  override var yearText: String? by boundStringOrNull(BR.yearText)
  override var numberOfSongsText: String? by boundStringOrNull(BR.numberOfSongsText)

  override fun setArgument(albumId: Long) {
    albumSubject.onNext(albumId)
  }

  private val albumSubject = BehaviorSubject.create<Long>()

  override var albumItem = MutableLiveData<MediaItem>()

  override val albumTheme = MutableLiveData<Theme>()

  override fun onSongClick(songItem: MediaItem, position: Int) {
    albumItem.value?.description?.artist?.let { mediaService.setQueueTitle(it) }
    songDescriptions?.let { mediaService.playAll(it, position) }
  }

  override fun onShuffleAllClick() {
    albumItem.value?.description?.artist?.let { mediaService.setQueueTitle(it) }
    songDescriptions?.let { mediaService.playAllShuffled(it) }
  }

  private var songDescriptions: List<MediaDescriptionCompat>? = null

  init {
    Observables.combineLatest(
      albumSubject
        .switchMap { mediaRepo.getAlbumById(it) }
        .doOnNext { updateHeaderText(it) }
        .switchMap { mediaRepo.getSongsFromAlbum(it.description.id) },
      mediaService.mediaMetadata
        .distinctUntilChanged { t: MediaMetadataCompat -> t.mediaId }
        .filter { it.mediaId != null }
    )
      .map { (songs, metadata) ->
        songs
          .applyNowPlaying(metadata.mediaId)
          .sortedBy { it.description.titleKey }
          .sortedBy { it.description.trackNumber }
      }
      .doOnNext { songs -> songDescriptions = songs.filter { it.isPlayable }.map { it.description } }
      .map { it.addDiscIndicator() }
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy(
        onNext = { items.value = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()
  }

  private fun updateHeaderText(albumItem: MediaItem) {
    this.albumItem.value = albumItem

    albumTitleText = albumItem.description.album
    artistText = albumItem.description.artist
    yearText = albumItem.description.albumYear

    val numberOfSong = albumItem.description.numberOfSongs
    numberOfSongsText = context.resources.getQuantityString(R.plurals.artists_numberOfSongs, numberOfSong, numberOfSong)

    GlideApp.with(context)
      .asBitmap()
      .load(albumItem.description.albumArtUri)
      .onResourceReady { albumArt -> themeService.createTheme(albumArt).subscribeBy { albumTheme.value = it } }
      .preload()
  }

  private fun List<MediaItem>.addDiscIndicator(): List<MediaItem> = toMutableList().apply {
    if (last().description.trackNumber > 1000) {
      // Add disc number indicators
      val numDiscs = last().description.trackNumber / 1000
      if (numDiscs > 0) {
        for (i in 1..numDiscs) {
          val index = indexOfFirst { it.description.trackNumber > 1000 }
          val item = MediaItem(
            MediaDescriptionCompat.Builder()
              .setMediaId("disc/$i")
              .setTitle(i.toString())
              .setExtras(bundleOf(MediaType.MEDIA_TYPE_KEY to MediaType.MEDIA_OTHER))
              .build(),
            MediaItem.FLAG_BROWSABLE
          )
          add(index, item)
        }
      }
    }
  }.toList()
}