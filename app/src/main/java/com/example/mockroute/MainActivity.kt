package com.example.mockroute

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var baiduMap: BaiduMap
    private lateinit var tvPatrolStatus: TextView
    private lateinit var btnClear: Button
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    private val patrolPointsBd = mutableListOf<LatLng>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.bmapView)
        baiduMap = mapView.map
        tvPatrolStatus = findViewById(R.id.tvPatrolStatus)
        btnClear = findViewById(R.id.btnClearPatrolPoints)
        btnStart = findViewById(R.id.btnStartPatrol)
        btnStop = findViewById(R.id.btnStopPatrol)

        // 地图点击：添加点
        baiduMap.setOnMapClickListener(object : BaiduMap.OnMapClickListener {
            override fun onMapClick(point: LatLng) {
                patrolPointsBd.add(point)
                val markerOptions = MarkerOptions()
                    .position(point)
                    .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_myplaces))
                baiduMap.addOverlay(markerOptions)
                tvPatrolStatus.text = "已选定 ${patrolPointsBd.size} 个点"
            }

            override fun onMapPoiClick(poi: MapPoi?) {
                poi?.let { onMapClick(it.position) }
            }
        })

        // 清空点位
        btnClear.setOnClickListener {
            patrolPointsBd.clear()
            baiduMap.clear()
            tvPatrolStatus.text = "点位已清空，请点击地图选点"
        }

        // 开启 10 秒切换
        btnStart.setOnClickListener {
            if (patrolPointsBd.size < 2) {
                Toast.makeText(this, "请在地图上点击至少 2 个点", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val wgsLats = DoubleArray(patrolPointsBd.size)
            val wgsLngs = DoubleArray(patrolPointsBd.size)

            for (i in patrolPointsBd.indices) {
                val wgs = CoordinateTransformUtil.bd09ToWgs84(
                    patrolPointsBd[i].latitude,
                    patrolPointsBd[i].longitude
                )
                wgsLats[i] = wgs.latitude
                wgsLngs[i] = wgs.longitude
            }

            val serviceIntent = Intent(this, PointPatrolService::class.java).apply {
                putExtra("EXTRA_PATROL_LATS", wgsLats)
                putExtra("EXTRA_PATROL_LNGS", wgsLngs)
                putExtra("EXTRA_INTERVAL_MS", 10_000L)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Toast.makeText(this, "10秒轮巡模拟已启动！", Toast.LENGTH_SHORT).show()
        }

        // 停止
        btnStop.setOnClickListener {
            stopService(Intent(this, PointPatrolService::class.java))
            Toast.makeText(this, "模拟已停止", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}
