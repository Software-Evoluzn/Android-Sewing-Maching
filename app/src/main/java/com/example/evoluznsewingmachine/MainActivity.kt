package com.example.evoluznsewingmachine

import android.app.Dialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import java.io.FileInputStream
import java.util.Calendar



class MainActivity : AppCompatActivity() {

    private lateinit var time:TextView
    private lateinit var pushBackCount:TextView
    private lateinit var temperature:TextView
    private lateinit var vibrationValue:TextView
    private lateinit var finalOilLevel:TextView
    private lateinit var threadPercent:TextView
    private lateinit var stitchCount:TextView
    private lateinit var threadConsumption:TextView
    private lateinit var resetBtn:AppCompatButton
    private lateinit var fileSavedBtn:ImageButton
    private lateinit var nextBtn:AppCompatButton
    private lateinit var dbHelper:DbHelper
    private val usbDataViewModel: UsbDataViewModel by viewModels()
    private lateinit var productionTimeCardView:CardView
    private lateinit var productionCountCardView:CardView
    private lateinit var stitchCountCardView:CardView
    private lateinit var tempCardView:CardView
    private lateinit var vibrationCardView:CardView
    private lateinit var oilLevelCardView:CardView
    private lateinit var bobbinThreadCardView:CardView
    private lateinit var stitchPerInchCardView:CardView

    private val ACTION_USB_PERMISSION = "permission"
    private lateinit var barChart: BarChart
    private lateinit var dataSet: BarDataSet
    private var previousValue = 0f
    private var entries = mutableListOf<BarEntry>()
    private var hourMap = mutableMapOf<Int, Float>().apply {
        for (i in 0..23) this[i] = 0f
    }

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
        nextBtn=findViewById(R.id.nextBtn)
        productionTimeCardView=findViewById(R.id.cardView2)
        productionCountCardView=findViewById(R.id.cardView)
        oilLevelCardView=findViewById(R.id.OilLevel)
        stitchCountCardView=findViewById(R.id.stitchCount)
        tempCardView=findViewById(R.id.temperature)
        vibrationCardView=findViewById(R.id.Vibration)
        bobbinThreadCardView=findViewById(R.id.bobinThreadLevel)
        stitchPerInchCardView=findViewById(R.id.threadConsumption)







        productionTimeCardView.setOnClickListener {
             val productionTimeList=dbHelper.productionTimeGraphToday()
            showGraphDialog("Production Time Graph",productionTimeList)
        }
        productionCountCardView.setOnClickListener {
            val productionCount=dbHelper.productionCountGraphToday()
            showGraphDialog("Production Count Graph",productionCount)

        }
        oilLevelCardView.setOnClickListener {
            val oilLevelList=dbHelper.oilLevelGraphToday()
            showGraphDialog("Oil Level Graph",oilLevelList)

        }
        stitchCountCardView.setOnClickListener {
            val stitchCountList=dbHelper.stitchCountGraphToday()
            showGraphDialog("Stitch Count  Graph",stitchCountList)

        }
        tempCardView.setOnClickListener {
            val temperatureList=dbHelper.tempGraphToday()
            showGraphDialog("Temperature  Graph",temperatureList)

        }
        vibrationCardView.setOnClickListener {
            val vibrationList=dbHelper.vibrationGraphToday()
            showGraphDialog("Vibration Graph",vibrationList)

        }
        bobbinThreadCardView.setOnClickListener {
            val bobbinThreadList=dbHelper.bobbinThreadGraphToday()
            showGraphDialog("Bobbin Thread Graph",bobbinThreadList)

        }
        stitchPerInchCardView.setOnClickListener {
              val stitchPerInchList=dbHelper.stitchPerInchGraphToday()
            showGraphDialog("Stitch Per Inch  Graph",stitchPerInchList)
        }

        fileSavedBtn.setOnClickListener {
            saveDatabaseToDownloads()
        }
        nextBtn.setOnClickListener {
           startActivity(Intent(this@MainActivity,HistoricDataShowing::class.java))
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
        val databasePath = getDatabasePath("Machine_Sewing.db").absolutePath
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "Machine_Sewing.db")
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
    private fun showGraphDialog(title: String, GraphData: List<Pair<String, Number>>) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_chart)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        val barChart = dialog.findViewById<BarChart>(R.id.productionTimeChart)
        val titleTextView = dialog.findViewById<TextView>(R.id.title)


        // Set dynamic title
        titleTextView.text = title



        // If no data is available, show a message and return
        if (GraphData.isEmpty()) {
            Toast.makeText(this, "No Data available.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            return
        }

        val entries = mutableListOf<BarEntry>()
        val hourLabels = mutableListOf<String>()


            // Ensure we collect unique hourly values (fix duplicate hours issue)
            val hourMap = mutableMapOf<Int, Float>()

            GraphData.forEach {
                val hour = it.first.substring(11, 13).toInt()
                hourMap[hour] = hourMap.getOrDefault(hour, 0f) + it.second.toFloat()
            }

            // Populate 24-hour range to avoid missing hours
            for (hour in 0..23) {
                entries.add(BarEntry(hour.toFloat(), hourMap.getOrDefault(hour, 0f)))
                hourLabels.add(String.format("%02d:00", hour)) // Correct fixed labels
            }


        // Setup Bar Chart Data
        val dataSet = BarDataSet(entries, "Production Time").apply {
            color = Color.RED
            setDrawValues(false)  // Hide values above bars
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.4f  // Adjust bar width for better alignment
        }

        barChart.data = barData

        // X-Axis Configuration
        barChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            isGranularityEnabled = true
            textSize = 12f
            textColor = Color.BLACK
            labelRotationAngle = -45f
            setAvoidFirstLastClipping(false)

            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index in hourLabels.indices) hourLabels[index] else ""
                }
            }

            setLabelCount(hourLabels.size, false) // Ensure labels are shown dynamically
        }

        // Y-Axis Configuration
        barChart.axisLeft.apply {
            axisMinimum = 0f // Ensure Y-axis starts from 0
            setDrawGridLines(false)
        }

        barChart.axisRight.isEnabled = false // Hide right Y-axis
        barChart.xAxis.setDrawGridLines(false)

        // Hide description and legend for a cleaner look
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false

        // Ensure bars are spaced properly
//        barChart.setVisibleXRangeMaximum(12f) // Show only 6 bars at a time
//        barChart.moveViewToX(0f)  // Start from the beginning of the chart


        // Ensure correct scaling to fit all bars
//        barChart.setScaleEnabled(true)  // Allow pinch zooming
//        barChart.setPinchZoom(true)     // Enable smooth zooming
//        barChart.isDragEnabled = true   // Allow horizontal scrolling

        // Animation for smooth appearance
        barChart.animateY(1000, Easing.EaseInOutQuad)

        // Custom marker view to show values on touch
        // Custom marker view to show values on touch
        val markerView = object : MarkerView(this, R.layout.custom_mark_view) {
            private val textView: TextView = findViewById(R.id.markerText)

            override fun refreshContent(e: Entry?, highlight: Highlight?) {
                e?.y?.let { value ->
                    val displayValue = if (value == value.toInt().toFloat()) {
                        value.toInt().toString()
                    } else {
                        String.format("%.2f", value)
                    }
                    textView.text = "Value: $displayValue"
                }
                super.refreshContent(e, highlight)
            }
        }

        barChart.marker = markerView


        dialog.show()
    }


}

