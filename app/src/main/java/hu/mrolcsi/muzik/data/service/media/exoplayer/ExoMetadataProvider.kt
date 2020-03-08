package hu.mrolcsi.muzik.data.service.media.exoplayer

import android.graphics.Bitmap
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import android.util.LruCache
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import hu.mrolcsi.muzik.data.MediaStoreWrapper
import hu.mrolcsi.muzik.data.model.media.albumArt
import hu.mrolcsi.muzik.data.model.media.albumArtUri
import hu.mrolcsi.muzik.data.model.media.displayDescription
import hu.mrolcsi.muzik.data.model.media.displayIcon
import hu.mrolcsi.muzik.data.model.media.displayIconUri
import hu.mrolcsi.muzik.data.model.media.displaySubtitle
import hu.mrolcsi.muzik.data.model.media.displayTitle
import hu.mrolcsi.muzik.data.model.media.duration
import hu.mrolcsi.muzik.data.model.media.isAdvertisement
import hu.mrolcsi.muzik.data.model.media.mediaId
import hu.mrolcsi.muzik.data.model.media.mediaUri
import hu.mrolcsi.muzik.data.model.media.title
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import java.io.FileNotFoundException

class ExoMetadataProvider : MediaSessionConnector.MediaMetadataProvider, KoinComponent {

  private val mediaStore: MediaStoreWrapper by inject()
  private val mediaSessionConnector: MediaSessionConnector by inject()

  private val metadataCache = LruCache<String, MediaMetadataCompat>(20)

  private val disposables = CompositeDisposable()
  private val pendingMetadataSubject = PublishSubject.create<Pair<Player, MediaDescriptionCompat>>()

  init {
    pendingMetadataSubject
      .subscribeOn(Schedulers.computation())
      .switchMapSingle { (player, description) -> createMetadata(player, description) }
      .doOnNext {
        Timber.v("Updating cache with {${it.description}}")
        metadataCache.put(it.mediaId, it)
      }
      .doOnError { Timber.e(it, "Error while loading metadata") }
      .retry()
      .subscribeBy(
        onNext = { mediaSessionConnector.invalidateMediaSessionMetadata() },
        onError = { Timber.e(it, "Error while loading metadata") }
      )
      .addTo(disposables)
  }

  override fun getMetadata(player: Player): MediaMetadataCompat =
    ((player.currentTag as MediaDescriptionCompat?)?.let { description ->
      metadataCache[description.mediaId] ?: EMPTY_METADATA.also {
        pendingMetadataSubject.onNext(player to description)
      }
    } ?: EMPTY_METADATA).also {
      Timber.v("getMetadata($player) = ${it.description}")
    }

  private fun createMetadata(
    player: Player,
    description: MediaDescriptionCompat
  ): Single<MediaMetadataCompat> =
    Single.fromCallable {
      if (player.currentTimeline.isEmpty) EMPTY_METADATA
      else MediaMetadataCompat.Builder().apply {
        isAdvertisement = player.isPlayingAd
        duration = if (player.duration == C.TIME_UNSET) -1 else player.duration

        title = description.title?.toString()
        displayTitle = description.title?.toString()
        displaySubtitle = description.subtitle?.toString()
        displayDescription = description.description?.toString()
        displayIcon = description.iconBitmap
        displayIconUri = description.iconUri
        mediaId = description.mediaId!!
        mediaUri = description.mediaUri

        description.extras?.let {
          for (key in it.keySet()) {
            when (val value = it[key]) {
              is String -> putString(key, value)
              is CharSequence -> putText(key, value)
              is Long -> putLong(key, value)
              is Int -> putLong(key, value.toLong())
              is Bitmap -> putBitmap(key, value)
              is RatingCompat -> putRating(key, value)
            }
          }
        }

        try {
          albumArt = mediaStore.getBitmap(description.albumArtUri)
        } catch (e: NullPointerException) {
          // MediaStore throws a NPE when the Uri is invalid (eg. id=-1)
        } catch (e: FileNotFoundException) {
          // ... or a FileNotFoundException on newer versions.
        } catch (e: Exception) {
          Timber.e(e)
        }
      }.build()
    }.subscribeOn(Schedulers.io())

  fun release() {
    disposables.dispose()
  }

}

private val EMPTY_METADATA = MediaMetadataCompat.Builder().build()