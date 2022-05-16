package com.leapsy.player4b.permissionHelper

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.ArrayList

class PermissionHelper : FragmentActivity() {
    companion object {
        private const val TAG : String = "PermissionHelper"

        const val PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 100

        fun requestPermissions(aContext : Context, aPermissions : Array<String>, aRequestCode : Int) : Boolean {
            val requestPermissionNames = ArrayList<String>()
            for (permission in aPermissions) {
                if (ContextCompat.checkSelfPermission(aContext, permission) == PackageManager.PERMISSION_GRANTED) {
                    android.util.Log.d(TAG, "Already Granted")
                } else {
                    requestPermissionNames.add(permission)
                }
            }

            if (requestPermissionNames.size > 0) {
                val permissionNames = arrayOfNulls<String>(requestPermissionNames.size)
                requestPermissionNames.toArray(permissionNames)
                ActivityCompat.requestPermissions(aContext as Activity, permissionNames, aRequestCode)
                return false
            }
            return true
        }
    }
}