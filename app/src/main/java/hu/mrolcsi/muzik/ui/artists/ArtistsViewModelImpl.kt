package hu.mrolcsi.muzik.ui.artists

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.MutableLiveData
import hu.mrolcsi.muzik.R
import hu.mrolcsi.muzik.data.model.media.id
import hu.mrolcsi.muzik.data.model.media.numberOfAlbums
import hu.mrolcsi.muzik.data.model.media.numberOfTracks
import hu.mrolcsi.muzik.data.repository.media.MediaRepository
import hu.mrolcsi.muzik.ui.base.DataBindingViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModel
import hu.mrolcsi.muzik.ui.base.ThemedViewModelImpl
import hu.mrolcsi.muzik.ui.common.ExecuteOnceNavCommandSource
import hu.mrolcsi.muzik.ui.common.ExecuteOnceUiCommandSource
import hu.mrolcsi.muzik.ui.common.ObservableImpl
import hu.mrolcsi.muzik.ui.common.extensions.toKeyString
import hu.mrolcsi.muzik.ui.library.LibraryFragmentDirections
import io.reactivex.rxkotlin.subscribeBy
import org.koin.core.KoinComponent
import org.koin.core.inject

class ArtistsViewModelImpl constructor(
  observable: ObservableImpl,
  uiCommandSource: ExecuteOnceUiCommandSource,
  navCommandSource: ExecuteOnceNavCommandSource,
  themedViewModel: ThemedViewModelImpl
) : DataBindingViewModel(observable, uiCommandSource, navCommandSource),
  ThemedViewModel by themedViewModel,
  ArtistsViewModel,
  KoinComponent {

  private val context: Context by inject()
  private val mediaRepo: MediaRepository by inject()

  override var progressVisible: Boolean by boundBoolean(BR.progressVisible)
  override var listViewVisible: Boolean by boundBoolean(BR.listViewVisible)
  override var emptyViewVisible: Boolean by boundBoolean(BR.emptyViewVisible)

  override val items = MutableLiveData<List<ArtistItem>>()

  init {
    mediaRepo.getArtists()
      .doOnSubscribe { progressVisible = true }
      .doOnNext { progressVisible = false }
      .map { it.asArtistItems() }
      .subscribeBy(
        onNext = { items.value = it },
        onError = { showError(this, it) }
      ).disposeOnCleared()
  }

  override fun onSelect(item: ArtistItem) {
    sendNavCommand {
      navigate(LibraryFragmentDirections.actionToArtistDetails(item.id))
    }
  }

  private fun List<MediaBrowserCompat.MediaItem>.asArtistItems() = map { item ->
    val numberOfAlbums = item.description.numberOfAlbums
    val numberOfSongs = item.description.numberOfTracks
    val numberOfAlbumsString = context.resources.getQuantityString(
      R.plurals.artists_numberOfAlbums, numberOfAlbums, numberOfAlbums
    )
    val numberOfSongsString = context.resources.getQuantityString(
      R.plurals.artists_numberOfSongs, numberOfSongs, numberOfSongs
    )
    val numberOfSongsText = context.getString(
      R.string.artists_item_subtitle,
      numberOfAlbumsString,
      numberOfSongsString
    )

    ArtistItem(
      item.description.id,
      item.description.title ?: context.getString(R.string.songs_unknownArtist),
      numberOfSongsText
    )
  }

  override fun getSectionText(artistItem: ArtistItem): CharSequence =
    artistItem.artistText.toKeyString().first().toUpperCase().toString()

}