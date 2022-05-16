package com.leapsy.player4b.data

import android.net.Uri

data class MediaData(val aMediaId : Long, val aMediaType : Int, val aMediaUri : Uri, val aMediaPath : String, val aMediaDisplayName : String, val aMediaBucketName : String, val aMediaDuration : Long, val aMediaDateAdded : Long) {}