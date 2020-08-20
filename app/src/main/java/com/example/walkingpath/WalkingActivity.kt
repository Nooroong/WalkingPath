package com.example.walkingpath

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.android.synthetic.main.activity_walking.*

/*
DB의 정보를 이용해 핀 그리기 + 세부정보 표시 + 현재 위치 표시 및 이동

<현재 위치 표시 및 이동>
https://developers.google.com/maps/documentation/android-sdk/current-place-tutorial (이곳을 참고함)
https://nittaku.tistory.com/69 (한국어 설명)
WalkingPathActivity.kt의 코드에서 필요한 부분만 가져왔습니다.
 */


class WalkingActivity :
    AppCompatActivity(),
    OnMapReadyCallback,
    GoogleApiClient.ConnectionCallbacks,
    com.google.android.gms.location.LocationListener {
    private var map: GoogleMap? = null
    private var cameraPosition: CameraPosition? = null
    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val defaultLocation = LatLng(37.566665, 126.978399)
    private var locationPermissionGranted = false //위치 정보 사용 여부
    private var lastKnownLocation: Location? = null
    private var locationManager: LocationManager? = null
    private var startLatLng: LatLng = LatLng(0.0, 0.0)
    private var endLatLng: LatLng = LatLng(0.0, 0.0)
    private var walkState: Boolean = false //걸음 상태
    private var polylines: MutableList<Polyline> = mutableListOf()
    private var mCurrentLocation: Location? = null
    private var mLocationRequest: LocationRequest? = null
    private var mGoogleApiClient: GoogleApiClient? = null
    var mCurrentMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
        }

        setContentView(R.layout.activity_walking)

        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //gps를 키도록 유도
        if (!locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER)!!) {
            Toast.makeText(this, "위치를 사용으로 전환해주세요.", Toast.LENGTH_LONG).show()
            //GPS 설정화면으로 이동
            val intent: Intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            startActivity(intent)
        } else { //gps가 켜져있는 경우
            getDeviceLocation() //현재 위치를 찾아 점을 찍고 카메라를 이동
        }

        walking_start.setOnClickListener {
            changeWalkState()
        }

//        //확대 및 축소
//        zoominBtn.setOnClickListener {
//            map?.animateCamera(CameraUpdateFactory.zoomIn())
//        }
//        zoomoutBtn.setOnClickListener {
//            map?.animateCamera(CameraUpdateFactory.zoomOut())
//        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        map?.let { map ->
            outState.putParcelable(KEY_CAMERA_POSITION, map.cameraPosition)
            outState.putParcelable(KEY_LOCATION, lastKnownLocation)
        }
        super.onSaveInstanceState(outState)
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        var i = 0
        val collection = arrayOf<String>("Gangbuk", "Jungnang", "Nowon", "Seongbuk")



        getLocationPermission()
        updateLocationUI()
        drawPath()



        //https://webnautes.tistory.com/1011 의 코드 참고. 복붙하면 알아서 변환해줌.
        map!!.setOnMyLocationButtonClickListener(OnMyLocationButtonClickListener {
            //gps가 꺼져있는 경우
            if (!locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER)!!) {
                Toast.makeText(this, "위치를 사용으로 전환해주세요.", Toast.LENGTH_LONG).show()
                //GPS 설정화면으로 이동
                val intent: Intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                startActivity(intent)
            } else { //gps가 켜져있는 경우
                getDeviceLocation() //현재 위치를 찾아 점을 찍고 카메라를 이동
            }
            true
        })


    }

    override fun onConnected(p0: Bundle?) {
//        enableMyLocation()
    }

    override fun onConnectionSuspended(p0: Int) {

    }

    /*
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (map != null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
            // Start location updates.
            LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

            if (mCurrentLocation != null) {
                Log.i("Location", "Latitude: " + mCurrentLocation!!.latitude
                        + ", Longitude: " + mCurrentLocation!!.longitude
                );
            }
        }
    }
     */

    override fun onLocationChanged(location: Location) {
        val latitude = location.latitude
        val longtitude = location.longitude

        mCurrentMarker?.remove()
        mCurrentLocation = location
        val markerOptions:MarkerOptions = MarkerOptions()
        markerOptions.position(LatLng(latitude, longtitude))
        mCurrentMarker =  map?.addMarker(markerOptions)


        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(
            LatLng(mCurrentLocation!!.latitude, mCurrentLocation!!.longitude), 18F))
        if(walkState){ //걸음 시작 버튼이 눌렸을 때
            endLatLng = LatLng(latitude, longtitude) //현재 위치를 끝점으로 설정
            drawPath() //polyline 그리기
            startLatLng = LatLng(latitude, longtitude) //시작점을 끝점으로 다시 설정
        }
    }

    private fun drawPath() { //polyline을 그려주는 메소드
        val options: PolylineOptions =
            PolylineOptions().add(startLatLng).add(endLatLng).width(15F).color(Color.BLACK)
                .geodesic(true)
        polylines.add(map!!.addPolyline(options))
        map!!.moveCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 18F));
    }

    private fun changeWalkState() {
        if (!walkState) {
            Toast.makeText(this, "걸음 시작", Toast.LENGTH_SHORT).show()
            walkState = true
            startLatLng = LatLng (mCurrentLocation!!.latitude, mCurrentLocation!!.longitude) //현재 위치를 시작점으로 설정
        } else {
            Toast.makeText(this, "걸음 종료", Toast.LENGTH_SHORT).show()
            walkState = false
        }
    }

    fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        if(mLocationRequest != null) {
            mLocationRequest!!.interval = 10000
            mLocationRequest!!.fastestInterval = 5000
            mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            map?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastKnownLocation!!.latitude,
                                        lastKnownLocation!!.longitude
                                    ), DEFAULT_ZOOM.toFloat()
                                )
                            )
                        }
                    } else {
                        Log.d("LocationCheck", "Current location is null. Using defaults.")
                        Log.e("LocationCheck", "Exception: %s", task.exception)
                        map?.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat())
                        )
//                        map?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }


    private fun getLocationPermission() {
        //앱 자체에 미리 권한이 받아져 있는 경우
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted = true
        } else {
            //위치 사용 허가를 물어봄
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    locationPermissionGranted = true
//                    enableMyLocation()
                } else return //권한 획득을 거부당하면 그냥 함수 종료. 이 부분 없으면 허락할 때까지 권한 요청함.(무한루프)
            }
        }
        updateLocationUI()
    }


    //위치 권한 허용에 따라 버튼 생성
    private fun updateLocationUI() {
        if (map == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                map?.isMyLocationEnabled = false
                map?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }


    companion object {
        private val TAG = WalkingActivity::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        // Keys for storing activity state.
        // [START maps_current_place_state_keys]
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"
        // [END maps_current_place_state_keys]

        // Used for selecting the current place.
        private const val M_MAX_ENTRIES = 5
    }
}