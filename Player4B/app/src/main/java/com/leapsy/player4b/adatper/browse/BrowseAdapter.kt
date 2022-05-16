package com.leapsy.player4b.adatper.browse

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.graphics.Typeface
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.leapsy.player4b.MediaLoader
import com.leapsy.player4b.R
import com.leapsy.player4b.adatper.ISelectFileListener
import com.leapsy.player4b.databinding.MemberListItemViewBinding
import com.leapsy.player4b.util.ConvertLongToDateFormatUtil
import com.leapsy.player4b.util.ConvertUtils

class BrowseAdapter(aContext : Context) : RecyclerView.Adapter<BrowseViewHolder>() {
    private var mContext : Context = aContext

    private lateinit var mMediaStoreCursor : Cursor

    private var mListItmList : ArrayList<String> = ArrayList()

    private var mISelectFileListenerCallback : ISelectFileListener? = null
    fun setMediaPathCallback(aISelectFileListenerCallback : ISelectFileListener?) {
        mISelectFileListenerCallback = aISelectFileListenerCallback
    }

    private lateinit var mMediaLoader : MediaLoader
    fun setMediaLoader(aMediaLoader : MediaLoader) {
        mMediaLoader = aMediaLoader
    }

    companion object {
        private var mIsRootFolder : Boolean = true
        fun getIsRootFolder() : Boolean { return mIsRootFolder }
    }

    override fun onCreateViewHolder(parent : ViewGroup, viewType : Int): BrowseViewHolder {
        val itemBinding = MemberListItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BrowseViewHolder(itemBinding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: BrowseViewHolder, position: Int) {
        val item : String = mListItmList[position]

        holder.mMemberListItemViewBinding.listTitleTextView.setTypeface(null, Typeface.BOLD)

        if (mIsRootFolder) {
            holder.mMemberListItemViewBinding.listIconImageView.setImageResource(R.drawable.ic_icon_file)
        } else {
            when (mMediaLoader.getFileType(item)) {
                MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> {
                    holder.mMemberListItemViewBinding.listIconImageView.setImageResource(R.drawable.ic_icon_muise)
                }

                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> {
                    Glide.with(mContext)
                        .load(mMediaLoader.getMediaData(item)?.aMediaUri)
                        .placeholder(R.drawable.ic_icon_movie)
                        .override(mContext.resources.getDimensionPixelSize(R.dimen.dp_32), mContext.resources.getDimensionPixelSize(R.dimen.dp_32))
                        .into(holder.mMemberListItemViewBinding.listIconImageView)
                }
            }
        }

        holder.mMemberListItemViewBinding.rightArrowImageView.visibility = if (mIsRootFolder) View.VISIBLE else View.GONE

        if (mIsRootFolder) {
            holder.mMemberListItemViewBinding.listTitleTextView.text = item
            if (MediaLoader.getNameOfFolders() != null && MediaLoader.getNameOfFolders()?.size == mListItmList.size) {
                holder.mMemberListItemViewBinding.listSubtitleTextView.text = "${MediaLoader.getNameOfFolders()!![position]}${mContext.resources.getString(R.string.files)}"
            }
        } else {
            if (item.length > 21) {
                val onlyFileName : String = item.substring(0, item.lastIndexOf("."))
                val extensionName : String = item.substring(item.lastIndexOf("."), item.length)
                val fixedName : String = "${item.substring(0, 6)}...${onlyFileName.substring(onlyFileName.length - 7, onlyFileName.length - 1)}${extensionName}"
                holder.mMemberListItemViewBinding.listTitleTextView.text = fixedName
            } else {
                holder.mMemberListItemViewBinding.listTitleTextView.text = item
            }
            holder.mMemberListItemViewBinding.listSubtitleTextView.text = "${ConvertLongToDateFormatUtil.getDateFormat(mMediaLoader.getMediaData(item)!!.aMediaDateAdded)} - ${
                ConvertUtils.secondsToHMS(ConvertUtils.millisecondsToSeconds(mMediaLoader.getMediaData(item)!!.aMediaDuration))}"
        }

        holder.mMemberListItemViewBinding.selectThisItemButton.setOnHoverListener(object : View.OnHoverListener {
            override fun onHover(v : View?, event : MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_HOVER_ENTER -> { holder.mMemberListItemViewBinding.listHoveredImageView.visibility = View.VISIBLE }
                    MotionEvent.ACTION_HOVER_MOVE -> {}
                    MotionEvent.ACTION_HOVER_EXIT -> { holder.mMemberListItemViewBinding.listHoveredImageView.visibility = View.GONE }
                }
                return false
            }
        })
        holder.mMemberListItemViewBinding.selectThisItemButton.setOnClickListener {
            if (mIsRootFolder) {
                mISelectFileListenerCallback?.onFolderSelected(position, item, MediaLoader.getNameOfFolders()!![position])
                mIsRootFolder = false
            } else {
                mISelectFileListenerCallback?.onFileItemClicked(position, item)
            }
        }
    }

    override fun getItemCount() : Int {
        return if (mListItmList.size > 0) mListItmList.size else 0
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

    /**
     * show all list item
     * @param aItemList
     */
    @SuppressLint("NotifyDataSetChanged")
    fun reloadListItems(aItemList : ArrayList<String>) {
        mListItmList = aItemList
        notifyDataSetChanged()
    }

    fun backToBrowseRoot() {
        mIsRootFolder = true
        reloadListItems(MediaLoader.getAllFilesBucketNameList())
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setCurrentIsRootFolder(aIsRoot : Boolean) {
        mIsRootFolder = aIsRoot
        notifyDataSetChanged()
    }
}