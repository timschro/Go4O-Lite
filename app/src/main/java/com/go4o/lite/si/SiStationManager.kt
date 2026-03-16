package com.go4o.lite.si

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.go4o.lite.si.adapter.UsbSiPort
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.Ch34xSerialDriver
import com.hoho.android.usbserial.driver.Cp21xxSerialDriver
import com.hoho.android.usbserial.driver.FtdiSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.ProlificSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import net.gecosi.CommStatus
import net.gecosi.SiHandler
import net.gecosi.SiListener
import net.gecosi.dataframe.SiDataFrame

class SiStationManager(
    private val context: Context,
    private val onCardRead: (SiDataFrame) -> Unit,
    private val onStatusChange: (CommStatus) -> Unit,
    private val onError: (String) -> Unit
) {

    companion object {
        private const val TAG = "SiStationManager"
        private const val ACTION_USB_PERMISSION = "com.go4o.lite.USB_PERMISSION"
        private const val MAX_RECONNECT_ATTEMPTS = 10
        private const val MAX_BACKOFF_MS = 30_000L

        /**
         * Known USB vendor/product IDs used by SportIdent stations.
         * Mapped to the usb-serial-for-android driver class that handles each chip.
         */
        private val SI_KNOWN_DEVICES: ProbeTable = ProbeTable().apply {
            // Silicon Labs CP2102 — BSM8-USB, BSF8-USB
            addProduct(0x10C4, 0x800A, Cp21xxSerialDriver::class.java)
            // Silicon Labs CP2104 — some newer SI stations
            addProduct(0x10C4, 0xEA60, Cp21xxSerialDriver::class.java)
            // FTDI FT232R — BSF7-USB, older SI stations
            addProduct(0x0403, 0x6001, FtdiSerialDriver::class.java)
            // Prolific PL2303
            addProduct(0x067B, 0x2303, ProlificSerialDriver::class.java)
            // WCH CH340
            addProduct(0x1A86, 0x7523, Ch34xSerialDriver::class.java)
        }

        private val siProber = UsbSerialProber(SI_KNOWN_DEVICES)
    }

    private var siHandler: SiHandler? = null
    private var usbSerialPort: UsbSerialPort? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isReconnecting = false
    private var reconnectAttempt = 0

    private val siListener = object : SiListener {
        override fun handleEcard(dataFrame: SiDataFrame) {
            onCardRead(dataFrame)
        }

        override fun notify(status: CommStatus) {
            if (status == CommStatus.CONNECTION_LOST) {
                handleConnectionLost()
                return
            }
            if (status == CommStatus.OFF && isReconnecting) {
                // Suppress the OFF notification from SiDriver.stop() during reconnection
                return
            }
            onStatusChange(status)
        }

        override fun notify(errorStatus: CommStatus, errorMessage: String) {
            Log.e(TAG, "SI Error: $errorStatus - $errorMessage")
            onError(errorMessage)
        }
    }

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == ACTION_USB_PERMISSION) {
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                if (granted) {
                    if (isReconnecting) {
                        connectToStation()
                        if (siHandler != null) {
                            clearReconnectState()
                        }
                    } else {
                        connectToStation()
                    }
                } else {
                    onError("USB permission denied")
                }
            }
        }
    }

    private val usbDetachReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                Log.d(TAG, "USB device detached")
                handleConnectionLost()
            }
        }
    }

    private val usbAttachReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED && isReconnecting) {
                Log.d(TAG, "USB device attached during reconnection, attempting reconnect")
                mainHandler.removeCallbacksAndMessages(RECONNECT_TOKEN)
                mainHandler.post { attemptReconnect() }
            }
        }
    }

    fun registerReceiver() {
        val permissionFilter = IntentFilter(ACTION_USB_PERMISSION)
        val detachFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)
        val attachFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbPermissionReceiver, permissionFilter, Context.RECEIVER_NOT_EXPORTED)
            context.registerReceiver(usbDetachReceiver, detachFilter, Context.RECEIVER_EXPORTED)
            context.registerReceiver(usbAttachReceiver, attachFilter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(usbPermissionReceiver, permissionFilter)
            context.registerReceiver(usbDetachReceiver, detachFilter)
            context.registerReceiver(usbAttachReceiver, attachFilter)
        }
    }

    fun unregisterReceiver() {
        try { context.unregisterReceiver(usbPermissionReceiver) } catch (_: Exception) { }
        try { context.unregisterReceiver(usbDetachReceiver) } catch (_: Exception) { }
        try { context.unregisterReceiver(usbAttachReceiver) } catch (_: Exception) { }
    }

    /**
     * Find a SportIdent USB-serial device. Tries:
     * 1. Our custom probe table of known SI vendor/product IDs
     * 2. The library's default prober (handles standard chips even with unusual product IDs)
     */
    private fun findSiDriver(usbManager: UsbManager): UsbSerialDriver? {
        // First try our known SI device table
        val knownDrivers = siProber.findAllDrivers(usbManager)
        if (knownDrivers.isNotEmpty()) {
            Log.d(TAG, "Found known SI device: vid=${knownDrivers[0].device.vendorId} pid=${knownDrivers[0].device.productId}")
            return knownDrivers[0]
        }
        // Fall back to default prober — picks up any supported USB-serial chip
        val defaultDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (defaultDrivers.isNotEmpty()) {
            Log.d(TAG, "Found USB-serial device via default prober: vid=${defaultDrivers[0].device.vendorId} pid=${defaultDrivers[0].device.productId}")
            return defaultDrivers[0]
        }
        return null
    }

    fun connect() {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val driver = findSiDriver(usbManager)

        if (driver == null) {
            onError("No SportIdent station found")
            return
        }

        val device = driver.device
        if (!usbManager.hasPermission(device)) {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
            val permissionIntent = PendingIntent.getBroadcast(context, 0,
                Intent(ACTION_USB_PERMISSION).apply { setPackage(context.packageName) }, flags)
            usbManager.requestPermission(device, permissionIntent)
            return
        }

        connectToStation()
    }

    private fun connectToStation() {
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val driver = findSiDriver(usbManager) ?: run {
                onError("SportIdent station disconnected")
                return
            }

            val connection = usbManager.openDevice(driver.device) ?: run {
                onError("Failed to open USB device")
                return
            }

            val port = driver.ports[0]
            port.open(connection)

            usbSerialPort = port
            val usbSiPort = UsbSiPort(port)
            val handler = SiHandler(siListener)
            siHandler = handler
            handler.connect(usbSiPort)
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            onError("Connection failed: ${e.message}")
        }
    }

    fun disconnect() {
        clearReconnectState()
        siHandler?.stop()
        siHandler = null
        try {
            usbSerialPort?.close()
        } catch (_: Exception) { }
        usbSerialPort = null
    }

    val isConnected: Boolean
        get() = siHandler?.isAlive == true

    // --- Reconnection logic ---

    private fun handleConnectionLost() {
        Log.d(TAG, "Connection lost, starting reconnection")
        // Clean up existing handler/port
        siHandler?.stop()
        siHandler = null
        try { usbSerialPort?.close() } catch (_: Exception) { }
        usbSerialPort = null

        if (!isReconnecting) {
            isReconnecting = true
            reconnectAttempt = 0
            onStatusChange(CommStatus.CONNECTION_LOST)
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        val delayMs = minOf(1000L * (1L shl minOf(reconnectAttempt, 5)), MAX_BACKOFF_MS)
        Log.d(TAG, "Scheduling reconnect attempt ${reconnectAttempt + 1} in ${delayMs}ms")
        mainHandler.postAtTime({ attemptReconnect() }, RECONNECT_TOKEN, android.os.SystemClock.uptimeMillis() + delayMs)
    }

    private fun attemptReconnect() {
        if (!isReconnecting) return

        reconnectAttempt++
        Log.d(TAG, "Reconnect attempt $reconnectAttempt")

        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val driver = findSiDriver(usbManager)

        if (driver == null) {
            if (reconnectAttempt < MAX_RECONNECT_ATTEMPTS) {
                scheduleReconnect()
            } else {
                Log.d(TAG, "Giving up reconnection after $MAX_RECONNECT_ATTEMPTS attempts")
                clearReconnectState()
                onStatusChange(CommStatus.OFF)
                onError("Station disconnected — could not reconnect")
            }
            return
        }

        if (!usbManager.hasPermission(driver.device)) {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
            val permissionIntent = PendingIntent.getBroadcast(context, 0,
                Intent(ACTION_USB_PERMISSION).apply { setPackage(context.packageName) }, flags)
            usbManager.requestPermission(driver.device, permissionIntent)
            // Permission result will come via usbPermissionReceiver; schedule a retry in case it's denied/ignored
            if (reconnectAttempt < MAX_RECONNECT_ATTEMPTS) {
                scheduleReconnect()
            }
            return
        }

        connectToStation()
        if (siHandler != null) {
            clearReconnectState()
        } else if (reconnectAttempt < MAX_RECONNECT_ATTEMPTS) {
            scheduleReconnect()
        } else {
            clearReconnectState()
            onStatusChange(CommStatus.OFF)
            onError("Station disconnected — could not reconnect")
        }
    }

    private fun clearReconnectState() {
        isReconnecting = false
        reconnectAttempt = 0
        mainHandler.removeCallbacksAndMessages(RECONNECT_TOKEN)
    }
}

private val RECONNECT_TOKEN = Any()
