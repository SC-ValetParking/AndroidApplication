package com.competition.valetparking

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import androidx.fragment.app.FragmentManager
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.UiSettings

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var mNaverMap: NaverMap;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fm: FragmentManager = supportFragmentManager
        val mapFragment: MapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment

        mapFragment.getMapAsync(this)

        NaverMapSdk.getInstance(this).client =
            NaverMapSdk.NaverCloudPlatformClient(BuildConfig.NAVERMAP_CLIENT_ID)
    }

    override fun onMapReady(naverMap: NaverMap) {
        mNaverMap = naverMap
        val uiSettings: UiSettings = mNaverMap.uiSettings
        uiSettings.isZoomControlEnabled = false
    }
}