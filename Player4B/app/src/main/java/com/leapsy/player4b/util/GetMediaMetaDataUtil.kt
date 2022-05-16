package com.leapsy.player4b.util

import android.media.MediaMetadataRetriever
import java.io.File

class GetMediaMetaDataUtil {
    companion object {
        private val TAG = GetMediaMetaDataUtil::class.java.simpleName

        /**
         * 影片的寬
         */
        private var videoWidth = 0
        /**
         * 影片的寬
         */
        fun getVideoWidth(): Int { return videoWidth }

        /**
         * 影片的高
         */
        private var videoHeight = 0
        /**
         * 影片的高
         */
        fun getVideoHeight(): Int { return videoHeight }

        /**
         * duration of media
         */
        private var mediaDuration: Long = 0
        /**
         * duration of media
         */
        fun getMediaDuration(): Long { return mediaDuration }

        fun getMediaMetaDataDuration(aMediaPath : String) : Long {
            var temp : Long = 0
            val file = File(aMediaPath)
            if (file.exists()) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(file.absolutePath)
                    if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) != null) {
                        android.util.Log.d(TAG, "Metadata :: playback duration (in ms) : ${retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)}")
                        temp = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.util.Log.d(TAG, "try to get duration error:  ${e.message}")
                }
            }
            return temp
        }

        fun getMediaMetaData(aMediaPath : String) {
            val file = File(aMediaPath)
            if (file.exists()) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(file.absolutePath)
                    if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) != null) {
                        android.util.Log.d(TAG, "Metadata :: 影片 寬: $videoWidth")
                        videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
                    }
                    if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) != null) {
                        android.util.Log.d(TAG, "Metadata :: 影片 高: $videoHeight")
                        videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
                    }
                    if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) != null) {
                        android.util.Log.d(TAG, "Metadata :: playback duration (in ms) : ${retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)}")
                        mediaDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
                    }
                    if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) != null) {
                        android.util.Log.d(TAG, "Metadata :: mime type : ${retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.util.Log.d(TAG, "try catch error:  ${e.message}")
                }
            } else {
                android.util.Log.d(TAG, "File: ${aMediaPath} is not exists")
            }
        }
    }
}