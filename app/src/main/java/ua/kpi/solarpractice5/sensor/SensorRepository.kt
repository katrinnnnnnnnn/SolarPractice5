package ua.kpi.solarpractice5.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

// SensorManager supplies physical device sensors. This layer never invents absent sensors.
data class PhoneReadings(val acceleration: Float? = null, val light: Float? = null)
class SensorRepository(context: Context) {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val hasAccelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    val hasLight = manager.getDefaultSensor(Sensor.TYPE_LIGHT) != null

    fun readings(): Flow<PhoneReadings> = callbackFlow {
        var snapshot = PhoneReadings()
        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            override fun onSensorChanged(event: SensorEvent) {
                snapshot = when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        val v = event.values
                        snapshot.copy(acceleration = sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]))
                    }
                    Sensor.TYPE_LIGHT -> snapshot.copy(light = event.values[0])
                    else -> snapshot
                }
                trySend(snapshot)
            }
        }
        listOfNotNull(manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            manager.getDefaultSensor(Sensor.TYPE_LIGHT)).forEach { sensor ->
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        trySend(snapshot)
        awaitClose { manager.unregisterListener(listener) }
    }
}
