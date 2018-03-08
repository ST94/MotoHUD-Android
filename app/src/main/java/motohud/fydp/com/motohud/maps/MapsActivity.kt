package motohud.fydp.com.motohud.maps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.android.synthetic.main.activity_maps.*
import motohud.fydp.com.motohud.R
import motohud.fydp.com.motohud.navigation.NavigationHttpRequest
import motohud.fydp.com.motohud.navigation.ui.DirectionDialogFragment
import motohud.fydp.com.motohud.utils.PermissionUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener, DirectionDialogFragment.DirectionDialogListener {

    private lateinit var mMap: GoogleMap
    private val markerPoints = ArrayList<LatLng>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fab.setOnClickListener { view ->
            showDirectionDialog()
        }
    }

    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the NoticeDialogFragment.NoticeDialogListener interface
    override fun onDialogPositiveClick(dialog: DialogFragment) {
        // User touched the dialog's positive button

//        val startField = findViewById<EditText>(R.id.start_location)
//        val startLatLng = startField.text
//        val endField = findViewById<EditText>(R.id.end_location)
//        val endLatLng = LatLng(startField.text)
//        generateDirectionMarkers(startLatLng, endLatLng)
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        // User touched the dialog's negative button
    }

    fun makeGeocodingRequest (address: String) {

    }

    fun showDirectionDialog() {
        // Create an instance of the dialog fragment and show it
        val dialog = DirectionDialogFragment()
        dialog.show(supportFragmentManager, "DirectionDialogFragment")
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case, by
     * default the user is placed at their current location.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

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

        mMap.setOnMapClickListener { point ->
            markerPoints.add(point)
            if (markerPoints.size > 1) {
                generateDirectionMarkers(markerPoints[0], markerPoints[1])
            }
        }
    }

    private fun generateDirectionMarkers(start: LatLng, end:LatLng) {
        // Already two locations
        if (markerPoints.size > 1) {
            markerPoints.clear()
            mMap.clear()
        }

        // Adding new item to the ArrayList
        markerPoints.add(start)

        // Creating MarkerOptions
        val options = MarkerOptions()

        // Setting the position of the marker
        options.position(end)

        if (markerPoints.size == 1) {
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        } else if (markerPoints.size == 2) {
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        }

        // Add new marker to the Google Map Android API V2
        mMap.addMarker(options)

        // Checks, whether start and end locations are captured
        if (markerPoints.size >= 2) {
            val origin = markerPoints[0]
            val dest = markerPoints[1]

            // Getting URL to the Google Directions API
            val url = URL("https://maps.googleapis.com/maps/api/directions/json?origin="+origin.latitude+","+origin.longitude+"&destination="+dest.latitude+","+dest.longitude+"&key=AIzaSyDwysEfPjHbKyzW-ZC5mDqi_s3bMY1-2E8")
            Log.d("onMapClick", url.toString())
            val FetchUrl = FetchUrl()

            // Start downloading json data from Google Directions API
            FetchUrl.execute(url.toString())
            //move map camera
            mMap.moveCamera(CameraUpdateFactory.newLatLng(origin))
            mMap.animateCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition.zoom))
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

    @Throws(IOException::class)
    private fun downloadUrl(strUrl: String): String {
        var data = ""
        var iStream: InputStream? = null
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL(strUrl)

            // Creating an http connection to communicate with url
            urlConnection = url.openConnection() as HttpURLConnection

            // Connecting to url
            urlConnection.connect()

            // Reading data from url
            iStream = urlConnection.inputStream

            val status = urlConnection.responseCode

            val br = BufferedReader(InputStreamReader(iStream))
            val sb = StringBuffer()

            when (status) {
                200, 201 -> {
                    var line = urlConnection.inputStream.bufferedReader().use(BufferedReader::readText)
                    sb.append(line)
                }
            }

            data = sb.toString()
            Log.d("downloadUrl", data.toString())
            br.close()

        } catch (e: Exception) {
            Log.d("Exception", e.toString())
            e.printStackTrace()
        } finally {
            iStream!!.close()
            urlConnection!!.disconnect()
        }
        return data
    }

    // Fetches data from url passed
    private inner class FetchUrl : AsyncTask<String, Void, String>(), NavigationHttpRequest.AsyncResponse{
        override fun processFinish(output: PolylineOptions) {
            mMap.addPolyline(output)
        }

        override fun doInBackground(vararg url: String): String {
            // For storing data from web service
            var data = ""
            try {
                // Fetching the data from web service
                data = downloadUrl(url[0])
                Log.d("Background Task data", data.toString())
            } catch (e: Exception) {
                Log.d("Background Task", e.toString())
            }

            return data
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            val parserTask = NavigationHttpRequest()
            // Invokes the thread for parsing the JSON data
            parserTask.delegate = this
            parserTask.execute(result)
        }
    }
}
