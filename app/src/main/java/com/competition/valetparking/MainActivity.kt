package com.competition.valetparking

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.naver.maps.map.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mNaverMap: NaverMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fm: FragmentManager = supportFragmentManager
        val mapFragment: MapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment

        mapFragment.getMapAsync(this)

        NaverMapSdk.getInstance(this).client =
            NaverMapSdk.NaverCloudPlatformClient(BuildConfig.NAVERMAP_CLIENT_ID)

        val parkingLayout: TableLayout = findViewById(R.id.parking_layout)
        var tableRow = TableRow(this)
        tableRow.layoutParams = TableRow.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val parkingMap = HashMap<Int, Boolean>()
        parkingMap[15] = true
        parkingMap[8] = true
        parkingMap[1] = true
        parkingMap[4] = true
        parkingMap[5] = true
        parkingMap[3] = true
        parkingMap[17] = true
        parkingMap[14] = true
        parkingMap[6] = true
        parkingMap[19] = true
        parkingMap[11] = true
        parkingMap[13] = true
        /*parkingMap[16] = true
        parkingMap[9] = true
        parkingMap[10] = true
        parkingMap[0] = true
        parkingMap[7] = true*/

        val saturationBar: ProgressBar = findViewById(R.id.saturation_bar)
        val progress: Int = (parkingMap.size * 100) / 20
        saturationBar.progress = progress
        val color: Int = when (progress) {
            in 1..25 -> R.color.green
            in 26..50 -> R.color.yellow
            in 51..75 -> R.color.orange
            else -> R.color.red
        }
        saturationBar.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(this, color))
        Log.d(localClassName, "progress: " + saturationBar.progress)

        for (i in 0 until 20) {
            val textView = TextView(this)
            textView.layoutParams = TableRow.LayoutParams(50, 110)
            textView.text = String.format("%02d", i + 1)
            textView.textSize = 16F
            textView.typeface = Typeface.create(textView.typeface, Typeface.BOLD)
            textView.setTextColor(Color.DKGRAY)
            textView.gravity = Gravity.CENTER
            textView.background = ColorDrawable(
                ContextCompat.getColor(
                    this, if (parkingMap[i] == true) {
                        R.color.using
                    } else {
                        R.color.free
                    }
                )
            )
            tableRow.addView(textView)
            if ((i + 1) % 4 == 0) {
                parkingLayout.addView(tableRow)
                tableRow = TableRow(this)
            }
        }
    }

    override fun onMapReady(naverMap: NaverMap) {
        mNaverMap = naverMap
        val uiSettings: UiSettings = mNaverMap.uiSettings
        uiSettings.isZoomControlEnabled = false
    }
}