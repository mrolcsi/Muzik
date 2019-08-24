package hu.mrolcsi.muzik.theme

import androidx.databinding.Observable
import androidx.lifecycle.LiveData

interface ThemeTestViewModel : Observable {

  val theme: LiveData<Theme>

}