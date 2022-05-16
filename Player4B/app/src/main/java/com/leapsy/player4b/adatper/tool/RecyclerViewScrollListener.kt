package com.leapsy.player4b.adatper.tool

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.leapsy.player4b.R

class RecyclerViewScrollListener {
    companion object {
        var firstVisiblePosition = 0
        var lastVisiblePosition = 0

        fun setRecyclerViewScrollEventListener(aRecyclerView : RecyclerView, aLayoutManager : LinearLayoutManager, aScrollUpButton : ImageButton, aScrollDownButton : ImageButton, aBackImage1 : ImageView, aBackImage2 : ImageView) {
            aRecyclerView.setOnScrollChangeListener(object : View.OnScrollChangeListener{
                override fun onScrollChange(v: View?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
                    firstVisiblePosition = 0
                    lastVisiblePosition = 0

                    val firstVisibleItem : Int = aLayoutManager.findFirstCompletelyVisibleItemPosition()
                    val lastVisibleItem : Int = aLayoutManager.findLastCompletelyVisibleItemPosition()

                    aBackImage1.setBackgroundResource(if (firstVisibleItem > 0) R.drawable.scroll_up_button_background_enable_layer_list else R.drawable.scroll_up_button_background_disable_layer_list)
                    aBackImage2.setBackgroundResource(if (lastVisibleItem < aLayoutManager.itemCount - 1) R.drawable.scroll_down_button_background_enable_layer_list else R.drawable.scroll_down_button_background_disable_layer_list)

                    aScrollUpButton.visibility = if (firstVisibleItem > 0) View.VISIBLE else View.GONE
                    aScrollDownButton.visibility = if (lastVisibleItem < aLayoutManager.itemCount - 1) View.VISIBLE else View.GONE

                    if (firstVisibleItem >= 0) {
                        firstVisiblePosition = firstVisibleItem
                    }
                    if (lastVisibleItem >= 0) {
                        lastVisiblePosition = lastVisibleItem
                    }
                }
            })
        }

        fun scrollUpRecyclerView(aRecyclerView : RecyclerView, aLayoutManager : LinearLayoutManager) {
            if (firstVisiblePosition == 0) return
            val nextViewPosition : Int = if (firstVisiblePosition - 3 >= 0) firstVisiblePosition - 3 else 0
            aRecyclerView.smoothScrollToPosition(nextViewPosition)
        }

        fun scrollDownRecyclerView(aRecyclerView : RecyclerView, aLayoutManager : LinearLayoutManager) {
            if (lastVisiblePosition == 0) return
            val nextViewPosition : Int = if (lastVisiblePosition + 3 < aLayoutManager.itemCount) lastVisiblePosition + 3 else lastVisiblePosition + 1
            aRecyclerView.smoothScrollToPosition(nextViewPosition)
        }
    }
}