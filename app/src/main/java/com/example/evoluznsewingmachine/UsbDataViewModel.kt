package com.example.evoluznsewingmachine

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

class UsbDataViewModel : ViewModel() {
    val productionTime: LiveData<String> = UsbDataRepository.productionTime
    val productionCount: LiveData<String> = UsbDataRepository.productionCount
    val temperature: LiveData<String> = UsbDataRepository.temperature
    val vibrationValue: LiveData<String> = UsbDataRepository.vibrationValue
    val oilLevel: LiveData<String> = UsbDataRepository.oilLevel
    val threadPercent: LiveData<String> = UsbDataRepository.threadPercent
    val stitchCount: LiveData<String> = UsbDataRepository.stitchCount
    val threadConsumption: LiveData<String> = UsbDataRepository.threadConsumption

}
