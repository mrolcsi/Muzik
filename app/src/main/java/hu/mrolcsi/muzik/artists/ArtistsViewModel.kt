package hu.mrolcsi.muzik.artists

import android.support.v4.media.MediaBrowserCompat
import hu.mrolcsi.muzik.common.viewmodel.ListViewModel
import hu.mrolcsi.muzik.common.viewmodel.NavCommandSource
import hu.mrolcsi.muzik.common.viewmodel.UiCommandSource
import hu.mrolcsi.muzik.theme.ThemedViewModel

interface ArtistsViewModel :
  ListViewModel<MediaBrowserCompat.MediaItem>, ThemedViewModel, UiCommandSource, NavCommandSource