package motohud.fydp.com.motohud.maps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import motohud.fydp.com.motohud.R
import motohud.fydp.com.motohud.utils.PermissionUtils
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.LatLng
import android.location.Criteria
import android.location.LocationManager
import com.google.android.gms.maps.CameraUpdateFactory


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case, by
     * default the user is placed at their current location.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move  the camera
        mMap.setOnMyLocationButtonClickListener(this)
        mMap.setOnMyLocationClickListener(this)
        enableMyLocation()

        // Getting Current Location
        try {
            // Getting LocationManager object from System Service LOCATION_SERVICE
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Creating a criteria object to retrieve provider
            val criteria = Criteria()

            // Getting the name of the best provider
            val provider = locationManager.getBestProvider(criteria, true)
            moveToCurrentLocation(locationManager, provider)

        } catch (ex : SecurityException) {
            PermissionUtils.requestPermission(this, MapConstants.LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true)
        }
    }

    @Throws(SecurityException::class)
    private fun moveToCurrentLocation(locationManager: LocationManager, provider : String) {
        val location = locationManager.getLastKnownLocation(provider)

        if (location != null) {
            // Getting latitude of the current location
            val latitude = location.latitude
            // Getting longitude of the current location
            val longitude = location.longitude
            // Creating a LatLng object for the current location
            val currentPosition = LatLng(latitude, longitude)
            //mMap.addMarker(MarkerOptions().position(currentPosition).title("Start"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 15f))
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show()
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false
    }

    override fun onMyLocationClick(location: Location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show()
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, MapConstants.LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true)
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.isMyLocationEnabled = true
        }
    }
}
