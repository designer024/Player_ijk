package com.leapsy.player4b.activities

import android.annotation.SuppressLint
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.*
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.loader.app.LoaderManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.leapsy.discretescrollviewlib.DSVOrientation
import com.leapsy.discretescrollviewlib.DiscreteScrollView
import com.leapsy.discretescrollviewlib.InfiniteScrollAdapter
import com.leapsy.discretescrollviewlib.transform.ScaleTransformer
import com.leapsy.player4b.*
import com.leapsy.player4b.adatper.ICallMediaPath
import com.leapsy.player4b.adatper.ISelectFileListener
import com.leapsy.player4b.adatper.browse.RecyclerViewItemDecoration
import com.leapsy.player4b.adatper.main.VideoAudioMediaViewHolder
import com.leapsy.player4b.adatper.tool.RecyclerViewScrollListener
import com.leapsy.player4b.data.MediaData
import com.leapsy.player4b.player.MyPlayer
import com.leapsy.player4b.GlobalDefine
import com.leapsy.player4b.usbHelper.CommandList
import com.leapsy.player4b.usbHelper.IConnectToDo
import com.leapsy.player4b.usbHelper.USBStatus
import com.leapsy.player4b.usbHelper.UsbSerialHelper
import com.leapsy.player4b.util.ConvertUtils
import com.leapsy.player4b.util.DiscreteScrollViewUtils
import com.leapsy.player4b.MediaLoader
import com.leapsy.player4b.VolumeHelper
import com.leapsy.widget_media_lib.IjkVideoView
import java.io.File

class MainActivity : BaseActivity() {
    companion object {
        private val TAG = MainActivity::class.java.simpleName

        /**
         * 當前是在哪一頁, include: recently, video, music, files
         */
        var browserPage : Int = 0

        /**
         * 控制器是否隱藏
         */
        private var isHidden : Boolean = false
        /**
         * 是否鎖定
         */
        private var isLocked : Boolean = false
        private var canShowLockButton : Boolean = false
    }

    /**
     * for recently, video, audio page scroll view
     */
    private lateinit var mDiscreteScrollView : DiscreteScrollView
    private lateinit var mInfiniteScrollMediaAdapter : InfiniteScrollAdapter<*>

    /**
     * 檔案瀏覽的 recycler view
     */
    private lateinit var mBrowserRecyclerView : RecyclerView

    /**
     * 切換至最近
     */
    private lateinit var mSwitchToRecentlyButton : Button
    /**
     * 切換至影片
     */
    private lateinit var mSwitchToVideoButton : Button
    /**
     * 切換至音樂
     */
    private lateinit var mSwitchToMusicButton : Button
    /**
     * 切換至檔案
     */
    private lateinit var mSwitchToBrowserButton : Button

    private var curRecentlyFocused : Int = 0
    private var curVideoFocused : Int = 0
    private var curMusicFocused : Int = 0

    /**
     * the media path by user selected
     */
    private var mSelectedMediaPath : String = ""
    /**
     * is the media path by user selected audio?
     */
    private var mIsSelectedMediaTypeIsAudio : Boolean = false

    /**
     * IJK Player Video View
     */
    private var mIjkVideoView : IjkVideoView? = null
    /**
     * 目前播放的影片是否已播完
     */
    private var mIsPlayFinished : Boolean = false
    /**
     * 影片的seek bar
     */
    private lateinit var mVideoSeekBar : SeekBar
    /**
     * 音量的seek bar
     */
    private lateinit var mVolumeSeekBar : SeekBar

    private var mMaxVolume : Int = 0

    //private lateinit var mGestureDetector : GestureDetectorHelper
    private lateinit var mVolumeHelper : VolumeHelper
    private lateinit var mUsbHelper : UsbSerialHelper
    private lateinit var mStatus : USBStatus

    private var mCur2d3dMode = 0
    private var mCanSwitch2d3d = false

    private lateinit var mLinearLayoutManager : LinearLayoutManager

    private var mHandler : Handler? = null
    private var mCurrentPlayedMediaTiming : Int = 0

    private var mShowPopTextViewHandler : Handler? = null
    private var mTotalShowPopTime : Int = 3

