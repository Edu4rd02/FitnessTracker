package com.example.fitnesstracker

import android.annotation.SuppressLint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import kotlin.math.sqrt

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity(), SensorEventListener {
    // UI Views
    private lateinit var tvMotion: TextView
    private lateinit var tvGyro: TextView
    private lateinit var tvDirection: TextView
    private lateinit var tvAccuracy: TextView
    private lateinit var btnCalibrate: Button
    private lateinit var btnRefreshChart: Button
    private lateinit var barChart: BarChart

    // Sensors
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null

    // Sensor fusion - needed for compass
    private var accelValues = FloatArray(size = 3)
    private var magnetValues = FloatArray(size = 3)

    // Calibration baseline
    private var baselineX = 0f
    private var baselineY = 0f
    private var baselineZ = 9.8f

    private var isCalibrating = false
    private val calibrationSamples = mutableListOf<FloatArray>()

    // Sample data for chart
    private val hourlySteps = floatArrayOf(1000f, 1200f, 0f, 800f)

    companion object {
        private const val STATIONARY_THRESHOLD = 2.0f
        private const val WALKING_THRESHOLD = 12.0f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState = savedInstanceState)
        setContentView(R.layout.activity_main)

        tvMotion = findViewById(R.id.tvMotion)
        tvGyro = findViewById(R.id.tvGyro)
        tvDirection = findViewById(R.id.tvDirection)
        tvAccuracy = findViewById(R.id.tvAccuracy)
        btnCalibrate = findViewById(R.id.btnCalibrate)
        btnRefreshChart = findViewById(R.id.btnRefreshChart)
        barChart = findViewById(R.id.barChart)

        initSensors()
        setupChart()
        btnCalibrate.setOnClickListener { startCalibration() }
        btnRefreshChart.setOnClickListener { setupChart() }
    }

    override fun onResume(){
        super.onResume()
        accelerometer?.let {sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)}
        gyroscope?.let {sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)}
        magnetometer?.let {sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)}
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    // FINAL REPORT: adding onStop() override + call unregisterListener(this)

    private fun initSensors(){
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accelValues = event.values.clone()
                handleAccelerometer(event.values)
            }
            Sensor.TYPE_GYROSCOPE -> {
                handleGyroscope(event.values)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magnetValues = event.values.clone()
                updateCompass()
            }
        }
    }

    // Pre-built: handleAccelerometer
    private fun handleAccelerometer(values: FloatArray){
        val x = values[0] - baselineX
        val y = values[1] - baselineY
        val z = values[2] - baselineZ
        val magnitude = sqrt(x * x + y * y + z * z)
        tvMotion.text = "Motion: ${classifyMotion(magnitude)}"

        if (isCalibrating) {
            calibrationSamples.add(floatArrayOf(values[0],values[1],values[2]))

            if (calibrationSamples.size > 20) finishCalibration()
        }
    }

    private fun classifyMotion(magnitude: Float): String {
        return when {
            magnitude < STATIONARY_THRESHOLD -> "Stationary"
            magnitude < WALKING_THRESHOLD -> "Walking"
            else -> "Jogging"
        }
    }

    private fun handleGyroscope(values: FloatArray){
        val pitch = values[0]
        val roll = values[1]
        val yaw = values [2]
        tvGyro.text = "Rotation (Pitch: ${"%.2f".format(pitch)}, Roll: ${"%.2f".format(roll)}, ${"%.2f".format(yaw)})"
    }

    private fun updateCompass(){
        val rotationMatrix = FloatArray(9)
        val inclinationMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        SensorManager.getRotationMatrix(rotationMatrix,inclinationMatrix,accelValues,magnetValues)
        SensorManager.getOrientation(rotationMatrix,orientation)

        var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
        if (azimuthDeg < 0) azimuthDeg += 360f
        val direction = when {
            azimuthDeg < 22.5f || azimuthDeg >= 337.5f -> "North"
            azimuthDeg < 67.5f  -> "Northeast"
            azimuthDeg < 112.5f -> "East"
            azimuthDeg < 157.5f -> "Southeast"
            azimuthDeg < 202.5f -> "South"
            azimuthDeg < 247.5f -> "Southwest"
            azimuthDeg < 292.5f -> "West"
            else                -> "Northwest"
        }
        tvDirection.text = "Direction: $azimuthDeg ($direction)"
    }

    private fun setupChart(){
        val entries = hourlySteps.mapIndexed { index, value ->
            BarEntry(index.toFloat(), value)
        }
        val dataSet = BarDataSet(entries, "Steps").apply {
            color = 0xFF80DEEA.toInt()
        }
        barChart.data = BarData(dataSet)
        barChart.description.text ="Step count per hour"
        barChart.invalidate()
    }

    // Final report
    // Show tvAccuracy warning if accuracy == SENSOR_STATUS_UNRELIABLE or ACCURACY_LOW
    override fun onAccuracyChanged(sensor: Sensor, accurarcy: Int) {

    }

    // Final report
    // Clear samples, set isCalibration = true, disable, Toast "hold the device steady"
    private fun startCalibration(){

    }

    // Final report
    // Average calibrationSamples for each axis -> set baselineX, baselineY, baselineZ
    private fun finishCalibration(){

    }
}
