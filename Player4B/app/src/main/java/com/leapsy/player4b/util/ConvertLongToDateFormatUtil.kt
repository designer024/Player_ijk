package com.leapsy.player4b.util

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

class ConvertLongToDateFormatUtil {
    companion object {
        /**
         * 轉成日期時間格式
         */
        fun getDateFormat(aMediaDateAdded : Long) : String {
            var date = aMediaDateAdded
            @SuppressLint("SimpleDateFormat") val dateFormat = SimpleDateFormat("yyyy/MM/dd")
            date *= 1000L
            return dateFormat.format(Date(date))
        }
    }
}