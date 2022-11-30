package com.competition.valetparking

import android.app.ActionBar.LayoutParams
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import android.widget.AdapterView.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.Overlay.OnClickListener

class MainActivity : AppCompatActivity(), OnMapReadyCallback, OnClickListener {

    enum class SpecialType(val value: Int) {    //특수주차구역 구분
        DISABLED(0), LIGHT(1), ELECTRIC(2);     //장애인, 경차, 전기차

        companion object {
            fun fromInt(value: Int) = values().first { it.value == value }
        }
    }

    data class FloorData(
        var parkingMap: HashMap<Int, ParkingData?>, var parkingSize: Int
    )

    data class ParkingData(     //parkingMap의 value으로 사용하기 위해 데이터클래스 생성
        var specialType: SpecialType, var using: Boolean    //특수주차구역 구분, 주차 여부
    )

    private lateinit var mNaverMap: NaverMap

    //Layout 변수
    private lateinit var generalArea: TableLayout
    private lateinit var specialArea: TableLayout
    private lateinit var floorSpinner: Spinner

    private val tableRowLength = 6  //최대 행 길이
    private val coordinateMap = HashMap<LatLng, DocumentReference?>()
    private val floorList: MutableList<FloorData> = arrayListOf()

    //Floor focus 유지를 위한 변수들
    private var prevMarker: Marker? = null
    private var recentMarker: Marker? = null
    private var prevFloor: Int? = null

    //reference listener 중복 생성 방지를 위한 변수
    private var referenceListener: ListenerRegistration? = null

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fm: FragmentManager = supportFragmentManager
        val mapFragment: MapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment

        mapFragment.getMapAsync(this)

        NaverMapSdk.getInstance(this).client =
            NaverMapSdk.NaverCloudPlatformClient(BuildConfig.NAVERMAP_CLIENT_ID)

        generalArea = findViewById(R.id.general_parking_layout)    //일반주차구역
        specialArea = findViewById(R.id.special_parking_layout)    //특수주차구역
        floorSpinner = findViewById(R.id.floor_spinner)

        val db = Firebase.firestore
        db.collection("Coordinates").get().addOnSuccessListener { coordinateData ->
            for (s1 in coordinateData) {
                val geoPoint = s1.getGeoPoint("geoPoint")
                if (geoPoint != null) {
                    val location = LatLng(geoPoint.latitude, geoPoint.longitude)
                    val marker = Marker()
                    marker.position = location
                    marker.onClickListener = this
                    marker.map = mNaverMap
                    coordinateMap[location] = s1.getDocumentReference("reference")
                }
            }
            val lastGeoPoint = coordinateData.last().getGeoPoint("geoPoint")
            if (lastGeoPoint != null) mNaverMap.moveCamera(
                CameraUpdate.scrollTo(
                    LatLng(
                        lastGeoPoint.latitude, lastGeoPoint.longitude
                    )
                ).animate(CameraAnimation.Fly)
            )
        }.addOnFailureListener { exception ->
            Log.w(TAG, "Error getting documents, $exception")
        }

        floorSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, selectFloor: Int, id: Long
            ) {
                if (prevMarker != recentMarker || prevFloor != selectFloor) {
                    prevFloor = selectFloor
                    drawParkingLayout(floorList[selectFloor])
                } else {
                    floorSpinner.setSelection(prevFloor!!)
                    drawParkingLayout(floorList[prevFloor!!])
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onMapReady(naverMap: NaverMap) {
        mNaverMap = naverMap
        val uiSettings: UiSettings = mNaverMap.uiSettings
        uiSettings.isZoomControlEnabled = false
    }

    override fun onClick(p0: Overlay): Boolean {
        if (p0 is Marker) {
            mNaverMap.moveCamera(CameraUpdate.scrollTo(p0.position).animate(CameraAnimation.Easing))

            val reference = coordinateMap[p0.position] ?: return false
            referenceListener?.remove()

            referenceListener = reference.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) Log.d(TAG, "Current data: null")

                floorList.clear()
                val data = snapshot?.data
                val p1 = data?.get("floorArray") as MutableList<*>
                for (floor in p1) {
                    floor as HashMap<*, *>
                    val parkingSize = anyToInt(floor["parkingSize"])
                    val parkingMap = HashMap<Int, ParkingData?>()
                    for ((key, value) in floor["parkingMap"] as Map<*, *>) {
                        val values = value as List<*>?
                        parkingMap[anyToInt(key)] = if (values != null) ParkingData(
                            SpecialType.fromInt(anyToInt(values[0])), anyToBoolean(values[1])
                        ) else null
                    }
                    floorList.add(FloorData(parkingMap, parkingSize))
                }
                if (floorSpinner.visibility == INVISIBLE) floorSpinner.visibility = VISIBLE
                prevMarker = recentMarker
                recentMarker = p0

                if (floorSpinner.adapter == null || floorSpinner.adapter.count != floorList.size) {
                    val spinnerList = ArrayList<String>()
                    for (i in 1..floorList.size) spinnerList.add("$i Floor")
                    val spinnerAdapter: ArrayAdapter<String> = ArrayAdapter(
                        this, android.R.layout.simple_spinner_dropdown_item, spinnerList
                    )
                    floorSpinner.adapter = spinnerAdapter
                } else if (prevFloor != null) drawParkingLayout(floorList[prevFloor!!])
            }
            return true
        }
        return false
    }

    private fun drawParkingLayout(floorData: FloorData) {
        generalArea.removeAllViews()
        specialArea.removeAllViews()

        val specialList = ArrayList<Int>()  //특수주차 칸 목록 생성

        var generalChild = TableRow(this)   //일반주차구역의 Row 생성
        var seek = 0    //특수주차 칸 때문에 비어버린 칸을 당겨오기 위한 변수

        val parkingSize = floorData.parkingSize
        val parkingMap = floorData.parkingMap

        for (i in 1..parkingSize) {
            if (parkingMap[i] == null) {    //일반주차 칸인 경우 (특수주차 칸이 아닌 경우)
                val textView = newLocText()     //하단의 TableRow에 할당하게 되면, TextView를 새로 만들어야 함
                textView.text = String.format("%02d", i)
                textView.setBackgroundColor(    //TextView 배경 색상 지정
                    ContextCompat.getColor(
                        this,
                        if (parkingMap.containsKey(i)) R.color.using    //parkingMap에 i라는 키 값이 있다면 (위에서 특수주차구역에 대한 검사를 했기 때문에 일반주차구역으로 간주)
                        else R.color.free
                    )
                )
                generalChild.addView(textView)
                seek++  //i와 별개로 seek 변수를 증가함으로서 특수주차구역을 제외한 칸 수만 카운트함
            } else specialList.add(i)   //특수주차 칸인 경우 특수주차 칸 목록에 해당 칸 번호 추가
            if (seek % tableRowLength == 0 || i == parkingSize) {    //일반주차구역 테이블에 행 길이 tableRowLength 만큼 넣음.
                generalArea.addView(generalChild)       //만약 현재 루프가 마지막 칸일 경우 최대 행 길이가 채워지지 않았더도 넣음.
                generalChild = TableRow(this)
            }
        }

        var specialChild = TableRow(this)   //특수주차구역의 Row 생성
        for (i in 0 until specialList.size) {
            val value = specialList[i]
            val linearLayout = LinearLayout(this)
            val imageView = ImageView(this)
            val textView = newLocText()
            textView.text = String.format("%02d", value)
            val image = when (parkingMap[value]!!.specialType) {    //구분 이미지 선택
                SpecialType.LIGHT -> R.drawable.ic_baseline_light_car
                SpecialType.ELECTRIC -> R.drawable.ic_baseline_electric_car
                else -> R.drawable.ic_baseline_disabled
            }
            imageView.setImageResource(image)
            imageView.imageTintList = ColorStateList.valueOf(Color.DKGRAY)
            imageView.layoutParams =
                TableRow.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
            linearLayout.orientation = LinearLayout.HORIZONTAL
            linearLayout.gravity = Gravity.CENTER
            linearLayout.setBackgroundColor(
                ContextCompat.getColor(
                    this, if (parkingMap[value]!!.using) R.color.using
                    else R.color.free
                )
            )
            linearLayout.addView(imageView)
            linearLayout.addView(textView)
            specialChild.addView(linearLayout)
            if ((i + 1) % tableRowLength == 0 || (i + 1) == specialList.size) {
                specialArea.addView(specialChild)
                specialChild = TableRow(this)
            }
        }

        val saturationBar: ProgressBar = findViewById(R.id.saturation_bar)  //포화도 막대
        val saturation: Int =
            (parkingMap.filterValues { it == null }.size * 100) / (parkingSize - specialList.size)
        //filterValues { it == null } ← 이건 parkingMap에서 value가 null인 항목만 찾겠다는 뜻이에요.
        val color: Int = when (saturation) {      //포화도 색상 선택
            in 1..25 -> R.color.green
            in 26..50 -> R.color.yellow
            in 51..75 -> R.color.orange
            else -> R.color.red
        }
        saturationBar.progressTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, color))    //포화도 색상 변경
        saturationBar.progress = saturation
    }

    private fun newLocText(): TextView {
        val locText = TextView(this)
        locText.layoutParams = TableRow.LayoutParams(100, 120)
        locText.textSize = 16F
        locText.typeface = Typeface.create(locText.typeface, Typeface.BOLD)
        locText.setTextColor(Color.DKGRAY)
        locText.gravity = Gravity.CENTER
        return locText
    }

    companion object {
        fun anyToInt(value: Any?) = value.toString().toInt()
        fun anyToBoolean(value: Any?) = value.toString().toBoolean()
    }
}