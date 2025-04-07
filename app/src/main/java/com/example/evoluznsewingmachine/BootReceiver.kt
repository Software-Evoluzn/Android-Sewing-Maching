package com.example.evoluznsewingmachine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            context?.let {
                Handler(Looper.getMainLooper()).postDelayed({
                        startUsbService(it)
                    }, 1000) // Delay 1 seconds to allow system boot stabilization

            }
        }
    }

    private fun startUsbService(context: Context) {
        val serviceIntent = Intent(context, UsbService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}


