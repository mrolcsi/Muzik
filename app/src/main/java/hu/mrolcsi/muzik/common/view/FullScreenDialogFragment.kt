package hu.mrolcsi.muzik.common.view

import android.os.Bundle
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import hu.mrolcsi.muzik.R

open class FullScreenDialogFragment : DialogFragment() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setStyle(STYLE_NORMAL, R.style.FullScreenDialogTheme)
  }

  override fun onStart() {
    super.onStart()
    dialog?.window?.run {
      setLayout(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      setWindowAnimations(R.style.FullScreenDialogTheme_Transitions)
    }
  }
}