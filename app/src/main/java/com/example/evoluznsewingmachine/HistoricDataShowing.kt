package com.example.evoluznsewingmachine

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
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
    lateinit var dbHelpher:DbHelper




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

                        fetchAndDisplayData(startDate, endDate)  // Fetch immediately
                    }
                    "Set Range" -> {
                        showDateRangePicker { start, end ->
                            setDate.text = "$start to $end"
                            startDate = start
                            endDate = end
                            fetchAndDisplayData(startDate, endDate)  // Fetch immediately
                        }
                    }
                }
                true
            }
            popupMenu.show()
        }

// Apply button should fetch data based on already selected dates
        applyBtn.setOnClickListener {
            if (this::startDate.isInitialized && this::endDate.isInitialized) {
                fetchAndDisplayData(startDate, endDate)
            } else {
                Toast.makeText(this, "Please select a date range first", Toast.LENGTH_SHORT).show()
            }
        }


        productionTimeCardView.setOnClickListener {
            checkAndShowGraph("Production Time Graph") { start, end ->
                dbHelpher.productionTimeGraph(start, end)
            }
        }



        productionCountCardView.setOnClickListener {
            checkAndShowGraph("Production Count Graph") { start, end ->
                dbHelpher.productionCountGraph(start, end)
            }
        }



        oilLevelCardView.setOnClickListener {
            checkAndShowGraph("Oil Level Graph") { start, end ->
                dbHelpher.oilLevelGraph(start, end)
            }
        }



        stitchCountCardView.setOnClickListener {
            checkAndShowGraph("Stitch Count Graph") { start, end ->
                dbHelpher.stitchCountGraph(start, end)
            }
        }



        temperatureCardView.setOnClickListener {
            checkAndShowGraph("Temperature Graph") { start, end ->
                dbHelpher.temperatureGraph(start, end)
            }
        }



        vibrationCardView.setOnClickListener {
            checkAndShowGraph("Vibration Graph") { start, end ->
                dbHelpher.vibrationGraph(start, end)
            }
        }



        bobbinThreadCardView.setOnClickListener {
            checkAndShowGraph("Bobbin Thread Graph") { start, end ->
                dbHelpher.bobbinThreadGraph(start, end)
            }
        }



        stitchPerInchCardView.setOnClickListener {
            checkAndShowGraph("Stitches Per Inch  Graph") { start, end ->
                dbHelpher.stitchPerInchGraph(start, end)
            }
        }



    }

    private fun showGraphDialog(title: String,productionTimes: List<Pair<String, Number>>) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_chart)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        val barChart = dialog.findViewById<BarChart>(R.id.productionTimeChart)
        val titleTextView = dialog.findViewById<TextView>(R.id.title) // Get the TextView

        // Set dynamic title
        titleTextView.text = title

        // Ensure there is data to display
        if (productionTimes.isEmpty()) {
            Toast.makeText(this, "No production times available.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            return
        }

        // Extract labels (time in HH:mm format) and values
        val labels = productionTimes.map { it.first }  // Extracting time labels
        val entries = productionTimes.mapIndexed { index, pair ->
            BarEntry(index.toFloat(), pair.second.toFloat())  // Creating bar entries
        }

        // Setup Bar Chart Data
        val dataSet = BarDataSet(entries, "Production Time")
        dataSet.color = Color.RED
        dataSet.setDrawValues(true)  // Show values on bars

        val barData = BarData(dataSet)
        barData.barWidth = 0.3f  // Adjust bar width
        barChart.data = barData

        // X-Axis customization
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.granularity = 1f
        barChart.xAxis.textSize = 12f
        barChart.xAxis.textColor = Color.BLACK
        barChart.xAxis.labelRotationAngle = -45f // Rotate for better visibility

        // Y-Axis settings
        barChart.axisLeft.axisMinimum = 0f  // Ensure Y-axis starts from 0
        barChart.axisLeft.setDrawGridLines(false)
        barChart.xAxis.setDrawGridLines(false)
        barChart.axisRight.isEnabled = false // Hide right Y-axis

        // Hide description and legend
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false

        barChart.invalidate()  // Refresh chart
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
            p_temp.text = it.latestTemperature.toString()
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
        if (!this::startDate.isInitialized || !this::endDate.isInitialized) {
            // Show an alert dialog to prompt user to select a date range
            AlertDialog.Builder(this)
                .setTitle("Date Range Required")
                .setMessage("Please select a date range before viewing the graph.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        } else {
            val start = "$startDate 00:00:00"
            val end = "$endDate 23:59:59"
            val data = fetchData(start, end)

            if (data.isEmpty()) {
                Toast.makeText(this, "No data found in the database!", Toast.LENGTH_SHORT).show()
            } else {
                showGraphDialog(title, data)
            }
        }
    }



}