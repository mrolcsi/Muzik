package hu.mrolcsi.muzik.theme

import androidx.databinding.Observable
import androidx.lifecycle.LiveData
import hu.mrolcsi.muzik.data.model.theme.Theme

interface ThemeTestViewModel : Observable {

  val theme: LiveData<Theme>

}