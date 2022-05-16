package com.leapsy.player4b

import android.app.Activity
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.leapsy.player4b.adatper.browse.BrowseAdapter
import com.leapsy.player4b.adatper.main.VideoAudioMediaAdapter
import com.leapsy.player4b.contentManager.ContentMemberBook
import com.leapsy.player4b.data.MediaData
import com.leapsy.player4b.util.GetMediaMetaDataUtil
import java.util.*

class MediaLoader(aActivity : Activity, aVideoAudioMediaAdapter : VideoAudioMediaAdapter, aBrowseAdapter : BrowseAdapter)  : LoaderManager.LoaderCallbacks<Cursor> {
    companion object {
        private val TAG = MediaLoader::class.java.simpleName
        const val MEDIA_STORE_LOADER_ID = 0

        /**
         * 此裝置所有媒體的ArrayList
         */
        private var mAllFilesMediaDataList = ArrayList<MediaData>()
        /**
         * 此裝置所有媒體的ArrayList
         */
        fun getAllFilesMediaDataList() : ArrayList<MediaData> {
            return mAllFilesMediaDataList
        }

        /**
         * recently的ArrayList
         */
        private var mRecentlyMediaDataList = ArrayList<MediaData>()
        /**
         * recently的ArrayList
         */
        fun getRecentlyMediaDataList() : ArrayList<MediaData> {
            return mRecentlyMediaDataList
        }

        /**
         * 此裝置所有video的ArrayList
         */
        private var mVideoMediaDataList = ArrayList<MediaData>()
        /**
         * 此裝置所有video的ArrayList
         */
        fun getVideoMediaDataList(): ArrayList<MediaData> {
            return mVideoMediaDataList
        }

        /**
         * 此裝置所有music的ArrayList
         */
        private var mMusicMediaDataList = ArrayList<MediaData>()
        /**
         * 此裝置所有music的ArrayList
         */
        fun getMusicMediaDataList(): ArrayList<MediaData> {
            return mMusicMediaDataList
        }

        private var mAllFilesBucketNameList = ArrayList<String>()
        fun getAllFilesBucketNameList(): ArrayList<String> {
            return mAllFilesBucketNameList
        }

        private var mNameOfFolders : IntArray? = null
        fun getNameOfFolders() : IntArray? {
            return mNameOfFolders
        }
    }

    private var mActivity : Activity = aActivity
    private var mVideoAudioMediaAdapter : VideoAudioMediaAdapter = aVideoAudioMediaAdapter
    private var mBrowseAdapter : BrowseAdapter = aBrowseAdapter

