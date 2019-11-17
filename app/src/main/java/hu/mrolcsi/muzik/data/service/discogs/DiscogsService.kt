package hu.mrolcsi.muzik.data.service.discogs

import android.net.Uri
import io.reactivex.Maybe

interface DiscogsService {

  fun getArtistPictureUrl(artistName: String?): Maybe<Uri>

}