package com.lifeSavers.emergencyappsignup

// import android.R
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.*
import com.mancj.materialsearchbar.MaterialSearchBar
import com.mancj.materialsearchbar.MaterialSearchBar.OnSearchActionListener
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter
import com.skyfishjy.library.RippleBackground
import java.util.*
import android.os.*
import androidx.annotation.NonNull
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    lateinit var mMap: GoogleMap
    lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    lateinit var placesClient:PlacesClient
    lateinit var predictionList : List<AutocompletePrediction>
    lateinit var mLastKnownLocation: Location
    lateinit var locationCallback: LocationCallback
    lateinit var materialSearchBar: MaterialSearchBar
    var mapView: View? = null
    lateinit var btnFind : Button
    lateinit var rippleBg: RippleBackground
    val DEFAULT_ZOOM = 15f
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        materialSearchBar = findViewById(R.id.searchBar)
        btnFind = findViewById(R.id.btn_find)
        rippleBg = findViewById(R.id.ripple_bg)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
        mapView = mapFragment.view
        mFusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this@MapActivity)
        Places.initialize(this@MapActivity, ("AIzaSyDkmsjZUMpcNvsHL4Fa04G8MvMWaWzvfFw"))
        placesClient = Places.createClient(this)
        val token = AutocompleteSessionToken.newInstance()
        materialSearchBar.setOnSearchActionListener(object : OnSearchActionListener {
            override fun onSearchStateChanged(enabled: Boolean) {}
            override fun onSearchConfirmed(text: CharSequence) {
                startSearch(text.toString(), true, null, true)
            }

            override fun onButtonClicked(buttonCode: Int) {
                if (buttonCode == MaterialSearchBar.BUTTON_NAVIGATION) {
                    //opening or closing a navigation drawer
                } else if (buttonCode == MaterialSearchBar.BUTTON_BACK) {
                    materialSearchBar.disableSearch()
                }
            }
        })
        materialSearchBar.addTextChangeListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val predictionsRequest = FindAutocompletePredictionsRequest.builder()
                    .setTypeFilter(TypeFilter.ADDRESS)
                    .setSessionToken(token)
                    .setQuery(s.toString())
                    .build()
                placesClient.findAutocompletePredictions(predictionsRequest)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val predictionsResponse = task.result
                            if (predictionsResponse != null) {
                                predictionList = predictionsResponse.autocompletePredictions
                                val suggestionsList: MutableList<String?> =
                                    ArrayList()
                                for (i in predictionList.indices) {
                                    val prediction = predictionList.get(i)
                                    suggestionsList.add(prediction.getFullText(null).toString())
                                }
                                materialSearchBar.updateLastSuggestions(suggestionsList)
                                if (!materialSearchBar.isSuggestionsVisible()) {
                                    materialSearchBar.showSuggestionsList()
                                }
                            }
                        } else {
                            Log.i("mytag", "prediction fetching task unsuccessful")
                        }
                    }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        materialSearchBar.setSuggestionsClickListener(object :
            SuggestionsAdapter.OnItemViewClickListener {
            override fun OnItemClickListener(position: Int, v: View) {
                if (position >= predictionList.size) {
                    return
                }
                val selectedPrediction = predictionList[position]
                val suggestion = materialSearchBar.getLastSuggestions()[position].toString()
                materialSearchBar.setText(suggestion)
                Handler(Looper.getMainLooper()).postDelayed({ materialSearchBar.clearSuggestions() }, 1000)
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(
                    materialSearchBar.getWindowToken(),
                    InputMethodManager.HIDE_IMPLICIT_ONLY
                )
                val placeId = selectedPrediction.placeId
                val placeFields = Arrays.asList(Place.Field.LAT_LNG)
                val fetchPlaceRequest = FetchPlaceRequest.builder(placeId, placeFields).build()
                placesClient.fetchPlace(fetchPlaceRequest)
                    .addOnSuccessListener { fetchPlaceResponse ->
                        val place = fetchPlaceResponse.place
                        Log.i("mytag", "Place found: " + place.name)
                        val latLngOfPlace = place.latLng
                        if (latLngOfPlace != null) {
                            mMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    latLngOfPlace,
                                    DEFAULT_ZOOM
                                )
                            )
                        }
                    }.addOnFailureListener { e ->
                        if (e is ApiException) {
                            val apiException = e
                            apiException.printStackTrace()
                            val statusCode = apiException.statusCode
                            Log.i("mytag", "place not found: " + e.message)
                            Log.i("mytag", "status code: $statusCode")
                        }
                    }
            }

            override fun OnItemDeleteListener(position: Int, v: View) {}
        })
        btnFind = findViewById(R.id.btn_find)
        btnFind.setOnClickListener(View.OnClickListener {
            // val currentMarkerLocation = mMap.cameraPosition.target
            rippleBg.startRippleAnimation()
            Handler(Looper.getMainLooper()).postDelayed({
                rippleBg.stopRippleAnimation()
                startActivity(Intent(this@MapActivity, SecondFragment::class.java))
                finish()
            }, 3000)
        })
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        if (mapView != null && mapView!!.findViewById<View?>("1".toInt()) != null) {
            val locationButton =
                (mapView!!.findViewById<View>("1".toInt()).parent as View).findViewById<View>("2".toInt())
            val layoutParams = locationButton.layoutParams as RelativeLayout.LayoutParams
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            layoutParams.setMargins(0, 0, 40, 180)
        }

        //check if gps is enabled or not and then request user to enable it
        val locationRequest = LocationRequest.create()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(this@MapActivity)
        val task = settingsClient.checkLocationSettings(builder.build())
        task.addOnSuccessListener(
            this@MapActivity
        ) { //getDeviceLocation()
        }
        task.addOnFailureListener(
            this@MapActivity
        ) { e ->
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(this@MapActivity, 51)
                } catch (e1: SendIntentException) {
                    e1.printStackTrace()
                }
            }
        }
        mMap.setOnMyLocationButtonClickListener {
            if (materialSearchBar.isSuggestionsVisible) materialSearchBar.clearSuggestions()
            if (materialSearchBar.isSearchEnabled) materialSearchBar.disableSearch()
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 51) {
            if (resultCode == RESULT_OK) {
                // getDeviceLocation()
            }
        }
    }

