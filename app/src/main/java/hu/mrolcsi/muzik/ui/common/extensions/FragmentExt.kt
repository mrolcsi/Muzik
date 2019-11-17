package hu.mrolcsi.muzik.ui.common.extensions

import androidx.annotation.TransitionRes
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater

fun Fragment.applySharedElementTransition(
  @TransitionRes transitionId: Int,
  animationDuration: Long
) {

  val transitionInflater = TransitionInflater.from(requireContext())

  sharedElementEnterTransition = transitionInflater
    .inflateTransition(transitionId)
    .setDuration(animationDuration)

  sharedElementReturnTransition = transitionInflater
    .inflateTransition(transitionId)
    .setDuration(animationDuration)
}