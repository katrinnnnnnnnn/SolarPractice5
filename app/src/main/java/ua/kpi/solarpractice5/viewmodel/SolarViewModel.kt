package ua.kpi.solarpractice5.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ua.kpi.solarpractice5.sensor.SensorRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

data class Sample(val voltage: Float, val current: Float, val temperature: Float, val power: Float)
data class MonitoringState(
    val readings: ua.kpi.solarpractice5.sensor.PhoneReadings = ua.kpi.solarpractice5.sensor.PhoneReadings(),
    val simulated: Sample = Sample(230f, 5f, 28f, 1150f),
    val history: List<Float> = emptyList(),
    val running: Boolean = false,
    val eventLog: List<String> = emptyList(),
    val acknowledgeCount: Int = 0
)
class SolarViewModel(app: Application) : AndroidViewModel(app) {
    private val sensors = SensorRepository(app)
    val hasAccelerometer get() = sensors.hasAccelerometer
    val hasLight get() = sensors.hasLight
    private val mutable = MutableStateFlow(MonitoringState())
    val state: StateFlow<MonitoringState> = mutable.asStateFlow()
    private var sampleJob: Job? = null
    init {
        viewModelScope.launch { sensors.readings().collect { phone ->
            mutable.value = mutable.value.copy(readings = phone)
        } }
    }
    fun start() {
        if (sampleJob?.isActive == true) return
        mutable.value = mutable.value.copy(running = true)
        sampleJob = viewModelScope.launch {
            var tick = 0
            while (true) {
                // Educational simulation: telephone sensors do not provide station voltage/current.
                val voltage = 228f + Random.nextFloat() * 7f
                val current = 3.5f + Random.nextFloat() * 3f
                val temperature = 25f + 6f * sin(tick / 6f)
                val power = voltage * current
                val next = Sample(voltage,current,temperature,power)
                val old = mutable.value
                mutable.value = old.copy(simulated=next,history=(old.history + power).takeLast(28))
                tick++
                delay(1200)
            }
        }
    }
    fun stop() { sampleJob?.cancel(); sampleJob=null; mutable.value = mutable.value.copy(running=false) }
    fun recordEvent(description: String) {
        mutable.value = mutable.value.copy(eventLog = (listOf(description)+mutable.value.eventLog).take(15))
    }
}
