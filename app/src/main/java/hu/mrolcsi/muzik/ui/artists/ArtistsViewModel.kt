package hu.mrolcsi.muzik.ui.artists

import android.support.v4.media.MediaBrowserCompat
import hu.mrolcsi.muzik.ui.base.ListViewModel
import hu.mrolcsi.muzik.ui.common.NavCommandSource
import hu.mrolcsi.muzik.ui.common.UiCommandSource
import hu.mrolcsi.muzik.ui.base.ThemedViewModel

interface ArtistsViewModel :
  ListViewModel<MediaBrowserCompat.MediaItem>, ThemedViewModel, UiCommandSource,
  NavCommandSource