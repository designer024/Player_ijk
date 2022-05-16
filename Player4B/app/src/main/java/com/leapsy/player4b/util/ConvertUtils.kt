package com.leapsy.player4b.util

import android.annotation.SuppressLint
import android.content.Context

class ConvertUtils {
    companion object {
        /**
         * Covert dp to px
         * @param dp
         * @param aContext
         * @return pixel
         */
        fun convertDpToPixel(dp: Float, aContext: Context) : Float {
            return dp * getDensity(aContext)
        }

        /**
         * Covert px to dp
         * @param px
         * @param aContext
         * @return dp
         */
        fun convertPixelToDp(px: Float, aContext: Context) : Float {
            return px / getDensity(aContext)
        }

        /**
         * 取得螢幕密度
         * 120dpi = 0.75
         * 160dpi = 1 (default)
         * 240dpi = 1.5
         * @param aContext
         * @return
         */
        private fun getDensity(aContext: Context) : Float {
            val metrics = aContext.resources.displayMetrics
            return metrics.density
        }

        fun millisecondsToSeconds(aMilliseconds : Long) : Int {
            val res = aMilliseconds.toInt()
            var result = 0
            return if (aMilliseconds % 1000 != 0L) {
                result = res / 1000
                result
            } else {
                res / 1000
            }
        }

        @SuppressLint("DefaultLocale")
        fun secondsToHMS(aSeconds : Int): String {
            val hours = aSeconds / 3600
            val minutes = aSeconds % 3600 / 60
            val seconds = aSeconds % 60
            if (hours < 1) {
                return String.format("%02d:%02d", minutes, seconds)
            }
            else {
                return String.format("%02d:%02d:%02d", hours, minutes, seconds)
            }
        }
    }
}