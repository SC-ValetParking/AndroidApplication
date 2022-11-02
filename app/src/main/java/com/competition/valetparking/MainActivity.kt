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

    enum class SpecialType {    //특별주차구역 구분
        LIGHT, DISABLED     //경차, 장애인
    }

    data class ParkingData(     //parkingMap의 value으로 사용하기 위해 데이터클래스 생성
        var specialType: SpecialType, var using: Boolean    //특별주차구역 구분, 주차 여부
    )

    private lateinit var mNaverMap: NaverMap

    //아래 변수들은 테스트를 위한 임시 변수로, 실제 값이 들어오면 동적으로 구현할 예정.
    private val parkingSize = 20    //주차 칸 수 (특별주차구역 포함)
    private val tableRowLength = 4  //최대 행 길이

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fm: FragmentManager = supportFragmentManager
        val mapFragment: MapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment

        mapFragment.getMapAsync(this)

        NaverMapSdk.getInstance(this).client =
            NaverMapSdk.NaverCloudPlatformClient(BuildConfig.NAVERMAP_CLIENT_ID)

        val parkingMap = HashMap<Int, ParkingData?>() //현재 주차중인 칸 or 특별주차구역 칸
        parkingMap[1] = null                        //null: 일반주차구역 사용중, ParkingData: 특별주차구역 구분과 주차여부
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

        val saturationBar: ProgressBar = findViewById(R.id.saturation_bar)  //포화도
        val progress: Int =
            (parkingMap.filterValues { it == null }.size * 100) / parkingSize   //filterValues { it == null } ← 이건 parkingMap에서 value가 null인 항목만 찾겠다는 뜻이에요.
        saturationBar.progress = progress
        Log.d(localClassName, "progress: $progress")
        val color: Int = when (progress) {      //포화도 색상 선택
            in 1..25 -> R.color.green
            in 26..50 -> R.color.yellow
            in 51..75 -> R.color.orange
            else -> R.color.red
        }
        saturationBar.progressTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, color))    //포화도 색상 변경

        val generalArea: TableLayout = findViewById(R.id.general_parking_layout)    //일반주차구역
        val specialArea: TableLayout = findViewById(R.id.special_parking_layout)    //특수주차구역
        val specialList = ArrayList<Int>()  //특수주차구역 목록 생성
        var generalChild = TableRow(this)   //일반주차구역의 Row 생성

        var seek = 0    //특수주차구역 때문에 비어버린 칸을 당겨오기 위한 변수
        for (i in 1..parkingSize) {
            if (parkingMap[i] == null) {    //일반주차구역인 경우 (일반주차구역이 아닌 경우)
                val textView = newLocText()     //하단의 TableRow에 할당하게 되면, TextView를 새로 만들어야 함
                textView.text = String.format("%02d", i)
                textView.background = ColorDrawable(    //TextView 배경 색상 지정
                    ContextCompat.getColor(
                        this,
                        if (parkingMap.containsKey(i)) {  //parkingMap에 i라는 키 값이 있다면 (위에서 특수주차구역에 대한 검사를 했기 때문에 일반주차구역으로 간주)
                            R.color.using
                        } else {
                            R.color.free
                        }
                    )
                )
                generalChild.addView(textView)
                seek++  //i와 별개로 seek 변수를 증가함으로서 특수주차구역을 제외한 칸 수만 카운트함
            } else specialList.add(i)   //특수주차구역인 경우 특수주차구역 목록에 칸 번호 추가
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
            val image = when (parkingMap[value]!!.specialType) {    //구분 이미지 선택
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
            if ((i + 1) % tableRowLength == 0 || (i + 1) == specialList.size) {
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