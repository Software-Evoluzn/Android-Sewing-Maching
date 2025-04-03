package com.example.evoluznsewingmachine


import android.app.DatePickerDialog
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Calendar
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
    lateinit var nextBtn:AppCompatButton
    lateinit var dbHelper:DbHelper
    private val usbDataViewModel: UsbDataViewModel by viewModels()
    lateinit var productionTimeCardView:CardView
    lateinit var productionCountCardView:CardView
    lateinit var stitchCountCardView:CardView
    lateinit var tempCardView:CardView
    lateinit var vibrationCardView:CardView
    lateinit var oilLevelCardView:CardView
    lateinit var bobbinThreadCardView:CardView
    lateinit var stitchPerInchCardView:CardView

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
            showLiveDateGraph(usbDataViewModel.productionTime)
        }
        productionCountCardView.setOnClickListener {
            showLiveDateGraph(usbDataViewModel.productionCount)
        }
        oilLevelCardView.setOnClickListener {
            showLiveDateGraph(usbDataViewModel.oilLevel)
        }
        stitchCountCardView.setOnClickListener {
            showLiveDateGraph(usbDataViewModel.stitchCount)
        }
        tempCardView.setOnClickListener {
            showLiveDateGraph(usbDataViewModel.temperature)
        }
        vibrationCardView.setOnClickListener {
            showLiveDateGraph(usbDataViewModel.vibrationValue)
        }
        bobbinThreadCardView.setOnClickListener {
            showLiveDateGraph(usbDataViewModel.threadPercent)
        }
        stitchPerInchCardView.setOnClickListener {
            showLiveDateGraph(usbDataViewModel.threadConsumption)
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

    private fun showLiveDateGraph(liveData: LiveData<String>) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_chart)

        val barChart = dialog.findViewById<BarChart>(R.id.productionTimeChart)

        // **Chart Settings**
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.setDrawBorders(false)
        barChart.setScaleEnabled(true) // Enable zoom & horizontal scroll
        barChart.isDragEnabled = true
        barChart.setPinchZoom(true)

        val entries = mutableListOf<BarEntry>()
        val hourLabels = (0..23).map { String.format("%02d:00", it) } // **All 24 hours**

        val dataSet = BarDataSet(entries, "Live Data").apply {
            color = Color.BLUE
            valueTextColor = Color.BLACK
            setDrawValues(true) // Show values above bars
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.4f
        barChart.data = barData

        // **Customize X Axis (Fixed 00:00 - 23:00)**
        barChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawAxisLine(true)
            setDrawGridLines(false)
            textSize = 10f
            textColor = Color.BLACK
            labelRotationAngle = -45f

            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index in 0..23) hourLabels[index] else ""
                }
            }
        }

        // **Customize Y Axis**
        barChart.axisLeft.apply {
            axisMinimum = 0f
            setDrawAxisLine(true)
            setDrawGridLines(false)
        }
        barChart.axisRight.isEnabled = false

        // **Store Last 24 Hours Data**
        val hourMap = mutableMapOf<Int, Float>().apply {
            for (i in 0..23) this[i] = 0f
        }

        liveData.observe(this) { value ->
            val yValue = value.toFloatOrNull() ?: 0f
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

            // **Update Data for Current Hour**
            hourMap[currentHour] = hourMap.getOrDefault(currentHour, 0f) + yValue

            // **Update Graph**
            entries.clear()

            for (i in 0..23) {
                entries.add(BarEntry(i.toFloat(), hourMap[i] ?: 0f))
            }

            dataSet.notifyDataSetChanged()
            barChart.data.notifyDataChanged()
            barChart.notifyDataSetChanged()
        }

        // **Enable Scrolling for all 24 bars**
        barChart.xAxis.axisMinimum = -0.5f
        barChart.xAxis.axisMaximum = 23.5f
        barChart.setVisibleXRangeMaximum(6f) // Show 6 bars at a time (adjust as needed)
        barChart.moveViewToX(0f) // Start from 00:00

        dialog.show()
    }




}

