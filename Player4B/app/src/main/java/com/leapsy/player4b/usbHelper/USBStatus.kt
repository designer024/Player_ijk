package com.leapsy.player4b.usbHelper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.text.TextUtils

class USBStatus(aUsbHelper : UsbSerialHelper) : BroadcastReceiver() {

    private var mUsbHelper : UsbSerialHelper = aUsbHelper

    override fun onReceive(aContext : Context?, aIntent : Intent?) {
        val action: String? = aIntent?.action
        if (!TextUtils.isEmpty(action)) {
            when (action) {
                UsbSerialHelper.USB_PERMISSION -> {
                    android.util.Log.d("xxoxox", "0000")
                    if (mUsbHelper.deviceNotFound()) {
                        return
                    }
                    android.util.Log.d("xxoxox", "0001")
                    var portOpen : Boolean = false
                    var hasPermission : Boolean = mUsbHelper.hasPermission()
                    if (hasPermission) {
                        android.util.Log.d("xxoxox", "is port open: " + mUsbHelper.getOpenSerialPort())
                    } else {
                        mUsbHelper.getPermission(aContext)
                        android.util.Log.d("xxoxox", "no permission")
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> mUsbHelper.detectUSB()
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {}
            }
        }
    }
}