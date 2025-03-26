package com.example.evoluznsewingmachine

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object UsbDataRepository {
    private val _productionTime = MutableLiveData<String>()
    val productionTime: LiveData<String> get() = _productionTime

    private val _productionCount = MutableLiveData<String>()
    val productionCount: LiveData<String> get() = _productionCount

    private val _temperature = MutableLiveData<String>()
    val temperature: LiveData<String> get() = _temperature

    private val _vibrationValue = MutableLiveData<String>()
    val vibrationValue: LiveData<String> get() = _vibrationValue

    private val _oilLevel = MutableLiveData<String>()
    val oilLevel: LiveData<String> get() = _oilLevel

    private val _threadPercent = MutableLiveData<String>()
    val threadPercent: LiveData<String> get() = _threadPercent

    private val _stitchCount = MutableLiveData<String>()
    val stitchCount: LiveData<String> get() = _stitchCount

    private val _threadConsumption = MutableLiveData<String>()
    val threadConsumption: LiveData<String> get() = _threadConsumption

    fun updateUsbData(
        productionTime: String, productionCount: String, temperature: String, vibrationValue: String,
        oilLevel: String, threadPercent: String, stitchCount: String, threadConsumption: String
    ) {
        _productionTime.postValue(productionTime)
        _productionCount.postValue(productionCount)
        _temperature.postValue(temperature)
        _vibrationValue.postValue(vibrationValue)
        _oilLevel.postValue(oilLevel)
        _threadPercent.postValue(threadPercent)
        _stitchCount.postValue(stitchCount)
        _threadConsumption.postValue(threadConsumption)
    }
}
