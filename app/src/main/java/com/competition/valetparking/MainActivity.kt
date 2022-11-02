package com.competition.valetparking

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.naver.maps.map.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    enum class SpecialType(val type: Int) {
        LIGHT(1), DISABLED(2)
    }

    data class ParkingData(
        var specialType: SpecialType,
        var using: Boolean
    )

    private lateinit var mNaverMap: NaverMap
    private val parkingSize = 20

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fm: FragmentManager = supportFragmentManager
        val mapFragment: MapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment

        mapFragment.getMapAsync(this)

        NaverMapSdk.getInstance(this).client =
            NaverMapSdk.NaverCloudPlatformClient(BuildConfig.NAVERMAP_CLIENT_ID)

        val parkingMap = HashMap<Int, ParkingData?>() //number, ParkingData{ specialType, using }
        parkingMap[1] = null
        parkingMap[3] = null
        parkingMap[4] = null
        parkingMap[5] = ParkingData(SpecialType.DISABLED, false)
        parkingMap[6] = null
        parkingMap[7] = null
        parkingMap[8] = ParkingData(SpecialType.DISABLED, false)
        parkingMap[9] = null
        parkingMap[11] = ParkingData(SpecialType.LIGHT, false)
        parkingMap[12] = null
        parkingMap[13] = null
        parkingMap[14] = null
        parkingMap[17] = null
        parkingMap[19] = null
        parkingMap[20] = ParkingData(SpecialType.LIGHT, true)

        val saturationBar: ProgressBar = findViewById(R.id.saturation_bar)
        val progress: Int = (parkingMap.filterValues { it == null }.size * 100) / parkingSize
        saturationBar.progress = progress
        Log.d(localClassName, "progress: $progress")
        val color: Int = when (progress) {
            in 1..25 -> R.color.green
            in 26..50 -> R.color.yellow
            in 51..75 -> R.color.orange
            else -> R.color.red
        }
        saturationBar.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(this, color))

        val generalArea: TableLayout = findViewById(R.id.general_parking_layout)
        val specialArea: TableLayout = findViewById(R.id.special_parking_layout)
        val specialList = ArrayList<Int>()
        var generalChild = TableRow(this)

        var peek = 0
        for (i in 1..parkingSize) {
            if (parkingMap[i] != null) {
                specialList.add(i)
                continue
            }
            val textView = newLocText()
            textView.text = String.format("%02d", i)
            textView.background = ColorDrawable(
                ContextCompat.getColor(
                    this, if (parkingMap.containsKey(i)) {
                        R.color.using
                    } else {
                        R.color.free
                    }
                )
            )
            generalChild.addView(textView)
            peek++
            if (peek % 4 == 0 || i == parkingSize) {
                generalArea.addView(generalChild)
                generalChild = TableRow(this)
            }
        }

        var specialChild = TableRow(this)
        for (i in 0 until specialList.size) {
            val value = specialList[i]
            val linearLayout = LinearLayout(this)
            val imageView = ImageView(this)
            val textView = newLocText()
            val image = when (parkingMap[value]!!.specialType) {
                SpecialType.LIGHT -> R.drawable.ic_baseline_car
                else -> R.drawable.ic_baseline_disabled
            }
            imageView.setImageResource(image)
            textView.text = String.format("%02d", value)
            linearLayout.orientation = LinearLayout.HORIZONTAL
            linearLayout.gravity = Gravity.CENTER
            linearLayout.background = ColorDrawable(
                ContextCompat.getColor(
                    this, if (parkingMap[value]!!.using) {
                        R.color.using
                    } else {
                        R.color.free
                    }
                )
            )
            linearLayout.addView(imageView)
            linearLayout.addView(textView)
            specialChild.addView(linearLayout)
            if ((i + 1) % 4 == 0 || i == specialList.size) {
                specialArea.addView(specialChild)
                specialChild = TableRow(this)
            }
        }
    }

    override fun onMapReady(naverMap: NaverMap) {
        mNaverMap = naverMap
        val uiSettings: UiSettings = mNaverMap.uiSettings
        uiSettings.isZoomControlEnabled = false
    }

    private fun newLocText(): TextView {
        val locText = TextView(this)
        locText.layoutParams = TableRow.LayoutParams(50, 110)
        locText.textSize = 16F
        locText.typeface = Typeface.create(locText.typeface, Typeface.BOLD)
        locText.setTextColor(Color.DKGRAY)
        locText.gravity = Gravity.CENTER
        return locText
    }
}