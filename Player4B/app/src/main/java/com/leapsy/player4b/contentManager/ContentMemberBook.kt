package com.leapsy.player4b.contentManager

import android.net.Uri
import android.provider.BaseColumns
import java.io.File

class ContentMemberBook {
    companion object {
        const val AUTHORITY : String = "com.leapsy.leapsyijkplayerkotlinprovider"
        val PATH : String = String.format("%s%s", File.separator, "recentlyMedia")
        val CONTENT_URI : Uri = Uri.parse(String.format("%s%s%s", "content://", AUTHORITY, PATH))

        const val CONTENT_LIST : String = "vnd.android.cursor.dir/vnd.com.leapsy.leapsyijkplayerkotlin"
        const val CONTENT_ITEM : String = "vnd.android.cursor.item/vnd.com.leapsy.leapsyijkplayerkotlin"

        const val DATABASE_NAME : String = "LeapsyIjkPlayerRecentlyDataBase"

        const val DATABASE_VERSION : Int = 1
    }

    class Member : BaseColumns {
        companion object {
            const val TABLE_NAME : String = "ijkplayerrecently"

            const val ID : String = "_id"
            const val MEDIA_ID = "mediaId"
            const val NAME = "name"
            const val PATH = "path"
            const val BUCKET = "bucket"
            const val TYPE = "type"
            const val DURATION = "duration"
        }
    }
}