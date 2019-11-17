package hu.mrolcsi.muzik.ui.common.pager

import androidx.annotation.IntDef
import androidx.recyclerview.widget.RecyclerView

@IntDef(RecyclerView.SCROLL_STATE_IDLE, RecyclerView.SCROLL_STATE_DRAGGING, RecyclerView.SCROLL_STATE_SETTLING)
@Retention(AnnotationRetention.SOURCE)
annotation class ScrollState