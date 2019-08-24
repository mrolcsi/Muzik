package hu.mrolcsi.muzik.library.pager

import hu.mrolcsi.muzik.theme.ThemeService
import hu.mrolcsi.muzik.theme.ThemedViewModelImpl
import javax.inject.Inject

class LibraryPagerViewModelImpl @Inject constructor(
  themeService: ThemeService
) : ThemedViewModelImpl(themeService), LibraryPagerViewModel