package com.go4o.lite.si.adapter

import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import net.gecosi.internal.CommWriter
import net.gecosi.internal.SiMessage

class UsbSiCommWriter(private val usbPort: UsbSerialPort) : CommWriter {

    companion object {
        private const val TAG = "UsbSiCommWriter"
        private const val WRITE_TIMEOUT_MS = 1000
    }

    override fun write(message: SiMessage) {
        val data = message.sequence()
        Log.d(TAG, "SEND ${message}")
        usbPort.write(data, WRITE_TIMEOUT_MS)
    }
}
