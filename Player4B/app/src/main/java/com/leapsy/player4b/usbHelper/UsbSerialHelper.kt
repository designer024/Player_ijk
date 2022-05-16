package com.leapsy.player4b.usbHelper

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.text.TextUtils
import com.hoho.android.usbserial.driver.*
import com.leapsy.player4b.tool.SingletonHolder
import java.io.IOException

class UsbSerialHelper private constructor(aContext : Context) {
    companion object : SingletonHolder<UsbSerialHelper, Context>(::UsbSerialHelper) {
        private val TAG : String = UsbSerialHelper::class.java.simpleName
        private const val ACTION_USB_PERMISSION : String = "com.android.example.USB_PERMISSION"

        const val USB_PERMISSION : String = "USB_DEVICE_2D3D"
    }

    private var mContext : Context = aContext

    private lateinit var mUsbManager: UsbManager
    fun getManager() : UsbManager { return mUsbManager }

    private var mDrivers : MutableList<UsbSerialDriver> = ArrayList()

    private var mUsbDevice : UsbDevice? = null
    fun getUsbDevice() : UsbDevice? {
        return if (getUsbSerialDriver() == null) null else getUsbSerialDriver()?.device
    }

    private var mIConnectCallback : IConnectToDo? = null
    fun setCallback(aIConnectCallback : IConnectToDo?) {
        mIConnectCallback = aIConnectCallback
    }

    fun init() {
        mUsbManager = mContext.applicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
        detectUSB()
    }

    fun deviceNotFound() : Boolean { return mDrivers.size == 0 }
    fun hasPermission(): Boolean {
        //return mUsbManager.hasPermission(mDrivers[0].device)
        return mUsbManager.hasPermission(getSTM32())
    }
    fun getPermission(aContext : Context?) {
        if (getUsbDevice() == null) return
        if (PendingIntent.getBroadcast(aContext, 0, Intent(USB_PERMISSION), 0) != null) {
            mUsbManager.requestPermission(getUsbDevice(), PendingIntent.getBroadcast(aContext, 0, Intent(
                USB_PERMISSION
            ), 0))
        }
    }

    private fun getUsbSerialDriver() : UsbSerialDriver? {
        // Open a connection to the first available driver.
        //return if (mDrivers.size > 0) mDrivers[0] else null
        return if (mDrivers.size > 0) getSTM32Driver() else null
    }

    private fun getUsbSerialPort() : UsbSerialPort? {
        //return if (getUsbSerialDriver() == null) null else getUsbSerialDriver()!!.ports[0]
        return if (getUsbSerialDriver() == null) null else getSTM32Driver()!!.ports[0]
    }

    /**
     * 偵測裝置
     */
    fun detectUSB() {
        if (mUsbManager == null) return

        if (mUsbManager.deviceList.size == 0) return

        // 取得目前插在USB-OTG上的裝置
        getUsbSerialDrivers()
    }

    private fun getUsbSerialDrivers() {
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager)
        if (availableDrivers.isEmpty()) {
            val deviceList = mUsbManager.deviceList
            android.util.Log.d(TAG, "裝置資訊列表:\n$deviceList")
            val deviceIterator: Iterator<UsbDevice> = deviceList.values.iterator()
            val customTable = ProbeTable()
            while (deviceIterator.hasNext()) {
                // 取得裝置資訊
                val device = deviceIterator.next()
                android.util.Log.v(TAG, "get device info, size= " + mDrivers.size + getDeviceInfo(device))

                // 設置驅動
                customTable.addProduct(
                    device.vendorId,
                    device.productId,  //Ch34xSerialDriver.class // Diver=CDC,C P21XX, CH34X, FTDI, Prolific etc...
                    CdcAcmSerialDriver::class.java
                )
                // 將驅動綁定給此裝置
                val prober = UsbSerialProber(customTable)
                mDrivers = prober.findAllDrivers(mUsbManager)
            }
        } else {
            mDrivers.add(availableDrivers[0])
        }

        @SuppressLint("UnspecifiedImmutableFlag") val permissionIntent = PendingIntent.getBroadcast(mContext, 0, Intent(
            ACTION_USB_PERMISSION
        ), 0)
        //mUsbDevice = mDrivers[0].device
        mUsbDevice = getSTM32()
        if (mUsbDevice == null) return
        mUsbManager.requestPermission(mUsbDevice, permissionIntent)
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        mContext.registerReceiver(mUsbReceiver, filter)
    }

    private fun getSTM32() : UsbDevice? {
        for (driver in mDrivers) {
            if (driver.device.vendorId == 4183 && driver.device.productId == 22336) {
                return driver.device
            }
        }
        return null
    }

    private fun getSTM32Driver() : UsbSerialDriver? {
        for (driver in mDrivers) {
            if (driver.device.vendorId == 4183 && driver.device.productId == 22336) {
                return driver
            }
        }
        return null
    }

    /**
     * BroadcastReceiver for USB permission
     */
    private val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (!TextUtils.isEmpty(action)) {
                if (action == ACTION_USB_PERMISSION) {
                    synchronized(this) {
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            processConnect()
                        }
                    }
                } else if (action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                    processDetach()
                    android.util.Log.d(TAG, "...processConnect")
                }
            }
        }
    }

    private fun getDeviceInfo(aDevice: UsbDevice): String {
        return "VID: ${aDevice.vendorId}\nPID: ${aDevice.deviceId}\nManufacturerName: ${aDevice.manufacturerName}\nProduceName: ${aDevice.productName}\nDevice Name: ${aDevice.deviceName}"
    }

    private fun processConnect() {
        android.util.Log.d(TAG, "is port open: ${openSerialPort()}")
        if(mUsbDevice != null) {
            mIConnectCallback?.doSuccess()
            android.util.Log.d(TAG, "...processConnect")
        }
    }
    private fun processDetach() {
        mIConnectCallback?.doFail()
    }

    fun getOpenSerialPort() : Boolean { return openSerialPort() }
    private fun openSerialPort(): Boolean {
        // 取得此USB裝置的PORT
        val port = getUsbSerialPort() ?: return false
        if (!port.isOpen) {
            //開啟port
            try {
                //初始化整個發送流程
                val connect = mUsbManager.openDevice(getUsbDevice())
                port.open(connect)
                //設定胞率、資料長度、停止位元、檢查位元
                port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
                android.util.Log.d(TAG, "openSerialPort, devices=${mDrivers.size}, connect=${connect.toString()}, port=${port.toString()}")
            } catch (e : IOException) {
                android.util.Log.e(TAG, "UsbSerialPort open failure: $e")
                return false
            }
        }
        return true
    }
    fun closeSerialPort() {
        val port = getUsbSerialPort() ?: return
        if (port.isOpen) {
            try {
                port.close()
            } catch (e : IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 送出資訊
     */
    fun sendValue(aValues : String) : String {
        val port = getUsbSerialPort() ?: return "Open serial port failure."

        return try {
            port.write(aValues.toByteArray(), 200)
            android.util.Log.v(TAG, "sendValue= $aValues")
            aValues
        } catch (e: IOException) {
            android.util.Log.e(TAG, "Write failure: $e")
            "Send value fail, msg=$e"
        }
    }
}