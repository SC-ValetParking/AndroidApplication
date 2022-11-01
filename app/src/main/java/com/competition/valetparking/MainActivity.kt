package com.competition.valetparking

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.UiSettings
import java.util.Random

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mNaverMap: NaverMap
    private lateinit var mContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fm: FragmentManager = supportFragmentManager
        val mapFragment: MapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment

        mContext = baseContext

        mapFragment.getMapAsync(this)

        NaverMapSdk.getInstance(mContext).client =
            NaverMapSdk.NaverCloudPlatformClient(BuildConfig.NAVERMAP_CLIENT_ID)

        val saturationBar: ProgressBar = findViewById(R.id.saturation_bar)
        saturationBar.progressTintList = ColorStateList.valueOf(Color.parseColor("#FF9233"))

        val parkingLayout: TableLayout = findViewById(R.id.parking_layout)
        var tableRow = TableRow(mContext)
        tableRow.layoutParams = TableRow.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val random = Random()
        for (i in 1..20) {
            val textView = TextView(mContext)
            textView.layoutParams = TableRow.LayoutParams(50, 110)
            textView.text = String.format("%02d", i)
            textView.textSize = 16F
            textView.typeface = Typeface.create(textView.typeface, Typeface.BOLD)
            textView.setTextColor(Color.DKGRAY)
            textView.gravity = Gravity.CENTER
            textView.background = ColorDrawable(
                ContextCompat.getColor(mContext, if (random.nextInt(3) == 0) { R.color.using } else { R.color.free })
            )
            tableRow.addView(textView)
            if (i % 4 == 0) {
                parkingLayout.addView(tableRow)
                tableRow = TableRow(mContext)
            }
        }
    }

    override fun onMapReady(naverMap: NaverMap) {
        mNaverMap = naverMap
        val uiSettings: UiSettings = mNaverMap.uiSettings
        uiSettings.isZoomControlEnabled = false
    }
}