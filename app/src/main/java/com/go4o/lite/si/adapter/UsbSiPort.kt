package com.go4o.lite.si.adapter

import com.hoho.android.usbserial.driver.UsbSerialPort
import net.gecosi.internal.CommWriter
import net.gecosi.internal.SiMessageQueue
import net.gecosi.internal.SiPort

class UsbSiPort(private val usbPort: UsbSerialPort) : SiPort {

    private var readerThread: Thread? = null
    private var reader: UsbSiCommReader? = null

    override fun createMessageQueue(): SiMessageQueue {
        val queue = SiMessageQueue(10)
        reader = UsbSiCommReader(usbPort, queue)
        readerThread = Thread(reader, "UsbSiCommReader").also { it.start() }
        return queue
    }

    override fun createWriter(): CommWriter {
        return UsbSiCommWriter(usbPort)
    }

    override fun setupHighSpeed() {
        usbPort.setParameters(38400, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
    }

    override fun setupLowSpeed() {
        usbPort.setParameters(4800, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
    }

    override fun close() {
        reader?.running = false
        readerThread?.interrupt()
        try {
            usbPort.close()
        } catch (_: Exception) { }
    }
}
