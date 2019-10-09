package hu.mrolcsi.muzik.discogs

import android.net.Uri
import io.reactivex.Maybe

interface DiscogsService {

  fun getArtistPictureUrl(artistName: String?): Maybe<Uri>

}