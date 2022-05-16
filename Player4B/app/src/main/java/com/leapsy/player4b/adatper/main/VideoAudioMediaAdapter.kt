package com.leapsy.player4b.adatper.main

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.leapsy.player4b.R
import com.leapsy.player4b.adatper.ICallMediaPath
import com.leapsy.player4b.contentManager.ContentMemberBook
import com.leapsy.player4b.data.MediaData
import com.leapsy.player4b.databinding.MemberMediaThumbnailLayoutBinding
import java.lang.String
import java.util.ArrayList

class VideoAudioMediaAdapter(aContext : Context) : RecyclerView.Adapter<VideoAudioMediaViewHolder>() {
    companion object {
        private val TAG = VideoAudioMediaAdapter::class.java.simpleName
    }

    private var mContext : Context = aContext

    private lateinit var mMediaStoreCursor : Cursor

    private var mMediaDataList : ArrayList<MediaData> = ArrayList()

    private var mICallMediaPathCallback : ICallMediaPath? = null
    fun setMediaPathCallback(aICallMediaPathCallback : ICallMediaPath?) {
        mICallMediaPathCallback = aICallMediaPathCallback
    }

    /**
     * 除了在Recently不行，其它的可以
     */
    private var canRecordToDataBase : Boolean = false

    override fun getItemViewType(position: Int) : Int {
        return mMediaDataList[position].aMediaType
    }

    override fun onCreateViewHolder(parent : ViewGroup, viewType : Int) : VideoAudioMediaViewHolder {
        val itemBinding = MemberMediaThumbnailLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoAudioMediaViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder : VideoAudioMediaViewHolder, position: Int) {
        val item : MediaData = mMediaDataList[position]

        // media display name
        if (item.aMediaDisplayName == "") {
            holder.mMemberMediaThumbnailLayoutBinding.mediaDisplayNameTextView.text = ""
        } else {
            holder.mMemberMediaThumbnailLayoutBinding.mediaDisplayNameTextView.text = item.aMediaDisplayName.substring(0, item.aMediaDisplayName.lastIndexOf("."))
        }


        holder.mMemberMediaThumbnailLayoutBinding.playButton.setOnClickListener {
            if (canRecordToDataBase) {
                setReadMediaToDataBase(item)
            }

            mICallMediaPathCallback?.onPlayMediaFromPath(position, item.aMediaPath)
        }

        when (item.aMediaType) {
            MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> {
                holder.mMemberMediaThumbnailLayoutBinding.musicSymbolImageView.visibility = View.VISIBLE
            }

            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> {
                holder.mMemberMediaThumbnailLayoutBinding.musicSymbolImageView.visibility = View.GONE

                Glide.with(mContext)
                    .load(item.aMediaUri)
                    .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(mContext.resources.getDimensionPixelSize(R.dimen.dp_8))))
                    .placeholder(R.drawable.ic_icon_movie)
                    .override(mContext.resources.getDimensionPixelSize(R.dimen.dp_130), mContext.resources.getDimensionPixelSize(R.dimen.dp_130))
                    .into(holder.mMemberMediaThumbnailLayoutBinding.mediaImageView)
            }
        }
    }

    override fun getItemCount() : Int {
        return if (mMediaDataList.size > 0) mMediaDataList.size else 0
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun swapCursor(aCursor: Cursor) : Cursor? {
        if (mMediaStoreCursor == aCursor) {
            return null
        }
        val oldCursor = mMediaStoreCursor
        mMediaStoreCursor = aCursor
        if (aCursor != null) {
            notifyDataSetChanged()
        }
        return oldCursor
    }

    fun changeCursor(aCursor: Cursor?) {
        val oldCursor : Cursor? = swapCursor(aCursor!!)
        oldCursor?.close()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setCanRecord(aCan : Boolean) {
        canRecordToDataBase = aCan
        notifyDataSetChanged()
    }
    /**
     * 將媒體資訊記錄於database
     */
    fun setReadMediaToDataBase(aReadMedia : MediaData) {
        val newValue = ContentValues()
        newValue.put(ContentMemberBook.Member.MEDIA_ID, aReadMedia.aMediaId)
        newValue.put(ContentMemberBook.Member.NAME, aReadMedia.aMediaDisplayName)
        newValue.put(ContentMemberBook.Member.PATH, aReadMedia.aMediaPath)
        newValue.put(ContentMemberBook.Member.BUCKET, aReadMedia.aMediaBucketName)
        newValue.put(ContentMemberBook.Member.TYPE, aReadMedia.aMediaType)
        newValue.put(ContentMemberBook.Member.DURATION, String.valueOf(aReadMedia.aMediaDuration))
        val mUri = mContext.contentResolver.insert(ContentMemberBook.CONTENT_URI, newValue)
        if (mUri != null) {
            //Toast.makeText(mContext,  "New item added success! ", Toast.LENGTH_SHORT).show();
            android.util.Log.d(TAG, "vv New item added success! vv")
        } else {
            //Toast.makeText(mContext,  "New item added failed! ", Toast.LENGTH_SHORT).show();
            android.util.Log.d(TAG, "xx New item added failed! xx")
        }
    }

    /**
     * show 出所有媒體包含影片、聲音
     */
    @SuppressLint("NotifyDataSetChanged")
    fun reloadAllFilesMediaData(aMediaDataList : ArrayList<MediaData>) {
        mMediaDataList = aMediaDataList
        notifyDataSetChanged()
    }


}