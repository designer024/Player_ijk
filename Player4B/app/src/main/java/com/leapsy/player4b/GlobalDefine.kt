package com.leapsy.player4b

class GlobalDefine {

    /**
     * 現在是在哪個頁面
     */
    class MainPage {
        companion object {
            /**
             * To page
             */
            var WhichPage : Int = 0
            /**
             * 10: 最近看過的頁面
             */
            const val RECENTLY_PAGE : Int = 10
            /**
             * 12: 我的音樂庫 頁面
             */
            const val MUSIC_PAGE : Int = 12
            /**
             * 13: 我的影片庫 頁面
             */
            const val VIDEO_PAGE : Int = 13
            /**
             * 15: 檔案頁面Home
             */
            const val ALL_FILES_HOME_PAGE : Int = 15
            /**
             * 15-1: 進入資料夾裡了
             */
            const val ALL_FILES_FOLDER_ENTERED_PAGE = 151
            /**
             * 20: 全螢幕頁面
             */
            const val FULL_SCREEN_PAGE : Int = 20
        }
    }
}