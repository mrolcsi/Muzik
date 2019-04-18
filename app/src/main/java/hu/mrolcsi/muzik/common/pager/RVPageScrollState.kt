package hu.mrolcsi.muzik.common.pager

enum class RVPageScrollState {
  IDLE,
  DRAGGING,
  SETTLING;

  override fun toString(): String {
    return "RVPageScrollState{${this.name}}"
  }
}