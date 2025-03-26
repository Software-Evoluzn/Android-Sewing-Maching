package com.example.evoluznsewingmachine

data class MachineData(
    val totalTime: Int,
    val totalPushBackCount: Int,
    val totalStitchCount: Int,
    val latestTemperature: String,
    val latestVibration: String,
    val latestOilLevel: String,
    val latestThreadPercent: Float,
    val stitchPerInch: Int
)

