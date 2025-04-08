package com.example.evoluznsewingmachine

import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class UsbService:Service() {
    private lateinit var usbManager: UsbManager
    private var usbDevice: UsbDevice? = null
    private var usbSerial: UsbSerialDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    private val usbBuffer = StringBuffer()
    lateinit var dbHelper:DbHelper
    private val ACTION_USB_PERMISSION = "permission"

    private val handler = Handler(Looper.getMainLooper())
    private var notificationRunnable: Runnable? = null
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "usb_service_channel"
    lateinit var sharedPref:SharedPreferences


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        dbHelper = DbHelper(this)


          sharedPref= getSharedPreferences("StitchPrefs", Context.MODE_PRIVATE)

        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        registerReceiver(usbReceiver, filter)

        startForeground(1, createNotification())

        Handler(Looper.getMainLooper()).postDelayed({
            startUsbConnection()
        }, 5000) // Another delay to ensure device enumeration
    }

    private fun startUsbConnection() {
        val usbDevices: HashMap<String, UsbDevice>? = usbManager.deviceList
        val sharedPref = getSharedPreferences("USB_PREFS", Context.MODE_PRIVATE)

        if (!usbDevices.isNullOrEmpty()) {
            var keep = true
            usbDevices.forEach { entry ->
                usbDevice = entry.value
                val deviceVendorId: Int? = usbDevice?.vendorId

                if (deviceVendorId == 9025) {
                    val permissionGranted = sharedPref.getBoolean("usb_permission_granted", false)

                    if (permissionGranted) {
                        // ✅ Auto connect since permission was already granted
                        usbConnection = usbManager.openDevice(usbDevice)
                        usbSerial = UsbSerialDevice.createUsbSerialDevice(usbDevice, usbConnection)

                        usbSerial?.let { serial ->
                            if (serial.open()) {
                                serial.setBaudRate(9600)
                                serial.setDataBits(UsbSerialInterface.DATA_BITS_8)
                                serial.setStopBits(UsbSerialInterface.STOP_BITS_1)
                                serial.setParity(UsbSerialInterface.PARITY_NONE)
                                serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                                serial.read(usbReadCallback)

                                Log.i("serial", "USB Auto Connected")
                                Toast.makeText(this, "USB Auto Connected", Toast.LENGTH_SHORT).show()
                            }
                        }

                    } else {
                        // ❌ No permission, ask for it
                        val intent = PendingIntent.getBroadcast(
                            this, 0, Intent(ACTION_USB_PERMISSION), 0
                        )
                        usbManager.requestPermission(usbDevice, intent)
                    }

                    keep = false
                }
                if (!keep) return
            }
        } else {
            val usbNotconnectedIntent = Intent("USB_NOT_CONNECTED")
            sendBroadcast(usbNotconnectedIntent)
            Log.i("serial", "No USB device connected")
        }
    }


    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_USB_PERMISSION -> {

                    val granted = intent.extras?.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                    if (granted == true) {
                        usbConnection = usbManager.openDevice(usbDevice)
                        usbSerial = UsbSerialDevice.createUsbSerialDevice(usbDevice, usbConnection)

                        usbSerial?.let { serial ->
                            if (serial.open()) {
                                serial.setBaudRate(9600)
                                serial.setDataBits(UsbSerialInterface.DATA_BITS_8)
                                serial.setStopBits(UsbSerialInterface.STOP_BITS_1)
                                serial.setParity(UsbSerialInterface.PARITY_NONE)
                                serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                                serial.read(usbReadCallback)

                                // ✅ Save permission state
                                getSharedPreferences("USB_PREFS", Context.MODE_PRIVATE).edit()
                                    .putBoolean("usb_permission_granted", true)
                                    .apply()

                                Toast.makeText(applicationContext, "USB Connected", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.i("USB_SERVICE", "Serial port not open")
                            }
                        }
                    } else {
                        Log.i("USB_SERVICE", "USB Permission denied")
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    Log.i("USB_SERVICE", "USB device disconnected")
                    Toast.makeText(applicationContext, "USB Device Disconnected", Toast.LENGTH_SHORT).show()

//                     Show dialog box when USB is disconnected
                    // Send a broadcast to notify the activity
                    val usbDisconnectedIntent = Intent("USB_DISCONNECTED")
                    sendBroadcast(usbDisconnectedIntent)

                    disconnectUsb()
                }

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    Log.i("USB_SERVICE", "USB device connected")
                    Toast.makeText(applicationContext, "USB Device Connected", Toast.LENGTH_SHORT).show()
                    startUsbConnection() // Try reconnecting automatically
                }
            }
        }
    }




    private val usbReadCallback = UsbSerialInterface.UsbReadCallback { data ->
        data?.let {
            val receivedChunk = String(data, Charsets.UTF_8)
            Log.i("USB_SERVICE", "Received Chunk: '$receivedChunk'")

            synchronized(usbBuffer) {
                usbBuffer.append(receivedChunk)
            }

            processUsbBuffer() // ✅ Process data in a separate function to prevent blocking
        }
    }

    // ✅ Background thread for message processing
    private fun processUsbBuffer() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                var fullMessage: String? = null
                synchronized(usbBuffer) {
                    val newlineIndex = usbBuffer.indexOf("\n")
                    if (newlineIndex != -1) {
                        fullMessage = usbBuffer.substring(0, newlineIndex).trim()
                        usbBuffer.delete(0, newlineIndex + 1)
                    }
                }

                if (!fullMessage.isNullOrEmpty()) {
                    processReceivedData(fullMessage!!)
                    showDataNotification(fullMessage!!)
                    resetNotificationTimeout()
                } else {
                    delay(100) // Prevents high CPU usage
                }
            }
        }
    }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private fun processReceivedData(received: String) {
        Log.i("USB_SERVICE", "Final Received Data: '$received'")

        coroutineScope.launch {
            try {
                val dataList = received.split(",")

                if (dataList.size < 7) {
                    Log.e("USB_SERVICE", "Invalid data format: Expected 7 values but got ${dataList.size}. Data: $dataList")
                    return@launch
                }

                val time = dataList[0].toInt()
                val pushCount = dataList[1].toInt()
                val temp = dataList[2]
                val vibrationValue = dataList[3]
                val oilLevel = dataList[4]
                val threadPercent = dataList[5].toFloat()
                val stitchCount = dataList[6].toInt()

                Log.i("USB_SERVICE", "Parsed Data -> Time: $time, PushCount: $pushCount, Temp: $temp, Vibration: $vibrationValue, OilLevel: $oilLevel, ThreadPercent: $threadPercent, StitchCount: $stitchCount")

//

                // Insert into database on IO thread
                val status = dbHelper.insertData(time, pushCount, temp, vibrationValue, oilLevel, threadPercent, stitchCount)

                if (status) {
                   Log.i("USB_SERVICE", "Data inserted successfully into database")

                    // Fetch updated machine data in IO thread
                    val updatedMachineData = dbHelper.getMachineData()


                    // Switch to Main thread to update UI
                   withContext(Dispatchers.Main) {
                       updatedMachineData?.let {
                            UsbDataRepository.updateUsbData(
                                it.totalTime.toString(),
                               it.totalPushBackCount.toString(),
                                it.latestTemperature,
                                it.latestVibration,
                               it.latestOilLevel,
                                it.latestThreadPercent.toString(),
                                it.totalStitchCount.toString(),
                                it.stitchPerInch.toString()


                            )
                        }
                   }
                }
            else {
                   Log.e("USB_SERVICE", "Data insertion failed")
               }

            } catch (e: Exception) {
                Log.e("USB_SERVICE", "Exception in processing received data: ${e.message}")
                e.printStackTrace()
            }
        }
    }




    private fun disconnectUsb() {
        usbSerial?.close()
        usbSerial = null
        usbDevice = null
        usbConnection = null
        Log.i("USB_SERVICE", "USB Disconnected")
    }


    override fun onDestroy() {
        unregisterReceiver(usbReceiver)
        disconnectUsb()
        coroutineScope.cancel() // 👈 Add this
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification {
        val channelId = "usb_service_channel"
        val channelName = "USB Service Channel"

        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            channelId, channelName, NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("USB Service Running")
            .setContentText("Receiving USB data in the background")
            .setSmallIcon(R.drawable.logo)
            .build()
    }

    private fun showDataNotification(data: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("USB Data Received")
            .setContentText("Data: $data")
            .setSmallIcon(R.drawable.logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun resetNotificationTimeout() {
        notificationRunnable?.let { handler.removeCallbacks(it) }

        notificationRunnable = Runnable {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.cancel(NOTIFICATION_ID) // Remove notification
        }
        handler.postDelayed(notificationRunnable!!, 10000) // Remove notification after 10 sec
    }
}