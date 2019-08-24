package hu.mrolcsi.muzik.library

import hu.mrolcsi.muzik.theme.ThemeService
import hu.mrolcsi.muzik.theme.ThemedViewModelImpl
import javax.inject.Inject

class LibraryViewModelImpl @Inject constructor(
  themeService: ThemeService
) : ThemedViewModelImpl(themeService), LibraryViewModel