package com.example.walkingpath

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
import com.google.android.gms.location.FusedLocationProviderApi
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.navigation.NavigationView
import java.util.*

class PathAcitivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, ConnectionCallbacks,
    OnConnectionFailedListener, OnRequestPermissionsResultCallback,
    LocationListener {
    private var mMap: GoogleMap? = null
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var mCurrentLocation: Location? = null
    private val mFusedLocationProviderApi: FusedLocationProviderApi? = null
    private var mPermissionDenied = false
    private val locationManager: LocationManager? = null
    private var mCurrentMarker: Marker? = null
    private var startLatLng = LatLng(0.0, 0.0) //polyline 시작점
    private var endLatLng = LatLng(0.0, 0.0) //polyline 끝점
    private var walkState = false //걸음 상태
    private val polylines = mutableListOf<Polyline>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_walking)
        //        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
        val fab =
            findViewById<View>(R.id.walking_start) as Button
        fab.setOnClickListener {
            changeWalkState() //걸음 상태 변경
        }
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
        if (mGoogleApiClient == null) {
            mGoogleApiClient = GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()
        }
        createLocationRequest()
    }

    private fun changeWalkState() {
        if (!walkState) {
            Toast.makeText(applicationContext, "걸음 시작", Toast.LENGTH_SHORT).show()
            walkState = true
            startLatLng = LatLng(
                mCurrentLocation!!.latitude,
                mCurrentLocation!!.longitude
            ) //현재 위치를 시작점으로 설정
        } else {
            Toast.makeText(applicationContext, "걸음 종료", Toast.LENGTH_SHORT).show()
            walkState = false
        }
    }

    private fun drawPath() {        //polyline을 그려주는 메소드
        val options = PolylineOptions().add(startLatLng).add(endLatLng).width(15f)
            .color(Color.BLACK).geodesic(true)
        polylines.add(mMap!!.addPolyline(options))
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 18f))
    }

    override fun onStart() {
        super.onStart()
        mGoogleApiClient!!.connect()
    }

    override fun onStop() {
        super.onStop()
        mGoogleApiClient!!.disconnect()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    override fun onConnected(bundle: Bundle?) {
        enableMyLocation()
    }

    override fun onConnectionSuspended(i: Int) {}
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return
        }
        if (PermissionUtils.isPermissionGranted(
                permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation()
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true
        }
    }

    override fun onLocationChanged(location: Location) {
        val latitude = location.latitude
        val longtitude = location.longitude
        if (mCurrentMarker != null) mCurrentMarker!!.remove()
        mCurrentLocation = location
        val markerOptions = MarkerOptions()
        markerOptions.position(LatLng(latitude, longtitude))
        mCurrentMarker = mMap!!.addMarker(markerOptions)
        mMap!!.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(mCurrentLocation!!.latitude, mCurrentLocation!!.longitude), 18f
            )
        )
        if (walkState) {                        //걸음 시작 버튼이 눌렸을 때
            endLatLng = LatLng(latitude, longtitude) //현재 위치를 끝점으로 설정
            drawPath() //polyline 그리기
            startLatLng = LatLng(latitude, longtitude) //시작점을 끝점으로 다시 설정
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(
                this, LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION, true
            )
        } else if (mMap != null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
            // Start location updates.
            LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this
            )
            if (mCurrentLocation != null) {
                Log.i(
                    "Location", "Latitude: " + mCurrentLocation!!.latitude
                            + ", Longitude: " + mCurrentLocation!!.longitude
                )
            }
        }
    }

    protected fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest!!.interval = 10000
        mLocationRequest!!.fastestInterval = 5000
        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {}
    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}