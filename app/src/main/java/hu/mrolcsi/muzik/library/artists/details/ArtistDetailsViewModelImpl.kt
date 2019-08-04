package hu.mrolcsi.muzik.library.artists.details

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.FragmentNavigatorExtras
import com.google.android.exoplayer2.util.Log
import hu.mrolcsi.muzik.R
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
import hu.mrolcsi.muzik.service.extensions.media.type
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class ArtistDetailsViewModelImpl @Inject constructor(
  context: Context,
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  private val mediaRepo: MediaRepository,
  private val discogsService: DiscogsService,
  private val mediaService: MediaService
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  ArtistDetailsViewModel {

  override var artistItem: MediaItem? by Delegates.observable(null) { _, old: MediaItem?, new: MediaItem? ->
    if (old != new && new != null) {
      artistSubject.onNext(new)
    }
  }

  private val artistSubject = PublishSubject.create<MediaItem>()

  override val artistSongs = MutableLiveData<List<MediaItem>>()

  override val artistAlbums = MutableLiveData<List<MediaItem>>()

  override val artistPicture = MutableLiveData<Uri>()

  private var songDescriptions: List<MediaDescriptionCompat>? = null

  override fun onAlbumClick(albumItem: MediaItem, vararg transitionedView: View) {
    if (albumItem.description.type == MediaType.MEDIA_ALBUM) {
      sendNavCommand {
        navigate(
          R.id.navigation_albumDetails,
          ArtistDetailsFragmentArgs(albumItem).toBundle(),
          null,
          FragmentNavigatorExtras(*transitionedView.map { it to ViewCompat.getTransitionName(it)!! }.toTypedArray())
        )
      }
    }
  }

  override fun onSongClick(songItem: MediaItem, position: Int) {

    artistItem?.description?.artist?.let { mediaService.setQueueTitle(it) }

    if (songItem.description.type == MediaType.MEDIA_OTHER) {
      songDescriptions?.let { mediaService.playAllShuffled(it) }
    } else {
      songDescriptions?.let { mediaService.playAll(it, position - 1) }
    }
  }

  private val shuffleAllItem = MediaItem(
    MediaDescriptionCompat.Builder()
      .setMediaId("shuffle/all")
      .setTitle(context.getString(R.string.mediaControl_shuffleAll))
      .setIconBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.ic_shuffle))
      .setExtras(bundleOf(MediaType.MEDIA_TYPE_KEY to MediaType.MEDIA_OTHER))
      .build(),
    0
  )

  init {
    val publishedArtistSubject = artistSubject
      .filter { it.mediaId != null }
      .observeOn(AndroidSchedulers.mainThread())
      .publish()

    // Get Albums
    publishedArtistSubject
      .switchMap { mediaRepo.getAlbumsByArtist(it.description.id) }
      .subscribeBy(
        onNext = { artistAlbums.value = it },
        onError = { showError(this, it) }
      ).disposeOnClear()

    // Get Songs
    publishedArtistSubject
      .switchMap { mediaRepo.getSongsByArtist(it.description.id) }
      .doOnNext { songs -> songDescriptions = songs.filter { it.isPlayable }.map { it.description } }
      .map { it.toMutableList().apply { add(0, shuffleAllItem) }.toList() }
      .subscribeBy(
        onNext = { artistSongs.value = it },
        onError = { showError(this, it) }
      ).disposeOnClear()

    // Get URL for Artist picture
    publishedArtistSubject
      .switchMapMaybe { discogsService.getArtistPictureUrl(it) }
      .observeOn(AndroidSchedulers.mainThread())
      .doOnNext { Log.d("ArtistDetailsVM", "Got uri: $it") }
      .subscribeBy(
        onNext = { artistPicture.value = it },
        onError = { showError(this, it) }
      ).disposeOnClear()

    publishedArtistSubject.connect()
  }
}