// var 3

//    @SuppressLint("MissingPermission")
//    open fun getDeviceLocation() {
//        mFusedLocationProviderClient.getLastLocation()
//            .addOnCompleteListener(object : OnCompleteListener<Location?> {
//                override fun onComplete(@NonNull task: Task<Location?>) {
//                    if (task.isSuccessful()) {
//                        mLastKnownLocation = task.getResult()!!
//                        if (mLastKnownLocation == null) {
//                            val locationRequest: LocationRequest = LocationRequest.create()
//                            locationRequest.setInterval(10000)
//                            locationRequest.setFastestInterval(5000)
//                            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
//                            locationCallback = object : LocationCallback() {
//                                override fun onLocationResult(locationResult: LocationResult?) {
//                                    super.onLocationResult(locationResult)
//                                    if (locationResult == null) {
//                                        return
//                                    }
//                                    mLastKnownLocation = locationResult.getLastLocation()
//                                    mMap.moveCamera(
//                                        CameraUpdateFactory.newLatLngZoom(
//                                            LatLng(
//                                                mLastKnownLocation.getLatitude(),
//                                                mLastKnownLocation.getLongitude()
//                                            ), DEFAULT_ZOOM
//                                        )
//                                    )
//                                    mFusedLocationProviderClient.removeLocationUpdates(locationCallback)
//                                }
//                            }
//                            mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
//                        } else {
//                            mMap.moveCamera(
//                                CameraUpdateFactory.newLatLngZoom(
//                                    LatLng(
//                                        mLastKnownLocation.getLatitude(),
//                                        mLastKnownLocation.getLongitude()
//                                    ), DEFAULT_ZOOM
//                                )
//                            )
//                        }
//                    } else {
//                        Toast.makeText(this@MapActivity, "unable to get last location", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            })
//    }

