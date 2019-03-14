package hu.mrolcsi.android.lyricsplayer.common.pager

sealed class RVPageScrollState {
  object Idle : RVPageScrollState() {
    override fun toString(): String {
      return "State{Idle}"
    }
  }

  object Dragging : RVPageScrollState() {
    override fun toString(): String {
      return "State{Dragging}"
    }
  }

  object Settling : RVPageScrollState() {
    override fun toString(): String {
      return "State{Settling}"
    }
  }
}