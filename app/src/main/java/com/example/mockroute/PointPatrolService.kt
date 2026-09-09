package com.example.mockroute

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class PointPatrolService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)
    private lateinit var locationManager: LocationManager
    private val providerName = LocationManager.GPS_PROVIDER

    private var points: List<CoordinateTransformUtil.LatLngPoint> = emptyList()
    private var currentIndex = 0
    private var intervalMs: Long = 10_000L
    private var isPatrolling = false

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        startForeground(1004, buildNotification("多点轮巡准备中"))
        setupTestProvider()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val lats = intent?.getDoubleArrayExtra("EXTRA_PATROL_LATS")
        val lngs = intent?.getDoubleArrayExtra("EXTRA_PATROL_LNGS")
        intervalMs = intent?.getLongExtra("EXTRA_INTERVAL_MS", 10_000L) ?: 10_000L

        if (lats != null && lngs != null && lats.size == lngs.size) {
            points = lats.indices.map {
                CoordinateTransformUtil.LatLngPoint(lats[it], lngs[it])
            }
            currentIndex = 0

            if (!isPatrolling && points.isNotEmpty()) {
                isPatrolling = true
                startPatrolLoop()
            }
        }
        return START_STICKY
    }

    private fun startPatrolLoop() {
        scope.launch {
            while (isActive && isPatrolling && points.isNotEmpty()) {
                val targetPoint = points[currentIndex]
                emitMockLocation(targetPoint.latitude, targetPoint.longitude)
                updateNotification("当前模拟第 ${currentIndex + 1}/${points.size} 个点")
                currentIndex = (currentIndex + 1) % points.size
                delay(intervalMs)
            }
        }
    }

    private fun emitMockLocation(lat: Double, lng: Double) {
        val loc = Location(providerName).apply {
            latitude = lat
            longitude = lng
            altitude = 10.0
            bearing = 0.0f
            speed = 0.0f
            accuracy = 3.0f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        try {
            locationManager.setTestProviderLocation(providerName, loc)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupTestProvider() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val properties = ProviderProperties.Builder()
                    .setHasNetworkRequirement(false)
                    .setHasSatelliteRequirement(true)
                    .setHasCellRequirement(false)
                    .setHasMonetaryCost(false)
                    .setSupportsAltitude(true)
                    .setSupportsSpeed(true)
                    .setSupportsBearing(true)
                    .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
                    .setAccuracy(Criteria.ACCURACY_FINE)
                    .build()
                locationManager.addTestProvider(providerName, properties)
            } else {
                @Suppress("DEPRECATION")
                locationManager.addTestProvider(
                    providerName,
                    false, false, false, false,
                    true, true, true,
                    Criteria.POWER_LOW, Criteria.ACCURACY_FINE
                )
            }
            locationManager.setTestProviderEnabled(providerName, true)
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isPatrolling = false
        job.cancel()
        try {
          locationManager.removeTestProvider(providerName)
      } catch (e: Exception) {
          e.printStackTrace()
      }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(text: String): Notification {
        val channelId = "patrol_location_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "多点轮巡服务", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("多点轮巡模式中")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1004, buildNotification(text))
    }
}
