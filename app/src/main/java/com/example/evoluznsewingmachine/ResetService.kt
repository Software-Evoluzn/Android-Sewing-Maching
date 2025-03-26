package com.example.evoluznsewingmachine

import android.app.AlarmManager
import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ResetService : IntentService("ResetService") {
    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        val sharedPref = getSharedPreferences("MachinePrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        // Aaj ka date store karna for next day's sum
        val resetTime = SimpleDateFormat("yyyy-MM-dd 00:00:00", Locale.getDefault()).format(Date())
        editor.putString("resetTime", resetTime)
        editor.apply()

        Log.d("ResetService", "Daily Reset Time Updated: $resetTime")
    }

    fun scheduleDailyReset(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ResetService::class.java)
        val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0) // Midnight 12:00 AM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        // Agle din ke liye set karo agar abhi 12:00 baje ke baad hai
        if (System.currentTimeMillis() > calendar.timeInMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }


}
