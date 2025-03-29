package com.example.evoluznsewingmachine

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
                        Log.d("Today date","today date : $today")
                        applyBtn.setOnClickListener {
                            if(today.isNotEmpty()){
                                fetchAndDisplayData(today, today)  // Fetch data for today
                            }
                        }



                    }
                    "Set Range" -> {
                        showDateRangePicker { start, end ->
                            Log.d("Start and end Date " , "start date : $start and end date: $end")
                            setDate.text = " $start  to  $end "
                            applyBtn.setOnClickListener {
                                if(start.isNotEmpty() && end.isNotEmpty()){
                                    fetchAndDisplayData(start, end)  // Fetch data for selected range
                                }
                            }


                        }
                    }
                }
                true
            }
            popupMenu.show()
        }










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



}