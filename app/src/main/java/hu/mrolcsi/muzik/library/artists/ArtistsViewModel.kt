package hu.mrolcsi.muzik.library.artists

import android.support.v4.media.MediaBrowserCompat
import hu.mrolcsi.muzik.common.viewmodel.ListViewModel
import hu.mrolcsi.muzik.common.viewmodel.NavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.UiCommandSource

interface ArtistsViewModel : ListViewModel<MediaBrowserCompat.MediaItem>, UiCommandSource, NavCommandSource