// var 2

//    @SuppressLint("MissingPermission")
//    fun getDeviceLocation() {
//        mFusedLocationProviderClient.getLastLocation()
//            .addOnCompleteListener(object : OnCompleteListener<Location?> {
//                override fun onComplete(@NonNull task: Task<Location?>) {
//                    if (task.isSuccessful()) {
//                        mLastKnownLocation = task.getResult()
//                        if (mLastKnownLocation != null) {
//                            mMap.moveCamera(
//                                CameraUpdateFactory.newLatLngZoom(
//                                    LatLng(
//                                        mLastKnownLocation.getLatitude(),
//                                        mLastKnownLocation.getLongitude()
//                                    ), DEFAULT_ZOOM
//                                )
//                            )
//                        } else {
//                            val locationRequest: LocationRequest = LocationRequest.create()
//                            locationRequest.setInterval(10000)
//                            locationRequest.setFastestInterval(5000)
//                            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
//                            locationCallback = object : LocationCallback() {
//                                override fun onLocationResult(locationResult: LocationResult?) {
//                                    super.onLocationResult(locationResult)
//                                    if (locationResult == null) {
//                                        return
//                                    }
//                                    mLastKnownLocation = locationResult.getLastLocation()
//                                    mMap.moveCamera(
//                                        CameraUpdateFactory.newLatLngZoom(
//                                            LatLng(
//                                                mLastKnownLocation.getLatitude(),
//                                                mLastKnownLocation.getLongitude()
//                                            ), DEFAULT_ZOOM
//                                        )
//                                    )
//                                    mFusedLocationProviderClient.removeLocationUpdates(locationCallback)
//                                }
//                            }
//                            mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
//                        }
//                    } else {
//                        Toast.makeText(this@MapActivity, "unable to get last location", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            })
//    }



//var 1


//    @get:SuppressLint("MissingPermission")
//    val deviceLocation: Unit
//        get() {
//            mFusedLocationProviderClient.lastLocation
//                .addOnCompleteListener { task ->
//                    if (task.isSuccessful) {
//                        mLastKnownLocation = task.getResult()
//                        if (mLastKnownLocation != null) {
//                            mMap.moveCamera(
//                                CameraUpdateFactory.newLatLngZoom(
//                                    LatLng(
//                                        mLastKnownLocation.latitude,
//                                        mLastKnownLocation.longitude
//                                    ), DEFAULT_ZOOM
//                                )
//                            )
//                        } else {
//                            val locationRequest =
//                                LocationRequest.create()
//                            locationRequest.interval = 10000
//                            locationRequest.fastestInterval = 5000
//                            locationRequest.priority =
//                                LocationRequest.PRIORITY_HIGH_ACCURACY
//                            locationCallback = object : LocationCallback() {
//                                override fun onLocationResult(locationResult: LocationResult) {
//                                    super.onLocationResult(locationResult)
//                                    if (locationResult == null) {
//                                        return
//                                    }
//                                    mLastKnownLocation = locationResult.lastLocation
//                                    mMap.moveCamera(
//                                        CameraUpdateFactory.newLatLngZoom(
//                                            LatLng(
//                                                mLastKnownLocation.getLatitude(),
//                                                mLastKnownLocation.getLongitude()
//                                            ), DEFAULT_ZOOM
//                                        )
//                                    )
//                                    mFusedLocationProviderClient.removeLocationUpdates(
//                                        locationCallback
//                                    )
//                                }
//                            }
//                            mFusedLocationProviderClient.requestLocationUpdates(
//                                locationRequest,
//                                locationCallback,
//                                null
//                            )
//                        }
//                    } else {
//                        Toast.makeText(
//                            this@MapActivity,
//                            "unable to get last location",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
//        }
}