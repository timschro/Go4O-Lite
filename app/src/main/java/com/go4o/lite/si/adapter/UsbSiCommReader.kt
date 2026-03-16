package com.go4o.lite.si.adapter

import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import net.gecosi.internal.SiMessage
import net.gecosi.internal.SiMessageQueue

class UsbSiCommReader(
    private val usbPort: UsbSerialPort,
    private val messageQueue: SiMessageQueue
) : Runnable {

    companion object {
        private const val TAG = "UsbSiCommReader"
        private const val MAX_MESSAGE_SIZE = 139
        private const val METADATA_SIZE = 6
        private const val READ_TIMEOUT_MS = 100
        private const val STALE_TIMEOUT_MS = 500
        const val POISON_COMMAND: Byte = 0xFF.toByte()
    }

    @Volatile
    var running = true

    private val accumulator = ByteArray(MAX_MESSAGE_SIZE)
    private var accSize = 0
    private var lastReadTime = 0L

    override fun run() {
        val buffer = ByteArray(MAX_MESSAGE_SIZE)
        var errorExit = false
        while (running && !Thread.currentThread().isInterrupted) {
            try {
                val bytesRead = usbPort.read(buffer, READ_TIMEOUT_MS)
                if (bytesRead > 0) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime > lastReadTime + STALE_TIMEOUT_MS) {
                        accSize = 0
                    }
                    lastReadTime = currentTime

                    System.arraycopy(buffer, 0, accumulator, accSize, bytesRead)
                    accSize += bytesRead

                    if (accSize == 1 && accumulator[0] != 0x02.toByte()) {
                        sendMessage()
                    } else {
                        checkExpectedLength()
                    }
                }
            } catch (e: Exception) {
                if (running) {
                    Log.e(TAG, "Read error: ${e.message}")
                    errorExit = true
                }
                break
            }
        }
        if (errorExit) {
            val poison = SiMessage(byteArrayOf(0x00, POISON_COMMAND))
            messageQueue.offer(poison)
        }
    }

    private fun checkExpectedLength() {
        if (accSize >= 3 && completeMessage()) {
            sendMessage()
        }
    }

    private fun completeMessage(): Boolean {
        return (accumulator[2].toInt() and 0xFF) == accSize - METADATA_SIZE
    }

    private fun sendMessage() {
        val message = SiMessage(accumulator.copyOfRange(0, accSize))
        Log.d(TAG, "READ $message")
        messageQueue.put(message)
        accSize = 0
    }
}