    private lateinit var mDeviceMediaDataCursor : Cursor
    /**
     * 此裝置所有影片的數量
     */
    private var mDeviceMediaAmount : Int = 0

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateLoader(id : Int, args : Bundle?) : Loader<Cursor> {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATE_ADDED,  //MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.DURATION,
            MediaStore.Files.FileColumns.DISPLAY_NAME
        )
        val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO
                + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
        return CursorLoader(
            mActivity,
            MediaStore.Files.getContentUri("external"),
            projection,
            selection,
            null,
            MediaStore.Files.FileColumns.DATE_ADDED + " DESC") //DESC降冪 ASC升冪
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        if (data != null) {
            mDeviceMediaDataCursor = data
            //mVideoAudioMediaAdapter.changeCursor(data)
            mDeviceMediaAmount = mDeviceMediaDataCursor.count
            refreshAllMediaData()
        } else {
            android.util.Log.e("xxooxox", "data is null")
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        mVideoAudioMediaAdapter.changeCursor(null)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun refreshAllMediaData() {
        if (mDeviceMediaAmount > 0) {
            mDeviceMediaDataCursor.moveToFirst()
            mAllFilesMediaDataList.clear()
            mVideoMediaDataList.clear()
            mMusicMediaDataList.clear()
            mAllFilesBucketNameList.clear()
            do {
                val mediaId = mDeviceMediaDataCursor.getLong(mDeviceMediaDataCursor.getColumnIndex(MediaStore.Files.FileColumns._ID))
                val mediaType = mDeviceMediaDataCursor.getInt(mDeviceMediaDataCursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE))
                var mediaDuration = mDeviceMediaDataCursor.getLong(mDeviceMediaDataCursor.getColumnIndex(MediaStore.Files.FileColumns.DURATION))
                var mediaDisplayName = mDeviceMediaDataCursor.getString(mDeviceMediaDataCursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME))
                val bucketDisplayName = mDeviceMediaDataCursor.getString(mDeviceMediaDataCursor.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME))
                val mediaDateAdded = mDeviceMediaDataCursor.getLong(mDeviceMediaDataCursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED))
                val mediaPath = mDeviceMediaDataCursor.getString(mDeviceMediaDataCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA))
                val mediaUri = Uri.parse("file://$mediaPath")

                if (mediaDisplayName.isNullOrEmpty()) {
                    mediaDisplayName = ""
                }

                if (mediaDuration <= 0) {
                    mediaDuration = GetMediaMetaDataUtil.getMediaMetaDataDuration(mediaPath)
                }

                val item : MediaData = MediaData(mediaId, mediaType, mediaUri, mediaPath, mediaDisplayName, bucketDisplayName, mediaDuration, mediaDateAdded)
                mAllFilesMediaDataList.add(item)

                mAllFilesBucketNameList.add(bucketDisplayName)

                when (mediaType) {
                    MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> {
                        val audioItem : MediaData = MediaData(mediaId, MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO, mediaUri, mediaPath, mediaDisplayName, bucketDisplayName, mediaDuration, mediaDateAdded)
                        mMusicMediaDataList.add(audioItem)
                        //android.util.Log.d("xxoxoxA", "audio name: ${audioItem.aMediaDisplayName}, duration: ${audioItem.aMediaDuration}")
                    }
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> {
                        val videoItem : MediaData = MediaData(mediaId, MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO, mediaUri, mediaPath, mediaDisplayName, bucketDisplayName, mediaDuration, mediaDateAdded)
                        mVideoMediaDataList.add(videoItem)
                        //android.util.Log.d("xxoxoxV", "video name: ${videoItem.aMediaDisplayName}, duration: ${videoItem.aMediaDuration}")
                    }
                }

            } while (mDeviceMediaDataCursor.moveToNext())

            //android.util.Log.d("xxoxox", "video: ${mVideoMediaDataList.size}, audio: ${mMusicMediaDataList.size}")

            val temp = LinkedHashSet(mAllFilesBucketNameList)
            mAllFilesBucketNameList.clear()
            mAllFilesBucketNameList.addAll(temp)

            if (mNameOfFolders != null) {
                mNameOfFolders = null
            }
            mNameOfFolders = IntArray(mAllFilesBucketNameList.size)
            Arrays.fill(mNameOfFolders!!, 0)

            for (i in mAllFilesMediaDataList.indices) {
                val bucketName: String = mAllFilesMediaDataList[i].aMediaBucketName
                for (j in mAllFilesBucketNameList.indices) {
                    if (bucketName == mAllFilesBucketNameList[j]) {
                        mNameOfFolders!![j] += 1
                    }
                }
            }

            mBrowseAdapter.reloadListItems(mAllFilesBucketNameList)
        }
    }

    fun loadAllRecentlyMedia() {
        mRecentlyMediaDataList.clear()

        val cursor = mActivity.contentResolver.query(ContentMemberBook.CONTENT_URI, null, null, null, null)

        if (cursor != null) {
            cursor.moveToFirst()
            while (cursor.moveToNext()) {
                val mediaId = cursor.getString(1).toLong()
                val mediaDuration = cursor.getString(6).toLong()
                val mediaUri = Uri.parse("file://${cursor.getString(3)}")
                val item = MediaData(mediaId, cursor.getInt(5), mediaUri, cursor.getString(3), cursor.getString(2), cursor.getString(4), mediaDuration, System.currentTimeMillis())
                mRecentlyMediaDataList.add(item)
            }
            cursor.close()

            if (mRecentlyMediaDataList.size > 0) {
                mVideoAudioMediaAdapter.reloadAllFilesMediaData(mRecentlyMediaDataList)
            }
        } else {
            android.util.Log.d(TAG, "cursor is null")
        }
    }

    /**
     * 點選並進入該資料夾
     */
    fun enterFolder(aPosition : Int, aBucketName : String, aNumberOfFilesInFolder : Int) {
        val itemNames = ArrayList<String>()
        for (i in mAllFilesMediaDataList.indices) {
            if (mAllFilesMediaDataList[i].aMediaBucketName.equals(aBucketName, ignoreCase = true)) {
                itemNames.add(mAllFilesMediaDataList[i].aMediaDisplayName)
            }
        }

        if (itemNames.size > 0) {
            mBrowseAdapter.reloadListItems(itemNames)
        }
    }

    /**
     * 取得該媒體之路徑
     */
    fun getFileFullPath(aFileName : String) : String {
        for (i in 0 until mAllFilesMediaDataList.size) {
            if (mAllFilesMediaDataList[i].aMediaDisplayName == aFileName) {
                return mAllFilesMediaDataList[i].aMediaPath
            }
        }
        return ""
    }

    /**
     * 取得該媒體之類型
     */
    fun getFileType(aFileName : String) : Int {
        for (i in 0 until mAllFilesMediaDataList.size) {
            if (mAllFilesMediaDataList[i].aMediaDisplayName == aFileName) {
                return mAllFilesMediaDataList[i].aMediaType
            }
        }
        return MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
    }

    /**
     * 取得該媒體之Data
     */
    fun getMediaData(aFileName : String) : MediaData? {
        for (i in 0 until mAllFilesMediaDataList.size) {
            if (mAllFilesMediaDataList[i].aMediaDisplayName == aFileName) {
                return mAllFilesMediaDataList[i]
            }
        }
        return null
    }
}