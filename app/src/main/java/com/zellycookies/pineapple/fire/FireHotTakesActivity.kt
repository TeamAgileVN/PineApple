package com.zellycookies.pineapple.fire

import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseUser
import com.zellycookies.pineapple.R
import com.zellycookies.pineapple.utils.GPS
import com.zellycookies.pineapple.utils.TopNavigationViewHelper

class FireHotTakesActivity : AppCompatActivity(), OnMapReadyCallback {

    private var tabLayout : TabLayout? = null

    private val mContext: Context = this@FireHotTakesActivity

    private var mMap: GoogleMap? = null
    var location: Location?=null
    var latitude=10.79474099959847
    var longitude=106.70861138817237
    lateinit var user: FirebaseUser
    private lateinit var gps: GPS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fire_hot_takes)
        setupTabLayout()
        setupTopNavigationView()
    }


    


    fun drawUser(latitude: Double, longitude: Double){
        if (mMap != null) {
            val me = LatLng(latitude, longitude)
            mMap!!.addMarker(
                MarkerOptions()
                .position(me)
                .title("Me")
                .snippet(" here is my location")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.charmander)))
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 14f))
        }

    }
















    private fun setupTabLayout() {
        tabLayout = findViewById(R.id.tabLayout)

        tabLayout!!.addTab(tabLayout!!.newTab().setText("Hot Takes"))
        tabLayout!!.addTab(tabLayout!!.newTab().setText("Search"))
        tabLayout!!.tabGravity = TabLayout.GRAVITY_FILL

        tabLayout!!.getTabAt(TAB_NUM)?.select()

        tabLayout!!.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab!!.position) {
                    1 -> {
                        val intent = Intent(this@FireHotTakesActivity, FireSearchActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupTopNavigationView() {
        Log.d(TAG, "setupTopNavigationView: setting up TopNavigationView")
        val tvEx = findViewById<View>(R.id.topNavViewBar) as BottomNavigationView
        TopNavigationViewHelper.setupTopNavigationView(tvEx)
        TopNavigationViewHelper.enableNavigation(mContext, tvEx)
        val menu = tvEx.menu
        val menuItem = menu.getItem(ACTIVITY_NUM)
        menuItem.isChecked = true
    }

    companion object {
        private const val TAG = "FireHotTakesActivity"
        private const val TAB_NUM = 0
        private const val ACTIVITY_NUM = 1
    }
}