    private var mHideControllerHandler : Handler = Handler(Looper.getMainLooper())
    private var mHideControllerTime : Int = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activityMainBinding.root)

        //mGestureDetector = GestureDetectorHelper(this@MainActivity, mTouchEvent)

        initUi()
        initUsbHelper()
        setMediaAdapterCallback()
        initVolumeHelper()

        android.util.Log.d("xxoxox", "onCreate")
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("xxoxox", "onDestroy")

        LoaderManager.getInstance(this).destroyLoader(MediaLoader.MEDIA_STORE_LOADER_ID)

        mIjkVideoView?.release(true)

        unregisterReceiver(mStatus)
        if (!mUsbHelper.deviceNotFound()) {
            mUsbHelper.closeSerialPort()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onStart() {
        super.onStart()
        android.util.Log.d("xxoxox", "onStart")
    }

    override fun onStop() {
        super.onStop()
        android.util.Log.d("xxoxox", "onStop")
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("xxoxox", "onResume")

        setListener()

        mediaLoader.loadAllRecentlyMedia()
        setCurrentPage(GlobalDefine.MainPage.RECENTLY_PAGE)
        mUsbHelper.detectUSB()
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.d("xxoxox", "onPause")
        mIjkVideoView?.stopPlayback()
        activityMainBinding.videoViewContainerLayout.removeAllViews()
    }

    override fun onBackPressed() {
        when (GlobalDefine.MainPage.WhichPage) {
            GlobalDefine.MainPage.RECENTLY_PAGE -> { super.onBackPressed() }
            GlobalDefine.MainPage.VIDEO_PAGE -> { super.onBackPressed() }
            GlobalDefine.MainPage.MUSIC_PAGE -> { super.onBackPressed() }
            GlobalDefine.MainPage.ALL_FILES_HOME_PAGE -> { super.onBackPressed() }
            GlobalDefine.MainPage.ALL_FILES_FOLDER_ENTERED_PAGE -> {
                setCurrentPage(GlobalDefine.MainPage.ALL_FILES_HOME_PAGE)
                browseAdapter.backToBrowseRoot()
            }
            GlobalDefine.MainPage.FULL_SCREEN_PAGE -> {
                if (!isLocked) {
                    mIjkVideoView?.stopPlayback()
                    activityMainBinding.videoViewContainerLayout.removeAllViews()
                    if (mIjkVideoView != null) {
                        mIjkVideoView = null
                    }
                    mHandler?.removeCallbacks(mRunnable)
                    if (mHandler != null) {
                        mHandler = null
                    }
                    if (mShowPopTextViewHandler != null) {
                        mShowPopTextViewHandler = null
                    }
                    browseAdapter.backToBrowseRoot()
                    setCurrentPage(browserPage)
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        if (GlobalDefine.MainPage.WhichPage == GlobalDefine.MainPage.FULL_SCREEN_PAGE) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                activityMainBinding.adjustVolumeImageView.setBackgroundResource(R.drawable.volume_adjust_background_selected_shape)
                activityMainBinding.adjustVolumeSeekbarLayout.visibility = View.VISIBLE
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {

        if (GlobalDefine.MainPage.WhichPage == GlobalDefine.MainPage.FULL_SCREEN_PAGE) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                activityMainBinding.adjustVolumeImageView.setBackgroundResource(R.drawable.volume_adjust_background_selected_shape)
                activityMainBinding.adjustVolumeSeekbarLayout.visibility = View.VISIBLE
                useKeyButtonToAdjustVolume(keyCode)
                return true
            }
        }

        return super.onKeyUp(keyCode, event)
    }

    private fun initUi() {
        mDiscreteScrollView = activityMainBinding.allFilesMediaDiscreteScrollView
        mDiscreteScrollView.setOrientation(DSVOrientation.HORIZONTAL)
        mInfiniteScrollMediaAdapter = InfiniteScrollAdapter.wrap(videoAudioMediaAdapter)
        mDiscreteScrollView.adapter = mInfiniteScrollMediaAdapter
        mDiscreteScrollView.setItemTransitionTimeMillis(100)
        mDiscreteScrollView.setItemTransformer(
            ScaleTransformer.Builder()
                .setMaxScale(1.0f)
                .setMinScale(0.6f)
                .build())
        mBrowserRecyclerView = activityMainBinding.browseAllRecyclerView
        mLinearLayoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
        mBrowserRecyclerView.layoutManager = mLinearLayoutManager
        mBrowserRecyclerView.addItemDecoration(RecyclerViewItemDecoration(resources.getDimensionPixelOffset(R.dimen.dp_8)))

        // 切換至最近
        mSwitchToRecentlyButton = activityMainBinding.switchRecentlyButton
        // 切換至影片
        mSwitchToVideoButton = activityMainBinding.switchVideoButton
        // 切換至音樂
        mSwitchToMusicButton = activityMainBinding.switchMusicButton
        // 切換至檔案
        mSwitchToBrowserButton = activityMainBinding.switchAllFilesMediaButton
        // 影片的seek bar
        mVideoSeekBar = activityMainBinding.videoSeekBar
        // 音量的seek bar
        mVolumeSeekBar = activityMainBinding.volumeSeekBar
    }

    private fun initUsbHelper() {
        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction(UsbSerialHelper.USB_PERMISSION)

        mUsbHelper = UsbSerialHelper.getInstance(this@MainActivity)
        mUsbHelper.init()
        mUsbHelper.setCallback(object : IConnectToDo {
            override fun doSuccess() {
                mCanSwitch2d3d = true
                activityMainBinding.forbidImageView.visibility = View.GONE
                Toast.makeText(this@MainActivity, "USB connected.", Toast.LENGTH_SHORT).show()
            }

            override fun doFail() {
                mCanSwitch2d3d = false
                activityMainBinding.forbidImageView.visibility = View.VISIBLE
                Toast.makeText(this@MainActivity, "USB connect fail.", Toast.LENGTH_SHORT).show()
            }
        })

        mStatus = USBStatus(mUsbHelper)
        registerReceiver(mStatus, filter)
    }
    private fun initVolumeHelper() {
        mVolumeHelper = VolumeHelper.getInstance(this@MainActivity)
    }
    private fun setMediaAdapterCallback() {
        videoAudioMediaAdapter.setMediaPathCallback(object : ICallMediaPath {
            override fun onPlayMediaFromPath(aOrder : Int, aMediaPath : String) {
                MyPlayer.CURRENT_PLAYING_ORDER = aOrder
                mSelectedMediaPath = aMediaPath
                android.util.Log.d(TAG, "current: ${MyPlayer.CURRENT_PLAYING_ORDER}, selected: ${mSelectedMediaPath}")
                setCurrentPage(GlobalDefine.MainPage.FULL_SCREEN_PAGE)
                if (mHandler != null) {
                    mHandler = null
                }
                if (mShowPopTextViewHandler != null) {
                    mShowPopTextViewHandler = null
                }
                var currentPlayedName : String = ""
                initVideoViewPlayer()
                when (browserPage) {
                    GlobalDefine.MainPage.RECENTLY_PAGE -> {
                        activityMainBinding.ifCurrentMediaIsAudioLayout.visibility = if (MediaLoader.getRecentlyMediaDataList()[aOrder].aMediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO) View.VISIBLE else View.GONE
                        activityMainBinding.switch2d3dButtonLayout.visibility = if (MediaLoader.getRecentlyMediaDataList()[aOrder].aMediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) View.VISIBLE else View.GONE
                        currentPlayedName = MediaLoader.getRecentlyMediaDataList()[aOrder].aMediaDisplayName.substring(0, MediaLoader.getRecentlyMediaDataList()[aOrder].aMediaDisplayName.lastIndexOf("."))
                        if (MediaLoader.getRecentlyMediaDataList()[aOrder].aMediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO) {
                            activityMainBinding.currentPlayedMusicNameTextView.text = currentPlayedName
                            activityMainBinding.currentPlayedVideoNameTextView.text = ""
                        } else {
                            activityMainBinding.currentPlayedMusicNameTextView.text = ""
                            activityMainBinding.currentPlayedVideoNameTextView.text = currentPlayedName
                        }
                        mVideoSeekBar.max = ConvertUtils.millisecondsToSeconds(MediaLoader.getRecentlyMediaDataList()[aOrder].aMediaDuration)
                        android.util.Log.d("hahahxx", "recently max: ${mVideoSeekBar.max}")
                        activityMainBinding.currentPlayedMediaTotalTimeTextView.text = ConvertUtils.secondsToHMS(
                            ConvertUtils.millisecondsToSeconds(MediaLoader.getRecentlyMediaDataList()[aOrder].aMediaDuration))
                    }
                    GlobalDefine.MainPage.VIDEO_PAGE -> {
                        activityMainBinding.ifCurrentMediaIsAudioLayout.visibility = View.GONE
                        activityMainBinding.switch2d3dButtonLayout.visibility = View.VISIBLE
                        activityMainBinding.currentPlayedVideoNameTextView.text = MediaLoader.getVideoMediaDataList()[aOrder].aMediaDisplayName.substring(0, MediaLoader.getVideoMediaDataList()[aOrder].aMediaDisplayName.lastIndexOf("."))
                        mVideoSeekBar.max = ConvertUtils.millisecondsToSeconds(MediaLoader.getVideoMediaDataList()[aOrder].aMediaDuration)
                        android.util.Log.d("hahahxx", "video max: ${mVideoSeekBar.max}")
                        activityMainBinding.currentPlayedMediaTotalTimeTextView.text = ConvertUtils.secondsToHMS(
                            ConvertUtils.millisecondsToSeconds(MediaLoader.getVideoMediaDataList()[aOrder].aMediaDuration))
                    }
                    GlobalDefine.MainPage.MUSIC_PAGE -> {
                        activityMainBinding.ifCurrentMediaIsAudioLayout.visibility = View.VISIBLE
                        activityMainBinding.switch2d3dButtonLayout.visibility = View.GONE
                        currentPlayedName = MediaLoader.getMusicMediaDataList()[aOrder].aMediaDisplayName.substring(0, MediaLoader.getMusicMediaDataList()[aOrder].aMediaDisplayName.lastIndexOf("."))
                        activityMainBinding.currentPlayedMusicNameTextView.text = currentPlayedName
                        activityMainBinding.currentPlayedVideoNameTextView.text = ""
                        mVideoSeekBar.max = ConvertUtils.millisecondsToSeconds(MediaLoader.getMusicMediaDataList()[aOrder].aMediaDuration)
                        android.util.Log.d("hahahxx", "audio max: ${mVideoSeekBar.max}")
                        activityMainBinding.currentPlayedMediaTotalTimeTextView.text = ConvertUtils.secondsToHMS(
                            ConvertUtils.millisecondsToSeconds(MediaLoader.getMusicMediaDataList()[aOrder].aMediaDuration))
                    }
                }
                mIsPlayFinished = false
                mIjkVideoView?.setVideoPath(mSelectedMediaPath)
                mIjkVideoView?.start()
                activityMainBinding.playPauseImageView.setBackgroundResource(R.drawable.pause_button_layer_list)

                mHandler = Handler(Looper.getMainLooper())
                mHandler!!.post(mRunnable)
                mShowPopTextViewHandler = Handler(Looper.getMainLooper())
            }
        })

        browseAdapter.setMediaPathCallback(object : ISelectFileListener {
            @SuppressLint("SetTextI18n")
            override fun onFolderSelected(aPosition : Int, aBucketName : String, aNumberOfFilesInFolder : Int) {
                GlobalDefine.MainPage.WhichPage = GlobalDefine.MainPage.ALL_FILES_FOLDER_ENTERED_PAGE
                mediaLoader.enterFolder(aPosition, aBucketName, aNumberOfFilesInFolder)
                activityMainBinding.rootFolderNameTextView.text = "${File.separator}${resources.getString(R.string.browse_root)}${File.separator}${aBucketName}${File.separator}"
            }

            override fun onFileItemClicked(aPosition : Int, aMediaFileName : String) {
                activityMainBinding.ifCurrentMediaIsAudioLayout.visibility = if (mediaLoader.getFileType(aMediaFileName) == MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO) View.VISIBLE else View.GONE
                activityMainBinding.switch2d3dButtonLayout.visibility = if (mediaLoader.getFileType(aMediaFileName) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) View.VISIBLE else View.GONE
                if (mediaLoader.getFileType(aMediaFileName) == MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO) {
                    activityMainBinding.currentPlayedMusicNameTextView.text = aMediaFileName.substring(0, aMediaFileName.lastIndexOf("."))
                    activityMainBinding.currentPlayedVideoNameTextView.text = ""
                } else {
                    activityMainBinding.currentPlayedMusicNameTextView.text = ""
                    activityMainBinding.currentPlayedVideoNameTextView.text = aMediaFileName.substring(0, aMediaFileName.lastIndexOf("."))
                }
                setCurrentPage(GlobalDefine.MainPage.FULL_SCREEN_PAGE)
                if (mHandler != null) {
                    mHandler = null
                }
                if (mShowPopTextViewHandler != null) {
                    mShowPopTextViewHandler = null
                }
                if (mediaLoader.getMediaData(aMediaFileName) != null) {
                    videoAudioMediaAdapter.setReadMediaToDataBase(mediaLoader.getMediaData(aMediaFileName)!!)
                }
                initVideoViewPlayer()
                mVideoSeekBar.max = ConvertUtils.millisecondsToSeconds(mediaLoader.getMediaData(aMediaFileName)!!.aMediaDuration)
                activityMainBinding.currentPlayedMediaTotalTimeTextView.text = ConvertUtils.secondsToHMS(
                    ConvertUtils.millisecondsToSeconds(mediaLoader.getMediaData(aMediaFileName)!!.aMediaDuration))
                mIsPlayFinished = false
                mIjkVideoView?.setVideoPath(mediaLoader.getFileFullPath(aMediaFileName))
                mIjkVideoView?.start()
                activityMainBinding.playPauseImageView.setBackgroundResource(R.drawable.pause_button_layer_list)
                mHandler = Handler(Looper.getMainLooper())
                mHandler!!.post(mRunnable)
                mShowPopTextViewHandler = Handler(Looper.getMainLooper())
            }
        })
    }
    private fun initVideoViewPlayer() {
        if (mIjkVideoView != null) {
            mIjkVideoView = null
        }
        mIjkVideoView = IjkVideoView(this@MainActivity)
        mIjkVideoView?.layoutParams = MyPlayer.relativeLayoutParams()
        activityMainBinding.videoViewContainerLayout.addView(mIjkVideoView)
    }

    /**
     * set listener
     */
    private fun setListener() {
        mBrowserRecyclerView.adapter = browseAdapter
        mSwitchToRecentlyButton.setOnHoverListener(object : View.OnHoverListener{
            override fun onHover(v : View?, event : MotionEvent?) : Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_HOVER_ENTER -> {
                        if (GlobalDefine.MainPage.WhichPage != GlobalDefine.MainPage.RECENTLY_PAGE) {
                            activityMainBinding.recentlyHoveredHighLightImageView.visibility = View.VISIBLE
                        }
                    }
                    MotionEvent.ACTION_HOVER_MOVE -> {}
                    MotionEvent.ACTION_HOVER_EXIT -> { activityMainBinding.recentlyHoveredHighLightImageView.visibility = View.GONE }
                }
                return false
            }
        })
        mSwitchToRecentlyButton.setOnClickListener {
            if (GlobalDefine.MainPage.WhichPage == GlobalDefine.MainPage.MUSIC_PAGE || GlobalDefine.MainPage.WhichPage == GlobalDefine.MainPage.ALL_FILES_HOME_PAGE || GlobalDefine.MainPage.WhichPage == GlobalDefine.MainPage.VIDEO_PAGE) {
                setCurrentPage(GlobalDefine.MainPage.RECENTLY_PAGE)
                mediaLoader.loadAllRecentlyMedia()
            }
        }
        mSwitchToVideoButton.setOnHoverListener(object : View.OnHoverListener{
            override fun onHover(v : View?, event : MotionEvent?) : Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_HOVER_ENTER -> {
                        if (GlobalDefine.MainPage.WhichPage != GlobalDefine.MainPage.VIDEO_PAGE) {
                            activityMainBinding.videoHoveredHighLightImageView.visibility = View.VISIBLE
                        }
                    }
                    MotionEvent.ACTION_HOVER_MOVE -> {}
                    MotionEvent.ACTION_HOVER_EXIT -> { activityMainBinding.videoHoveredHighLightImageView.visibility = View.GONE }
                }
                return false
            }
        })
        mSwitchToVideoButton.setOnClickListener {
            if (GlobalDefine.MainPage.WhichPage == GlobalDefine.MainPage.MUSIC_PAGE || GlobalDefine.MainPage.WhichPage == GlobalDefine.MainPage.ALL_FILES_HOME_PAGE || GlobalDefine.MainPage.WhichPage == GlobalDefine.MainPage.RECENTLY_PAGE) {
                setCurrentPage(GlobalDefine.MainPage.VIDEO_PAGE)
            }
        }
        mSwitchToMusicButton.setOnHoverListener(object : View.OnHoverListener{
            override fun onHover(v : View?, event : MotionEvent?) : Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_HOVER_ENTER -> {
                        if (GlobalDefine.MainPage.WhichPage != GlobalDefine.MainPage.MUSIC_PAGE) {
                            activityMainBinding.musicHoveredHighLightImageView.visibility = View.VISIBLE
                        }
                    }
                    MotionEvent.ACTION_HOVER_MOVE -> {}
                    MotionEvent.ACTION_HOVER_EXIT -> { activityMainBinding.musicHoveredHighLightImageView.visibility = View.GONE }
                }
                return false
            }
        })
        mSwitchToMusicButton.setOnClickListener {
            if (GlobalDefine.MainPage.WhichPage == GlobalDefine.MainPage.VIDEO_PAGE || GlobalDefine.MainPage.WhichPage == GlobalDefine.MainPage.ALL_FILES_HOME_PAGE || GlobalDefine.MainPage.WhichPage == GlobalDefine.MainPage.RECENTLY_PAGE) {
                setCurrentPage(GlobalDefine.MainPage.MUSIC_PAGE)
            }
        }
        mSwitchToBrowserButton.setOnHoverListener(object : View.OnHoverListener{
            override fun onHover(v : View?, event : MotionEvent?) : Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_HOVER_ENTER -> {
                        if (GlobalDefine.MainPage.WhichPage != GlobalDefine.MainPage.ALL_FILES_HOME_PAGE) {
                            activityMainBinding.browserHoveredHighLightImageView.visibility = View.VISIBLE
                        }
                    }
                    MotionEvent.ACTION_HOVER_MOVE -> {}
                    MotionEvent.ACTION_HOVER_EXIT -> { activityMainBinding.browserHoveredHighLightImageView.visibility = View.GONE }
                }
                return false
            }
        })
        mSwitchToBrowserButton.setOnClickListener {
            if (GlobalDefine.MainPage.WhichPage == GlobalDefine.MainPage.MUSIC_PAGE || GlobalDefine.MainPage.WhichPage == GlobalDefine.MainPage.VIDEO_PAGE || GlobalDefine.MainPage.WhichPage == GlobalDefine.MainPage.RECENTLY_PAGE) {
                setCurrentPage(GlobalDefine.MainPage.ALL_FILES_HOME_PAGE)
                browseAdapter.backToBrowseRoot()
            }
        }

        activityMainBinding.scrollLeftImageButton.setOnClickListener { scrollLeft() }
        activityMainBinding.scrollRightImageButton.setOnClickListener { scrollRight() }

        RecyclerViewScrollListener.setRecyclerViewScrollEventListener(mBrowserRecyclerView, mLinearLayoutManager, activityMainBinding.scrollUpImageButton, activityMainBinding.scrollDownImageButton, activityMainBinding.scrollUpImageView, activityMainBinding.scrollDownImageView)
        activityMainBinding.scrollUpImageButton.setOnHoverListener(object : View.OnHoverListener{
            override fun onHover(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_HOVER_ENTER -> { activityMainBinding.scrollUpHoveredImageView.visibility = View.VISIBLE }
                    MotionEvent.ACTION_HOVER_MOVE -> {}
                    MotionEvent.ACTION_HOVER_EXIT -> { activityMainBinding.scrollUpHoveredImageView.visibility = View.GONE }
                }
                return false
            }
        })
        activityMainBinding.scrollDownImageButton.setOnHoverListener(object : View.OnHoverListener{
            override fun onHover(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_HOVER_ENTER -> { activityMainBinding.scrollDownHoveredImageView.visibility = View.VISIBLE }
                    MotionEvent.ACTION_HOVER_MOVE -> {}
                    MotionEvent.ACTION_HOVER_EXIT -> { activityMainBinding.scrollDownHoveredImageView.visibility = View.GONE }
                }
                return false
            }
        })
        activityMainBinding.scrollUpImageButton.setOnClickListener { RecyclerViewScrollListener.scrollUpRecyclerView(mBrowserRecyclerView, mLinearLayoutManager) }
        activityMainBinding.scrollDownImageButton.setOnClickListener { RecyclerViewScrollListener.scrollDownRecyclerView(mBrowserRecyclerView, mLinearLayoutManager) }

        // forward
        activityMainBinding.forwardButton.setOnClickListener { forwardVideo() }
        // rewind
        activityMainBinding.rewindButton.setOnClickListener { rewindVideo() }
        // video seek bar
        mVideoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar : SeekBar?, progress : Int, fromUser : Boolean) {
                if (fromUser) {
                    mCurrentPlayedMediaTiming = progress
                    android.util.Log.d("hahahxx", "當前進度為: ${mCurrentPlayedMediaTiming}")
                    mTotalShowPopTime = 3
                    mShowPopTextViewHandler?.post(mHidePopTextView)
                    activityMainBinding.popDisplayCurrentPlayedMediaTextView.visibility = View.VISIBLE
                    activityMainBinding.popDisplayCurrentPlayedMediaTextView.text = "${ConvertUtils.secondsToHMS(mCurrentPlayedMediaTiming)} ${File.separator} ${ConvertUtils.secondsToHMS(mVideoSeekBar.max)}"
                    activityMainBinding.currentPlayedMediaTimingTextView.text = ConvertUtils.secondsToHMS(mCurrentPlayedMediaTiming)
                    mIjkVideoView?.seekTo(mCurrentPlayedMediaTiming * 1000)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                android.util.Log.d(TAG, "onStartTrackingTouch")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                android.util.Log.d(TAG, "onStopTrackingTouch")
            }

        })
        // set max value of volume seek bar
        mMaxVolume = VolumeHelper.getMaxVolumeValue()
        mVolumeSeekBar.max = mMaxVolume
        mVolumeSeekBar.progress = VolumeHelper.getCurDeviceVolumeValue()
        //android.util.Log.d("#####", "onResume max:${mMaxVolume}, cur:${VolumeHelper.getCurDeviceVolumeValue()}")
        // volume seek bar
        displayCurrentVolumeValue(VolumeHelper.getCurDeviceVolumeValue())
        mVolumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar : SeekBar?, progress : Int, fromUser : Boolean) {
                if (fromUser) {
                    mVolumeHelper.getAudioManager().setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
                    displayCurrentVolumeValue(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                android.util.Log.d(TAG, "onStartTrackingTouch")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                android.util.Log.d(TAG, "onStopTrackingTouch")
            }
        })
    }

    /**
     * set current page
     */
    @SuppressLint("SetTextI18n")
    private fun setCurrentPage(aToGo : Int) {
        GlobalDefine.MainPage.WhichPage = aToGo
        setAllPageLayoutGone()
        mediaLoader.loadAllRecentlyMedia()
        when (aToGo) {
            GlobalDefine.MainPage.RECENTLY_PAGE -> {
                activityMainBinding.recentlySelectedHighLightImageView.visibility = View.VISIBLE
                browserPage = GlobalDefine.MainPage.RECENTLY_PAGE
                videoAudioMediaAdapter.setCanRecord(false)
                activityMainBinding.highLightFrameLayout.visibility = if (MediaLoader.getRecentlyMediaDataList().size > 0) View.VISIBLE else View.INVISIBLE
                activityMainBinding.containRecentlyVideoAudioLayout.visibility = if (MediaLoader.getRecentlyMediaDataList().size > 0) View.VISIBLE else View.GONE
                activityMainBinding.noAnyFileLayout.visibility = if (MediaLoader.getRecentlyMediaDataList().size > 0) View.INVISIBLE else View.VISIBLE
                activityMainBinding.noAnyFileImageView.setBackgroundResource(R.drawable.ic_img_play_empty)
                activityMainBinding.noAnyFileTitleTextView.text = resources.getString(R.string.no_recently)
                videoAudioMediaAdapter.reloadAllFilesMediaData(MediaLoader.getRecentlyMediaDataList())
                mDiscreteScrollView.addOnItemChangedListener(mItemChangedListenerCallback)
                mDiscreteScrollView.addScrollStateChangeListener(mScrollStateChangeListenerCallback)
            }
            GlobalDefine.MainPage.VIDEO_PAGE -> {
                activityMainBinding.videoSelectedHighLightImageView.visibility = View.VISIBLE
                browserPage = GlobalDefine.MainPage.VIDEO_PAGE
                videoAudioMediaAdapter.setCanRecord(true)
                activityMainBinding.highLightFrameLayout.visibility = if (MediaLoader.getVideoMediaDataList().size > 0) View.VISIBLE else View.INVISIBLE
                activityMainBinding.containRecentlyVideoAudioLayout.visibility = if (MediaLoader.getVideoMediaDataList().size > 0) View.VISIBLE else View.GONE
                activityMainBinding.noAnyFileLayout.visibility = if (MediaLoader.getVideoMediaDataList().size > 0) View.INVISIBLE else View.VISIBLE
                activityMainBinding.noAnyFileImageView.setBackgroundResource(R.drawable.ic_img_movie_empty)
                activityMainBinding.noAnyFileTitleTextView.text = resources.getString(R.string.no_my_video)
                mDiscreteScrollView.visibility = if (MediaLoader.getVideoMediaDataList().size > 0) View.VISIBLE else View.INVISIBLE
                videoAudioMediaAdapter.reloadAllFilesMediaData(MediaLoader.getVideoMediaDataList())
                mDiscreteScrollView.addOnItemChangedListener(mItemChangedListenerCallback)
                mDiscreteScrollView.addScrollStateChangeListener(mScrollStateChangeListenerCallback)
            }
            GlobalDefine.MainPage.MUSIC_PAGE -> {
                activityMainBinding.musicSelectedHighLightImageView.visibility = View.VISIBLE
                browserPage = GlobalDefine.MainPage.MUSIC_PAGE
                videoAudioMediaAdapter.setCanRecord(true)
                activityMainBinding.highLightFrameLayout.visibility = if (MediaLoader.getMusicMediaDataList().size > 0) View.VISIBLE else View.INVISIBLE
                activityMainBinding.containRecentlyVideoAudioLayout.visibility = if (MediaLoader.getMusicMediaDataList().size > 0) View.VISIBLE else View.GONE
                activityMainBinding.noAnyFileLayout.visibility = if (MediaLoader.getMusicMediaDataList().size > 0) View.INVISIBLE else View.VISIBLE
                activityMainBinding.noAnyFileImageView.setBackgroundResource(R.drawable.ic_img_music_empty)
                activityMainBinding.noAnyFileTitleTextView.text = resources.getString(R.string.no_my_music)
                videoAudioMediaAdapter.reloadAllFilesMediaData(MediaLoader.getMusicMediaDataList())
                mDiscreteScrollView.addOnItemChangedListener(mItemChangedListenerCallback)
                mDiscreteScrollView.addScrollStateChangeListener(mScrollStateChangeListenerCallback)
            }
            GlobalDefine.MainPage.ALL_FILES_HOME_PAGE -> {
                browserPage = GlobalDefine.MainPage.ALL_FILES_HOME_PAGE
                activityMainBinding.containBrowseLayout.visibility = View.VISIBLE
                activityMainBinding.browserSelectedHighLightImageView.visibility = View.VISIBLE
                activityMainBinding.rootFolderNameTextView.visibility = View.VISIBLE
                activityMainBinding.rootFolderNameTextView.text = "${File.separator}${resources.getString(R.string.browse_root)}${File.separator}"
                videoAudioMediaAdapter.setCanRecord(true)
                activityMainBinding.highLightFrameLayout.visibility = View.INVISIBLE
                activityMainBinding.noAnyFileLayout.visibility = if (MediaLoader.getAllFilesMediaDataList().size > 0) View.INVISIBLE else View.VISIBLE
                activityMainBinding.noAnyFileImageView.setBackgroundResource(R.drawable.ic_img_list_empty)
                activityMainBinding.noAnyFileTitleTextView.text = resources.getString(R.string.no_my_files)

                android.util.Log.d("lalala", "${mBrowserRecyclerView.scrollBarSize}, ${mBrowserRecyclerView.scrollState}, ${mBrowserRecyclerView.scrollY}")
            }
            GlobalDefine.MainPage.FULL_SCREEN_PAGE -> {
                activityMainBinding.videoControllerLayout.visibility = View.VISIBLE
                activityMainBinding.fullScreenMediaLayout.visibility = View.VISIBLE
                activityMainBinding.playPauseButton.setOnHoverListener(object : View.OnHoverListener {
                    override fun onHover(v : View?, event : MotionEvent?): Boolean {
                        when (event?.action) {
                            MotionEvent.ACTION_HOVER_ENTER -> { activityMainBinding.playPauseHoveredImageView.visibility = View.VISIBLE }
                            MotionEvent.ACTION_HOVER_MOVE -> {}
                            MotionEvent.ACTION_HOVER_EXIT -> { activityMainBinding.playPauseHoveredImageView.visibility = View.GONE }
                        }
                        return false
                    }
                })
                activityMainBinding.playPauseButton.setOnClickListener {
                    if (mIjkVideoView?.isPlaying == true) {
                        mIjkVideoView?.pause()
                        activityMainBinding.playPauseImageView.setBackgroundResource(R.drawable.play_button_layer_list)
                    } else {
                        if (mIsPlayFinished) {
                            mCurrentPlayedMediaTiming = 0
                            mVideoSeekBar.progress = mCurrentPlayedMediaTiming
                            activityMainBinding.currentPlayedMediaTimingTextView.text = "00:00"
                            activityMainBinding.currentPlayedMusicTimingTextView.text = "00:00"
                        }
                        mIjkVideoView?.start()
                        activityMainBinding.playPauseImageView.setBackgroundResource(R.drawable.pause_button_layer_list)
                    }
                }
                activityMainBinding.rewindButton.setOnHoverListener(object : View.OnHoverListener {
                    override fun onHover(v : View?, event : MotionEvent?): Boolean {
                        when (event?.action) {
                            MotionEvent.ACTION_HOVER_ENTER -> { activityMainBinding.rewindHoveredImageView.visibility = View.VISIBLE }
                            MotionEvent.ACTION_HOVER_MOVE -> {}
                            MotionEvent.ACTION_HOVER_EXIT -> { activityMainBinding.rewindHoveredImageView.visibility = View.GONE }
                        }
                        return false
                    }
                })
                activityMainBinding.forwardButton.setOnHoverListener(object : View.OnHoverListener {
                    override fun onHover(v : View?, event : MotionEvent?): Boolean {
                        when (event?.action) {
                            MotionEvent.ACTION_HOVER_ENTER -> { activityMainBinding.forwardHoveredImageView.visibility = View.VISIBLE }
                            MotionEvent.ACTION_HOVER_MOVE -> {}
                            MotionEvent.ACTION_HOVER_EXIT -> { activityMainBinding.forwardHoveredImageView.visibility = View.GONE }
                        }
                        return false
                    }
                })
                activityMainBinding.adjustVolumeButton.setOnHoverListener(object : View.OnHoverListener {
                    override fun onHover(v : View?, event : MotionEvent?): Boolean {
                        when (event?.action) {
                            MotionEvent.ACTION_HOVER_ENTER -> { activityMainBinding.adjustVolumeHoveredImageView.visibility = View.VISIBLE }
                            MotionEvent.ACTION_HOVER_MOVE -> {}
                            MotionEvent.ACTION_HOVER_EXIT -> { activityMainBinding.adjustVolumeHoveredImageView.visibility = View.GONE }
                        }
                        return false
                    }
                })
                activityMainBinding.adjustVolumeButton.setOnClickListener {
                    activityMainBinding.adjustVolumeImageView.setBackgroundResource(if (activityMainBinding.adjustVolumeSeekbarLayout.visibility == View.VISIBLE) R.drawable.volume_adjust_background_default_shape else R.drawable.volume_adjust_background_selected_shape)
                    activityMainBinding.adjustVolumeSeekbarLayout.visibility = if (activityMainBinding.adjustVolumeSeekbarLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                }
                activityMainBinding.switch2d3dButton.setOnHoverListener(object : View.OnHoverListener {
                    override fun onHover(v : View?, event : MotionEvent?): Boolean {
                        when (event?.action) {
                            MotionEvent.ACTION_HOVER_ENTER -> {
                                if (mCanSwitch2d3d) {
                                    activityMainBinding.switch2d3dHoveredImageView.visibility = View.VISIBLE
                                }
                            }
                            MotionEvent.ACTION_HOVER_MOVE -> {}
                            MotionEvent.ACTION_HOVER_EXIT -> { activityMainBinding.switch2d3dHoveredImageView.visibility = View.GONE }
                        }
                        return false
                    }
                })
                activityMainBinding.switch2d3dButton.setOnClickListener {
                    if (mCanSwitch2d3d) {
                        val nextMode : Int = mCur2d3dMode + 1
                        val setValue : String = CommandList.sendSetCommand(CommandList.SET_LIST[2], (nextMode % 2).toString())
                        if (mUsbHelper.getOpenSerialPort()) {
                            android.util.Log.d(TAG, "port is open")
                            val result : String = mUsbHelper.sendValue(setValue)
                            android.util.Log.d(TAG, "command send, result=${result}")
                            if (result == setValue) {
                                if (mIjkVideoView != null) mIjkVideoView?.pause()
                                mCur2d3dMode = nextMode
                                activityMainBinding.switch2d3dButton.setBackgroundResource(if (nextMode % 2 == 0) R.drawable.ic_btn_2d_view else R.drawable.ic_btn_3d_view)
                            } else {
                                Toast.makeText(this@MainActivity, "Send failure, result=$result", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            android.util.Log.e(TAG, "send failure, SetCommand:${setValue}")
                        }
                    }
                }

                activityMainBinding.showHideVideoController0Button.setOnClickListener {
                    isHidden = !isHidden
                    hideControllerPanel(isHidden, isLocked)
                }
                activityMainBinding.showHideVideoController1Button.setOnClickListener {
                    isHidden = !isHidden
                    hideControllerPanel(isHidden, isLocked)
                }

                activityMainBinding.lockButton.setOnHoverListener(object : View.OnHoverListener {
                    override fun onHover(v : View?, event : MotionEvent?): Boolean {
                        when (event?.action) {
                            MotionEvent.ACTION_HOVER_ENTER -> { activityMainBinding.lockHoveredImageView.visibility = View.VISIBLE }
                            MotionEvent.ACTION_HOVER_MOVE -> {}
                            MotionEvent.ACTION_HOVER_EXIT -> { activityMainBinding.lockHoveredImageView.visibility = View.GONE }
                        }
                        return false
                    }
                })
                activityMainBinding.lockButton.setOnClickListener {
                    isLocked = !isLocked
                    isHidden = false
                    hideControllerPanel(isHidden, isLocked)
                    lockTheScreen(isLocked)
                }
            }
        }
    }
    @SuppressLint("SetTextI18n")
    private fun setAllPageLayoutGone() {
        videoAudioMediaAdapter.setCanRecord(false)
        activityMainBinding.fullScreenMediaLayout.visibility = View.GONE

        activityMainBinding.recentlySelectedHighLightImageView.visibility = View.GONE
        activityMainBinding.videoSelectedHighLightImageView.visibility = View.GONE
        activityMainBinding.musicSelectedHighLightImageView.visibility = View.GONE
        activityMainBinding.browserSelectedHighLightImageView.visibility = View.GONE

        activityMainBinding.containRecentlyVideoAudioLayout.visibility = View.GONE
        activityMainBinding.containBrowseLayout.visibility = View.GONE
        activityMainBinding.rootFolderNameTextView.visibility = View.GONE

        mDiscreteScrollView.removeScrollStateChangeListener(mScrollStateChangeListenerCallback)
        mDiscreteScrollView.removeItemChangedListener(mItemChangedListenerCallback)
        mDiscreteScrollView.scrollToPosition(0)
        MyPlayer.CURRENT_PLAYING_ORDER = 0
        mCurrentPlayedMediaTiming = 0
        activityMainBinding.currentPlayedMediaTimingTextView.text = "00:00"
        activityMainBinding.currentPlayedMusicTimingTextView.text = "00:00"
        mVideoSeekBar.progress = 0
        mIsPlayFinished = false

        activityMainBinding.adjustVolumeImageView.setBackgroundResource(R.drawable.volume_adjust_background_default_shape)
        activityMainBinding.adjustVolumeSeekbarLayout.visibility = View.GONE
    }
    /**
     * 隱藏控制器 or lock button
     */
    private fun hideControllerPanel(aHideController : Boolean, aHideLockButton : Boolean) {
        activityMainBinding.videoControllerLayout.visibility = if (aHideController) View.GONE else View.VISIBLE
        activityMainBinding.lockButtonLayout.visibility = if (aHideController) View.GONE else View.VISIBLE

        activityMainBinding.fullScreenLockImageView.visibility = if (aHideLockButton) View.VISIBLE else View.GONE
        activityMainBinding.lockImageView.setBackgroundResource(if (aHideLockButton) R.drawable.locked_layer_list else R.drawable.unlock_layer_list)
    }
    private fun lockTheScreen(aIsLocked : Boolean) {
        if (aIsLocked) {
            mHideControllerHandler.post(mHideControllerRunnable)
        } else {
            mHideControllerHandler.removeCallbacks(mHideControllerRunnable)
            mHideControllerTime = 3
            canShowLockButton = false
        }
    }

    /**
     * display current volume value
     * @param aCurVolume current volume
     */
    @SuppressLint("SetTextI18n")
    private fun displayCurrentVolumeValue(aCurVolume: Int) {
        val curVolumeValue = (100 * aCurVolume) / mMaxVolume
        if (aCurVolume == mMaxVolume) {
            activityMainBinding.currentVolumeTextView.text = "100"
        } else if (aCurVolume < 1) {
            activityMainBinding.currentVolumeTextView.text = "0"
        } else {
            activityMainBinding.currentVolumeTextView.text = curVolumeValue.toString()
        }

        if (curVolumeValue > 60) {
            activityMainBinding.adjustVolumeImageView.setImageResource(R.drawable.ic_btn_volume_up)
        } else if (curVolumeValue < 1) {
            activityMainBinding.adjustVolumeImageView.setImageResource(R.drawable.ic_btn_volume_mute)
        } else {
            activityMainBinding.adjustVolumeImageView.setImageResource(R.drawable.ic_btn_volume_down)
        }
    }
    /**
     * 直接按音量鍵調整音量
     */
    private fun useKeyButtonToAdjustVolume(aKeyValue : Int) {
        when (aKeyValue) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (mVolumeSeekBar.progress + 1 < mVolumeSeekBar.max) {
                    mVolumeSeekBar.progress ++
                } else {
                    mVolumeSeekBar.progress = mVolumeSeekBar.max
                }
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (mVolumeSeekBar.progress - 1 < 0) {
                    mVolumeSeekBar.progress = 0
                } else {
                    mVolumeSeekBar.progress --
                }
            }
        }
        mVolumeHelper.getAudioManager().setStreamVolume(AudioManager.STREAM_MUSIC, mVolumeSeekBar.progress, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
        android.util.Log.d("#####", "after max:${mMaxVolume}, cur:${mVolumeSeekBar.progress}")
        displayCurrentVolumeValue(mVolumeSeekBar.progress)
    }

    /**
     * 於最近 影片 音樂 這頁面 向右捲
     */
    private fun scrollRight() {
        when (GlobalDefine.MainPage.WhichPage) {
            GlobalDefine.MainPage.RECENTLY_PAGE -> {
                curRecentlyFocused --
                if (curRecentlyFocused < 0) {
                    curRecentlyFocused = MediaLoader.getRecentlyMediaDataList().size - 1
                }
                DiscreteScrollViewUtils.customSmoothScrollTo(mDiscreteScrollView, curRecentlyFocused)
            }

            GlobalDefine.MainPage.VIDEO_PAGE -> {
                curVideoFocused --
                if (curVideoFocused < 0) {
                    curVideoFocused = MediaLoader.getVideoMediaDataList().size - 1
                }
                DiscreteScrollViewUtils.customSmoothScrollTo(mDiscreteScrollView, curVideoFocused)
            }

            GlobalDefine.MainPage.MUSIC_PAGE -> {
                curMusicFocused --
                if (curMusicFocused < 0) {
                    curMusicFocused = MediaLoader.getMusicMediaDataList().size - 1
                }
                DiscreteScrollViewUtils.customSmoothScrollTo(mDiscreteScrollView, curMusicFocused)
            }
        }
    }
    /**
     * 於最近 影片 音樂 這頁面 向左捲
     */
    private fun scrollLeft() {
        when (GlobalDefine.MainPage.WhichPage) {
            GlobalDefine.MainPage.RECENTLY_PAGE -> {
                curRecentlyFocused ++
                if (curRecentlyFocused == MediaLoader.getRecentlyMediaDataList().size) {
                    curRecentlyFocused = 0
                }
                DiscreteScrollViewUtils.customSmoothScrollTo(mDiscreteScrollView, curRecentlyFocused)
            }

            GlobalDefine.MainPage.VIDEO_PAGE -> {
                curVideoFocused ++
                if (curVideoFocused == MediaLoader.getVideoMediaDataList().size) {
                    curVideoFocused = 0
                }
                DiscreteScrollViewUtils.customSmoothScrollTo(mDiscreteScrollView, curVideoFocused)
            }

            GlobalDefine.MainPage.MUSIC_PAGE -> {
                curMusicFocused ++
                if (curMusicFocused == MediaLoader.getMusicMediaDataList().size) {
                    curMusicFocused = 0
                }
                DiscreteScrollViewUtils.customSmoothScrollTo(mDiscreteScrollView, curMusicFocused)
            }
        }
    }

    private val mScrollStateChangeListenerCallback : DiscreteScrollView.ScrollStateChangeListener<VideoAudioMediaViewHolder> = object : DiscreteScrollView.ScrollStateChangeListener<VideoAudioMediaViewHolder> {
        override fun onScrollStart(currentItemHolder: VideoAudioMediaViewHolder, adapterPosition: Int) {
            activityMainBinding.highLightFrameLayout.visibility = View.INVISIBLE
        }

        override fun onScrollEnd(currentItemHolder: VideoAudioMediaViewHolder, adapterPosition: Int) {
            activityMainBinding.highLightFrameLayout.visibility = View.VISIBLE
        }

        override fun onScroll(scrollPosition: Float, currentPosition: Int, newPosition: Int, currentHolder: VideoAudioMediaViewHolder?, newCurrent: VideoAudioMediaViewHolder?) {
            activityMainBinding.highLightFrameLayout.visibility = View.INVISIBLE
        }
    }
    /**
     * the event listener of scroll view
     */
    private val mItemChangedListenerCallback : DiscreteScrollView.OnItemChangedListener<VideoAudioMediaViewHolder> = object : DiscreteScrollView.OnItemChangedListener<VideoAudioMediaViewHolder> {
        override fun onCurrentItemChanged(viewHolder : VideoAudioMediaViewHolder?, adapterPosition: Int) {
            when (GlobalDefine.MainPage.WhichPage) {
                GlobalDefine.MainPage.RECENTLY_PAGE -> {
                    curRecentlyFocused = mInfiniteScrollMediaAdapter.getRealPosition(adapterPosition)
                    onItemFocusedChanged(MediaLoader.getRecentlyMediaDataList()[curRecentlyFocused])
                }
                GlobalDefine.MainPage.VIDEO_PAGE -> {
                    curVideoFocused = mInfiniteScrollMediaAdapter.getRealPosition(adapterPosition)
                    onItemFocusedChanged(MediaLoader.getVideoMediaDataList()[curVideoFocused])
                }
                GlobalDefine.MainPage.MUSIC_PAGE -> {
                    curMusicFocused = mInfiniteScrollMediaAdapter.getRealPosition(adapterPosition)
                    onItemFocusedChanged(MediaLoader.getMusicMediaDataList()[curMusicFocused])
                }
            }
        }
    }
    private fun onItemFocusedChanged(aFocusedItem : MediaData) {
        activityMainBinding.focusedMediaNameTextView.text = ConvertUtils.secondsToHMS(ConvertUtils.millisecondsToSeconds(aFocusedItem.aMediaDuration))
    }


    /**
     * forward video
     */
    @SuppressLint("SetTextI18n")
    private fun forwardVideo() {
        if (mCurrentPlayedMediaTiming + 10 <= mVideoSeekBar.max) {
            mCurrentPlayedMediaTiming += 10
            mVideoSeekBar.progress = mCurrentPlayedMediaTiming
            mIjkVideoView?.seekTo(mCurrentPlayedMediaTiming * 1000)
        }
        mTotalShowPopTime = 3
        activityMainBinding.popDisplayCurrentPlayedMediaTextView.visibility = View.VISIBLE
        activityMainBinding.popDisplayCurrentPlayedMediaTextView.text = "${ConvertUtils.secondsToHMS(mCurrentPlayedMediaTiming)} ${File.separator} ${ConvertUtils.secondsToHMS(mVideoSeekBar.max)}"
        mShowPopTextViewHandler?.post(mHidePopTextView)
    }
    /**
     * rewind video
     */
    @SuppressLint("SetTextI18n")
    private fun rewindVideo() {
        if (mCurrentPlayedMediaTiming - 10 >= 0) {
            mCurrentPlayedMediaTiming -= 10
            mVideoSeekBar.progress = mCurrentPlayedMediaTiming
            mIjkVideoView?.seekTo(mCurrentPlayedMediaTiming * 1000)

        } else {
            mCurrentPlayedMediaTiming = 0
            mVideoSeekBar.progress = 0
            mIjkVideoView?.seekTo(0)
        }
        mTotalShowPopTime = 3
        activityMainBinding.popDisplayCurrentPlayedMediaTextView.visibility = View.VISIBLE
        activityMainBinding.popDisplayCurrentPlayedMediaTextView.text = "${ConvertUtils.secondsToHMS(mCurrentPlayedMediaTiming)} ${File.separator} ${ConvertUtils.secondsToHMS(mVideoSeekBar.max)}"
        mShowPopTextViewHandler?.post(mHidePopTextView)
    }

    /**
     * display current timing
     */
    private val mRunnable: Runnable = object : Runnable {
        override fun run() {
            if (mHandler != null) {
                //android.util.Log.d("hahahxx", "max: ${mVideoSeekBar.max}")
                mHandler!!.postDelayed(this, 1000)
                //android.util.Log.d("hahahxx", "isPlaying: ${mIjkVideoView?.isPlaying}")
                if (mIjkVideoView?.isPlaying == true) {
                    mIsPlayFinished = false
                    if (mVideoSeekBar.max < 1) {
                        mVideoSeekBar.max = ConvertUtils.millisecondsToSeconds(mIjkVideoView?.duration!!.toLong())
                        mCurrentPlayedMediaTiming++
                        activityMainBinding.currentPlayedMediaTotalTimeTextView.text = ConvertUtils.secondsToHMS(mVideoSeekBar.max)
                        activityMainBinding.currentPlayedMediaTimingTextView.text = ConvertUtils.secondsToHMS(mCurrentPlayedMediaTiming)
                        mVideoSeekBar.progress = mCurrentPlayedMediaTiming
                    } else {
                        mCurrentPlayedMediaTiming++
                        //android.util.Log.d("hahahxx", "current: " + mCurrentPlayedMediaTiming);
                        if (mCurrentPlayedMediaTiming <= mVideoSeekBar.max) {
                            //android.util.Log.d("hahahxx", "current: ${ConvertUtils.secondsToHMS(mCurrentPlayedMediaTiming)}");
                            activityMainBinding.currentPlayedMediaTimingTextView.text = ConvertUtils.secondsToHMS(mCurrentPlayedMediaTiming)
                            activityMainBinding.currentPlayedMusicTimingTextView.text = ConvertUtils.secondsToHMS(mCurrentPlayedMediaTiming)
                            mVideoSeekBar.progress = mCurrentPlayedMediaTiming
                        }
                    }
                } else if (mIjkVideoView?.isPlaying == false && mCurrentPlayedMediaTiming >= mVideoSeekBar.max - 1) {
                    //mIjkVideoView?.stopPlayback()
                    mIsPlayFinished = true
                    activityMainBinding.playPauseImageView.setBackgroundResource(R.drawable.play_button_layer_list)
                }
            }
        }
    }
    private val mHidePopTextView : Runnable = object : Runnable {
        override fun run() {
            mShowPopTextViewHandler?.postDelayed(this, 1000)
            mTotalShowPopTime--
            if (mTotalShowPopTime < 0) {
                activityMainBinding.popDisplayCurrentPlayedMediaTextView.visibility = View.GONE
                mTotalShowPopTime = 3
                mShowPopTextViewHandler?.removeCallbacks(this)
            }
        }
    }
    private val mHideControllerRunnable : Runnable = object : Runnable {
        override fun run() {
            mHideControllerHandler.postDelayed(this, 1000)
            mHideControllerTime--
            if (mHideControllerTime < 0) {
                isHidden = true
                hideControllerPanel(isHidden, true)
                activityMainBinding.lockButtonLayout.visibility = View.GONE
                canShowLockButton = true
                activityMainBinding.fullScreenLockImageView.setOnClickListener { showLockButton() }
                mHideControllerTime = 3
                mHideControllerHandler.removeCallbacks(this)
            }
        }
    }
    /**
     * 顯示 lock 按鈕
     */
    private fun showLockButton() {
        if (canShowLockButton) {
            activityMainBinding.lockButtonLayout.visibility = View.VISIBLE
            mHideControllerHandler.post(mHideControllerRunnable)
        }
    }
}