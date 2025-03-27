package com.example.evoluznsewingmachine


import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.Observer
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {

    lateinit var time:TextView
    lateinit var pushBackCount:TextView
    lateinit var temperature:TextView
    lateinit var vibrationValue:TextView
    lateinit var finalOilLevel:TextView
    lateinit var threadPercent:TextView
    lateinit var stitchCount:TextView
    lateinit var threadConsumption:TextView
    lateinit var resetBtn:AppCompatButton
    private lateinit var fileSavedBtn:ImageButton


    lateinit var dbHelper:DbHelper
    private val usbDataViewModel: UsbDataViewModel by viewModels()


    private val usbDisconnectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                if (intent.action == "USB_DISCONNECTED") {
                    showUsbDisconnectedDialog()
                }
            }
        }
    }

    private val usbNotConnectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                if (intent.action == "USB_NOT_CONNECTED") {
                    showUsbNotconnectedDialog()
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)



        time=findViewById(R.id.time)
        pushBackCount=findViewById(R.id.count)
        temperature=findViewById(R.id.temperatureValue)
        vibrationValue=findViewById(R.id.vibrationValue)
        finalOilLevel=findViewById(R.id.oilLevelValue)
        threadPercent=findViewById(R.id.bobinThreadLevelValue)
        stitchCount=findViewById(R.id.stitchCountValue)
        threadConsumption=findViewById(R.id.threadConsumptionValue)
        resetBtn=findViewById(R.id.resetBtn)
        fileSavedBtn=findViewById(R.id.dbFileSave)
        dbHelper=DbHelper(this)

        fileSavedBtn.setOnClickListener {
            saveDatabaseToDownloads()
        }





        // USB Service start karein
        val serviceIntent = Intent(this, UsbService::class.java)
        startService(serviceIntent)

        val filter = IntentFilter("USB_DISCONNECTED")
        registerReceiver(usbDisconnectReceiver, filter)

        val filter1 = IntentFilter("USB_NOT_CONNECTED")
        registerReceiver(usbNotConnectReceiver, filter1)

        // Observe LiveData and update UI
        usbDataViewModel.productionTime.observe(this, Observer {
            time.text = it
        })

        usbDataViewModel.productionCount.observe(this, Observer {
            pushBackCount.text = it
        })

        usbDataViewModel.temperature.observe(this, Observer {
            temperature.text = it
        })

        usbDataViewModel.vibrationValue.observe(this, Observer {
            vibrationValue.text = it
        })

        usbDataViewModel.oilLevel.observe(this, Observer {
            finalOilLevel.text = it
        })

        usbDataViewModel.threadPercent.observe(this, Observer {
            threadPercent.text = it
        })

        usbDataViewModel.stitchCount.observe(this, Observer {
            stitchCount.text = it
        })

        usbDataViewModel.threadConsumption.observe(this, Observer {
            threadConsumption.text = it
        })

        resetBtn.setOnClickListener {
            val dbHelper = DbHelper(this)

            // Mark the last row as reset and insert a new zeroed row
            dbHelper.resetMachineData()

            // Update UI to reflect reset
            time.text = "0"
            pushBackCount.text = "0"
            temperature.text = "0"
            vibrationValue.text = "0"
            finalOilLevel.text = "0"
            threadPercent.text = "0"
            stitchCount.text = "0"
            threadConsumption.text = "0"

            // Refresh data after 3 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                usbDataViewModel.productionTime
                usbDataViewModel.productionCount
                usbDataViewModel.temperature
                usbDataViewModel.vibrationValue
                usbDataViewModel.oilLevel
                usbDataViewModel.threadPercent
                usbDataViewModel.stitchCount
                usbDataViewModel.threadConsumption
            }, 3000)
        }





    }



    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbDisconnectReceiver)
        unregisterReceiver(usbNotConnectReceiver)
    }

    private fun showUsbDisconnectedDialog() {
        AlertDialog.Builder(this)
            .setTitle("USB Disconnected")
            .setMessage("The USB device has been disconnected. Please reconnect.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }

    private fun showUsbNotconnectedDialog() {
        AlertDialog.Builder(this)
            .setTitle("USB Not connected")
            .setMessage("The USB device has not  connected. Please connect.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveDatabaseToDownloads() {
        val databasePath = getDatabasePath("MachineData.db").absolutePath
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "MachineData.db")
            put(MediaStore.Downloads.MIME_TYPE, "application/x-sqlite3")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            try {
                resolver.openOutputStream(it)?.use { outputStream ->
                    FileInputStream(databasePath).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Toast.makeText(this, "Database saved to Downloads", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to save database", Toast.LENGTH_SHORT).show()
            }
        }
    }

}

