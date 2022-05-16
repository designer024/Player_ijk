package com.leapsy.player4b

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener

class GestureDetectorHelper(private val context: Context, private val mTouchEvent: TouchEvent) {
    private val mTouchListener: TouchListener
    private val mGestureDetector: GestureDetector
    val onTouchListener: OnTouchListener
        get() = mTouchListener

    private inner class TouchListener : OnTouchListener {
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            if (mGestureDetector.onTouchEvent(event)) return true
            if (MotionEvent.ACTION_UP == event.action and MotionEvent.ACTION_MASK) {
                mTouchEvent.endGesture()
                return true
            }
            return view.performClick()
        }
    }

    internal inner class SimpleGestureListener : GestureDetector.OnDoubleTapListener {
        override fun onSingleTapConfirmed(motionEvent: MotionEvent): Boolean {
            mTouchEvent.onSingleTapUp()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val dm = context.resources.displayMetrics
            val windowWidth = dm.widthPixels
            if (e.rawX > windowWidth * 3.0 / 5) { //Right
                mTouchEvent.onProgressFastForward()
                return true
            }
            if (e.rawX < windowWidth / 3.0) { //Left
                mTouchEvent.onProgressRewind()
                return true
            }
            return true
        }

        override fun onDoubleTapEvent(motionEvent: MotionEvent): Boolean {
            return false
        }
    }

    internal inner class GestureListener : GestureDetector.OnGestureListener {
        override fun onDown(motionEvent: MotionEvent): Boolean {
            return false
        }

        override fun onShowPress(motionEvent: MotionEvent) {}
        override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
            return false
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, v: Float, v1: Float): Boolean {
            val mOldX = e1.x
            val mOldY = e1.y
            val x = e2.rawX.toInt()
            val y = e2.rawY.toInt()
            val dm = context.resources.displayMetrics
            val windowWidth = dm.widthPixels
            val windowHeight = dm.heightPixels
            if (mOldX > windowWidth * 4.0 / 5) { //Right slide
                mTouchEvent.onVolumeSlide((mOldY - y) / windowHeight)
                return true
            }
            if (mOldX < windowWidth / 5.0) { //Left slide
                mTouchEvent.onBrightnessSlide((mOldY - y) / windowHeight)
                return true
            }
            if (mOldY > windowHeight / 2.0) {
                mTouchEvent.onProgressSeek((x - mOldX) / windowWidth)
            }
            return false
        }

        override fun onLongPress(motionEvent: MotionEvent) {}
        override fun onFling(
            motionEvent: MotionEvent,
            motionEvent1: MotionEvent,
            v: Float,
            v1: Float
        ): Boolean {
            return false
        }
    }

    interface TouchEvent {
        fun onVolumeSlide(percent: Float)
        fun onBrightnessSlide(percent: Float)
        fun onProgressSeek(percent: Float)
        fun onProgressFastForward()
        fun onProgressRewind()
        fun onSingleTapUp()
        fun endGesture()
    }

    init {
        mTouchListener = TouchListener()
        mGestureDetector = GestureDetector(context, GestureListener())
        mGestureDetector.setOnDoubleTapListener(SimpleGestureListener())
    }
}