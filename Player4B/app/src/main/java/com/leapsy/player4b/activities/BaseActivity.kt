package com.leapsy.player4b.activities

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.loader.app.LoaderManager
import com.leapsy.player4b.MediaLoader
import com.leapsy.player4b.adatper.browse.BrowseAdapter
import com.leapsy.player4b.adatper.main.VideoAudioMediaAdapter
import com.leapsy.player4b.permissionHelper.PermissionHelper
import com.leapsy.player4b.databinding.ActivityMainBinding

open class BaseActivity : AppCompatActivity() {
    companion object {
        /**權限宣告REQUEST_CODE */
        private const val PERMISSIONS_REQUEST_CODE = 0
        private val RequestPermissionList = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    protected lateinit var activityMainBinding : ActivityMainBinding

    protected lateinit var mediaLoader : MediaLoader

    protected lateinit var videoAudioMediaAdapter: VideoAudioMediaAdapter
    protected lateinit var browseAdapter: BrowseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)

        videoAudioMediaAdapter = VideoAudioMediaAdapter(this@BaseActivity)
        browseAdapter = BrowseAdapter(this@BaseActivity)

        mediaLoader = MediaLoader(this@BaseActivity, videoAudioMediaAdapter, browseAdapter)

        browseAdapter.setMediaLoader(mediaLoader)
    }

    override fun onDestroy() {
        super.onDestroy()

        LoaderManager.getInstance(this).destroyLoader(MediaLoader.MEDIA_STORE_LOADER_ID)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionHelper.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                LoaderManager.getInstance(this).initLoader(MediaLoader.MEDIA_STORE_LOADER_ID, null, mediaLoader)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onStart() {
        super.onStart()

        //checkManagerExternalStoragePermission()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onResume() {
        super.onResume()

        if (PermissionHelper.requestPermissions(this@BaseActivity, RequestPermissionList, PermissionHelper.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)) {
            //LoaderManager.getInstance(this).initLoader(MediaLoader.MEDIA_STORE_LOADER_ID, null, mediaLoader)
            LoaderManager.getInstance(this).restartLoader(MediaLoader.MEDIA_STORE_LOADER_ID, null, mediaLoader)
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                //window.setDecorFitsSystemWindows(false)
                window.insetsController?.let {
                    it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                // Enables regular immersive mode.
                // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
                // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
            }
        }
    }

    /**
     * check MANAGE_EXTERNAL_STORAGE permission
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkManagerExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {

                val builder = AlertDialog.Builder(this)
                    .setMessage("Need MANAGE_EXTERNAL_STORAGE permission")
                    .setPositiveButton("Confirm") {
                            _, _ -> startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    }
                    .show()
            }
        }
    }
}