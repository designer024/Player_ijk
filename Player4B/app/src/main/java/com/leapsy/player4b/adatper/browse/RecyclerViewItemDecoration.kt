package com.leapsy.player4b.adatper.browse

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewItemDecoration(aBottomSpace : Int) : RecyclerView.ItemDecoration() {
    private var mBottomSpace : Int = aBottomSpace

    override fun getItemOffsets(outRect : Rect, view : View, parent : RecyclerView, state : RecyclerView.State) {
        /*if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = 0
        } else {
            outRect.top = mTopSpace
        }*/
        outRect.bottom = mBottomSpace
    }
}