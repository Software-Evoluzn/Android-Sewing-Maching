package com.example.evoluznsewingmachine

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
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
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class HistoricDataShowing : AppCompatActivity() {

    lateinit var p_time:TextView
    lateinit var p_count:TextView
    lateinit var p_temp:TextView
    lateinit var p_vibration:TextView
    lateinit var p_oil_level:TextView
    lateinit var p_bobbin_thread:TextView
    lateinit var p_sitch_count:TextView
    lateinit var p_stitch_per_inch:TextView
    lateinit var setDate:AppCompatButton
    lateinit var startDate: String
    lateinit var endDate: String
    lateinit var backBtn:ImageButton
    lateinit var  applyBtn:AppCompatButton
    lateinit var productionTimeCardView:CardView
    lateinit var productionCountCardView: CardView
    lateinit var oilLevelCardView:CardView
    lateinit var stitchCountCardView:CardView
    lateinit var temperatureCardView:CardView
    lateinit var vibrationCardView:CardView
    lateinit var bobbinThreadCardView:CardView
    lateinit var stitchPerInchCardView:CardView
    var isToday:Boolean = false
    lateinit var dbHelpher:DbHelper
    private val usbDataViewModel: UsbDataViewModel by viewModels()





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_historic_data_showing)

        p_time=findViewById(R.id.time)
        p_count=findViewById(R.id.count)
        p_temp=findViewById(R.id.temperatureValue)
        p_vibration=findViewById(R.id.vibrationValue)
        p_oil_level=findViewById(R.id.oilLevelValue)
        p_bobbin_thread=findViewById(R.id.bobinThreadLevelValue)
        p_sitch_count=findViewById(R.id.stitchCountValue)
        p_stitch_per_inch=findViewById(R.id.threadConsumptionValue)
        setDate=findViewById(R.id.setDate)
        backBtn=findViewById(R.id.backBtn)
        applyBtn=findViewById(R.id.applyBtn)
        productionTimeCardView=findViewById(R.id.cardView2)
        dbHelpher=DbHelper(this)
        productionCountCardView=findViewById(R.id.cardView)
        oilLevelCardView=findViewById(R.id.OilLevel)
        stitchCountCardView=findViewById(R.id.stitchCount)
        temperatureCardView=findViewById(R.id.temperature)
        vibrationCardView=findViewById(R.id.Vibration)
        bobbinThreadCardView=findViewById(R.id.bobinThreadLevel)
        stitchPerInchCardView=findViewById(R.id.threadConsumption)


        backBtn.setOnClickListener {
            startActivity(Intent(this@HistoricDataShowing,MainActivity::class.java))
        }

        setDate.setOnClickListener {
            val popupMenu = PopupMenu(this, setDate)
            popupMenu.menu.add("Today")
            popupMenu.menu.add("Set Range")

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Today" -> {
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        setDate.text = today
                        startDate = today
                        endDate = today

                        isToday=true
                        applyBtn.setOnClickListener {
                            fetchAndDisplayData(startDate, endDate)  // Fetch immediately
                        }

                    }
                    "Set Range" -> {
                        showDateRangePicker { start, end ->
                            setDate.text = "$start to $end"
                            startDate = start
                            endDate = end
                            if(startDate==endDate){
                                isToday=true
                            }else{
                                isToday=false
                            }

                            applyBtn.setOnClickListener {
                                fetchAndDisplayData(startDate, endDate)  // Fetch immediately
                            }
                        }
                    }
                }
                true
            }
            popupMenu.show()
        }
        productionTimeCardView.setOnClickListener {
            if (::startDate.isInitialized && ::endDate.isInitialized) {
                checkAndShowGraph("Production Time Graph") { start, end ->
                    if (isToday) {
                        dbHelpher.productionTimeGraph(start, end)
                    } else {
                        dbHelpher.productionTimeForSetRangeGraph(start, end)
                    }
                }
            } else {
                showAlertDialog()

            }
        }

        productionCountCardView.setOnClickListener {
            if (::startDate.isInitialized && ::endDate.isInitialized) {
                checkAndShowGraph("Production Count Graph") { start, end ->
                    if (isToday) {
                        dbHelpher.productionCountGraph(start, end)
                    } else {
                        dbHelpher.productionCountForSetRangeGraph(start, end)
                    }
                }
            }else{
                showAlertDialog()
            }
        }
        oilLevelCardView.setOnClickListener {
            if(::startDate.isInitialized && ::endDate.isInitialized){
                checkAndShowGraph("Oil Level Graph") { start, end ->
                    if(isToday){
                        dbHelpher.oilLevelGraph(start, end)
                    }else{
                        dbHelpher.oilLevelForSetRangeGraph(startDate, endDate)
                    }
                }
            }else{
                showAlertDialog()
            }

        }

        stitchCountCardView.setOnClickListener {
            if(::startDate.isInitialized && ::endDate.isInitialized){
                checkAndShowGraph("Stitch Count Graph") { start, end ->
                    if(isToday){
                        dbHelpher.stitchCountGraph(start, end)
                    }else{
                        dbHelpher.stitchCountForSetRangeGraph(startDate, endDate)
                    }
                }
            }else{
                showAlertDialog()

            }

        }
        temperatureCardView.setOnClickListener {
            if(::startDate.isInitialized && ::endDate.isInitialized){
                checkAndShowGraph("Temperature Graph") { start, end ->
                    if(isToday){
                        dbHelpher.temperatureGraph(start, end)
                    }else{
                        dbHelpher.temperatureForSetRangeGraph(startDate, endDate)
                    }
                }
            }else{
                showAlertDialog()
            }

        }
        vibrationCardView.setOnClickListener {

            if(::startDate.isInitialized && ::endDate.isInitialized){
                checkAndShowGraph("Vibration Graph") { start, end ->
                    if(isToday){
                        dbHelpher.vibrationGraph(start, end)
                    }else{
                        dbHelpher.vibrationForSetRangeGraph(startDate, endDate)
                    }
                }
            }else{
                showAlertDialog()
            }

        }
        bobbinThreadCardView.setOnClickListener {
            if(::startDate.isInitialized && ::endDate.isInitialized){
                checkAndShowGraph("Bobbin Thread Graph") { start, end ->
                    if(isToday){
                        dbHelpher.bobbinThreadGraph(start, end)
                    }else{
                        dbHelpher.bobbinThreadForSetRangeGraph(startDate, endDate)
                    }
                }
            }else{
                showAlertDialog()
            }

        }
        stitchPerInchCardView.setOnClickListener {
            if(::startDate.isInitialized && ::endDate.isInitialized){
                checkAndShowGraph("Stitches Per Inch  Graph") { start, end ->
                    if(isToday){
                        dbHelpher.stitchPerInchGraph(start, end)
                    }else{
                        dbHelpher.stitchCountPerInchForSetRangeGraph(startDate, endDate)
                    }
                }
            }else{
                showAlertDialog()
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
        val dateTextView=dialog.findViewById<TextView>(R.id.showingDate)

        // Set dynamic title
        titleTextView.text = title

        // **Show the Selected Date in UI**
        val dateToShow = if (startDate == endDate) {
            "Date: $startDate"  // Single date (Today case)
        } else {
            "Date Range: $startDate to $endDate" // Set range case
        }
        dateTextView.text = dateToShow

        // If no data is available, show a message and return
        if (GraphData.isEmpty()) {
            Toast.makeText(this, "No Data available.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            return
        }

        val entries = mutableListOf<BarEntry>()
        val hourLabels = mutableListOf<String>()

        if (isToday) {
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
        } else {
            // Ensure unique timestamps for non-today cases
            GraphData.forEachIndexed { index, pair ->
                entries.add(BarEntry(index.toFloat(), pair.second.toFloat()))
               hourLabels.add(pair.first) // Correctly formatted "yyyy-MM-dd HH:mm"
            }
        }

        // Setup Bar Chart Data
        val dataSet = BarDataSet(entries, "Production Time").apply {
            color = Color.BLUE
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
        barChart.setVisibleXRangeMaximum(12f) // Show only 6 bars at a time
        barChart.moveViewToX(0f)  // Start from the beginning of the chart


        // Ensure correct scaling to fit all bars
        barChart.setScaleEnabled(true)  // Allow pinch zooming
        barChart.setPinchZoom(true)     // Enable smooth zooming
        barChart.isDragEnabled = true   // Allow horizontal scrolling

        // Animation for smooth appearance
        barChart.animateY(1000, Easing.EaseInOutQuad)

        // Custom marker view to show values on touch
        val markerView = object : MarkerView(this, R.layout.custom_mark_view) {
            private val textView: TextView = findViewById(R.id.markerText)

            override fun refreshContent(e: Entry?, highlight: Highlight?) {
                textView.text = "Value: ${e?.y?.toInt()}" // Show value on touch
                super.refreshContent(e, highlight)
            }
        }

        barChart.marker = markerView // Attach marker view

        dialog.show()
    }

    private fun showDateRangePicker(onRangeSelected: (String, String) -> Unit) {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Date Range")
            .setTheme(R.style.CustomDatePicker) // Apply the custom theme
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = sdf.format(Date(selection.first!!))
            val endDate = sdf.format(Date(selection.second!!))
            onRangeSelected(startDate, endDate) // Return start and end dates separately
        }

        dateRangePicker.show(supportFragmentManager, "DATE_PICKER")
    }


    private fun fetchAndDisplayData(startDate: String, endDate: String) {
        val dbHelper = DbHelper(this)  // Initialize your database helper
        val machineData = dbHelper.getMachineDataByDateRange(startDate, endDate)

        machineData?.let {
            p_time.text = it.totalTime.toString()
            p_count.text = it.totalPushBackCount.toString()
            p_temp.text = it.latestTemperature
            p_vibration.text = it.latestVibration
            p_oil_level.text = it.latestOilLevel
            p_bobbin_thread.text = it.latestThreadPercent.toString()
            p_sitch_count.text = it.totalStitchCount.toString()
            p_stitch_per_inch.text = it.stitchPerInch.toString()
        } ?: run {
            // Handle case when no data is found
            p_time.text = "No Data"
            p_count.text = "No Data"
            p_temp.text = "No Data"
            p_vibration.text = "No Data"
            p_oil_level.text = "No Data"
            p_bobbin_thread.text = "No Data"
            p_sitch_count.text = "No Data"
            p_stitch_per_inch.text = "No Data"


        }

    }

    private fun checkAndShowGraph(title: String, fetchData: (String, String) -> List<Pair<String, Number>>) {
        if (startDate.isEmpty() || endDate.isEmpty()) {  // Ensure dates are set
            AlertDialog.Builder(this)
                .setTitle("Date Range Required")
                .setMessage("Please select a date range before viewing the graph.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
            return
        }

//       val isToday = startDate == endDate // Check if the user selected "Today"
        val start = "$startDate 00:00:00"
        val end = "$endDate 23:59:59"

        val data = fetchData(start, end)

        if (data.isEmpty()) {
            Toast.makeText(this, "No data found in the database!", Toast.LENGTH_SHORT).show()
        } else {
            showGraphDialog(title, data) // Pass `isToday` to adjust labels dynamically
        }
    }


   fun  showAlertDialog(){
        AlertDialog.Builder(this)
            .setTitle("Date Range Required")
            .setMessage("Please select a date range before viewing the graph.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
        return
    }


}