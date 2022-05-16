package com.leapsy.player4b

import android.content.Context
import android.media.AudioManager
import com.leapsy.player4b.tool.SingletonHolder

class VolumeHelper private constructor(aContext : Context) {
    private var mContext : Context = aContext

    /**
     * 控制音量的
     */
    private var mAudioManager : AudioManager = aContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    /**
     * 控制音量的
     */
    fun getAudioManager(): AudioManager { return mAudioManager }

    init {
        mMaxVolumeValue = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        mCurDeviceVolumeValue = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }
    companion object : SingletonHolder<VolumeHelper, Context>(::VolumeHelper) {
        /**
         * 音量之最大值
         */
        private var mMaxVolumeValue = 0

        /**
         * 音量之最大值
         */
        fun getMaxVolumeValue(): Int {
            return mMaxVolumeValue
        }

        /**
         * 當前音量之值
         */
        private var mCurDeviceVolumeValue = 0

        /**
         * 當前音量之值
         */
        fun getCurDeviceVolumeValue(): Int {
            return mCurDeviceVolumeValue
        }
    }
}