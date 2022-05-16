package com.leapsy.player4b.util

import com.leapsy.discretescrollviewlib.DiscreteScrollView
import com.leapsy.discretescrollviewlib.InfiniteScrollAdapter

class DiscreteScrollViewUtils {
    companion object {
        fun customSmoothScrollTo(scrollView : DiscreteScrollView, aNextPosition : Int) {
            val adapter = scrollView.adapter
            val itemCount =
                if (adapter is InfiniteScrollAdapter<*>) adapter.realItemCount else adapter?.itemCount
                    ?: 0
            var destination = 0
            destination = (adapter as InfiniteScrollAdapter<*>?)!!.getClosestPosition(aNextPosition)
            scrollView.smoothScrollToPosition(destination)
        }
    }
}