package com.leapsy.player4b.player

import android.content.Context
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.leapsy.player4b.tool.SingletonHolder

class MyPlayer private constructor(aContext : Context) {
    init {}
    companion object : SingletonHolder<MyPlayer, Context>(::MyPlayer) {
        var CURRENT_PLAYING_ORDER : Int = 0

        /**
         * 主菜單App Layout的參數
         * @param aWidth Icon width
         * @param aHeight Icon Height
         * @return
         */
        fun relativeLayoutParams() : ViewGroup.LayoutParams {
            val params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE)
            params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
            return params
        }
    }
}