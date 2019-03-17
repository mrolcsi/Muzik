package hu.mrolcsi.android.lyricsplayer.common.pager

enum class RVPageScrollState {
  IDLE,
  DRAGGING,
  SETTLING;

  override fun toString(): String {
    return "RVPageScrollState{${this.name}}"
  }
}